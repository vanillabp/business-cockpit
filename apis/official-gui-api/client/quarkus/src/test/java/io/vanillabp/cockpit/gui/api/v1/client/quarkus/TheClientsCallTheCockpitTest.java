package io.vanillabp.cockpit.gui.api.v1.client.quarkus;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vanillabp.cockpit.gui.api.v1.client.OfficialCurrentUserApi;
import io.vanillabp.cockpit.gui.api.v1.client.OfficialTasklistApi;
import io.vanillabp.cockpit.support.rest.restclient.RestClients;
import io.vanillabp.cockpit.support.rest.restclient.RestClientsMisconfigured;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;

/**
 * Workflow modules call the cockpit with the generated clients, built from the keys at their own
 * prefixes. 'base-url' is the address of the cockpit, with a context path or without, and the
 * clients add the path of the API themselves. While the application answers a request, the user's cookie goes along to the cockpit,
 * and only to the cockpit.
 */
@QuarkusTest
@QuarkusTestResource(TheCockpitOfTheTests.class)
@ExtendWith(SuppressOutputExtension.class)
public class TheClientsCallTheCockpitTest {

  static final MockWebServer cockpit = new MockWebServer();

  @Inject
  @LoanApprovalCockpit.LoanApproval
  OfficialTasklistApi tasklist;

  @Inject
  @LoanApprovalCockpit.LoanApproval
  OfficialCurrentUserApi currentUser;

  @Inject
  @OnboardingCockpit.Onboarding
  OfficialTasklistApi onboardingTasklist;

  @AfterAll
  static void stop() {
    cockpit.close();
  }

  @BeforeEach
  void startTheCockpit() {

    if (!cockpit.getStarted()) {
      try {
        cockpit.start(InetAddress.getByName("127.0.0.1"), Integer.getInteger(TheCockpitOfTheTests.PORT));
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    try {
      while (cockpit.takeRequest(10, TimeUnit.MILLISECONDS) != null) {
        // forget the requests of earlier tests
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

  }

  static MockResponse json(
      final String body) {

    return new MockResponse.Builder().addHeader("Content-Type", "application/json").body(body).build();

  }

  static MockResponse userTask(
      final String id,
      final String assignee) {

    return json("""
        {"id": "%s", "workflowModuleId": "loan-approval", "assignee": {"id": "%s"}}"""
        .formatted(id, assignee));

  }

  static MockResponse user(
      final String id) {

    return json("""
        {"id": "%s", "display": "Doe, Jane", "groups": [{"id": "CLERK"}]}"""
        .formatted(id));

  }

  static RecordedRequest next() {

    try {
      final var request = cockpit.takeRequest(5, TimeUnit.SECONDS);
      assertThat(request).as("the cockpit got a request").isNotNull();
      return request;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AssertionError(e);
    }

  }

  @Test
  public void inside_a_request_the_users_cookie_goes_along() {

    cockpit.enqueue(userTask("task-1", "jane"));
    cockpit.enqueue(user("jane"));

    given()
        .header("Cookie", "bc=signed-login")
        .get("/loan-approval/assess-risk/task-1")
        .then()
        .statusCode(200)
        .body(equalTo("the form for jane"));

    final var taskRequest = next();
    assertThat(taskRequest.getTarget()).isEqualTo("/gui/api/v1/usertask/task-1");
    assertThat(taskRequest.getHeaders().get("Cookie")).isEqualTo("bc=signed-login");
    final var userRequest = next();
    assertThat(userRequest.getTarget()).isEqualTo("/gui/api/v1/app/current-user");
    assertThat(userRequest.getHeaders().get("Cookie")).isEqualTo("bc=signed-login");

  }

  @Test
  public void the_form_tells_a_task_another_user_claimed() {

    cockpit.enqueue(userTask("task-1", "john"));
    cockpit.enqueue(user("jane"));

    given()
        .header("Cookie", "bc=signed-login")
        .get("/loan-approval/assess-risk/task-1")
        .then()
        .statusCode(200)
        .body(equalTo("claimed by john"));

  }

  @Test
  public void outside_of_a_request_no_cookie_goes_along() {

    cockpit.enqueue(user("jane"));

    final var user = currentUser.currentUser();

    assertThat(user.getId()).isEqualTo("jane");
    assertThat(user.getGroups()).extracting(group -> group.getId()).containsExactly("CLERK");
    assertThat(next().getHeaders().get("Cookie")).isNull();

  }

  @Test
  public void another_service_never_gets_the_users_cookie() {

    cockpit.enqueue(new MockResponse.Builder().addHeader("Content-Type", "text/plain").body("hello").build());

    given()
        .header("Cookie", "bc=signed-login")
        .get("/another-service")
        .then()
        .statusCode(200)
        .body(equalTo("hello"));

    final var request = next();
    assertThat(request.getTarget()).isEqualTo("/another-service/hello");
    assertThat(request.getHeaders().get("Cookie")).isNull();

  }

  @Test
  public void each_module_calls_the_cockpit_with_its_own_keys() {

    cockpit.enqueue(userTask("task-2", "jane"));
    onboardingTasklist.getUserTask("task-2", null);
    assertThat(next().getTarget()).isEqualTo("/onboarding/gui/api/v1/usertask/task-2");

    // only the module 'onboarding' waits no longer than 300 milliseconds
    cockpit.enqueue(new MockResponse.Builder().headersDelay(1, TimeUnit.SECONDS).build());
    assertThatThrownBy(() -> onboardingTasklist.getUserTask("task-2", null)).isInstanceOf(ProcessingException.class);
    cockpit.enqueue(new MockResponse.Builder()
        .headersDelay(1, TimeUnit.SECONDS)
        .addHeader("Content-Type", "application/json")
        .body("""
            {"id": "task-1", "workflowModuleId": "loan-approval"}""")
        .build());
    assertThat(tasklist.getUserTask("task-1", null).getId()).isEqualTo("task-1");

  }

  @Test
  public void an_address_with_the_path_of_the_api_stops_the_start() {

    final var failure = catchThrowableOfType(RestClientsMisconfigured.class, () -> new RestClients().build(
        OfficialTasklistApi.class, "with-the-path.cockpit"));

    assertThat(failure).isNotNull();
    assertThat(failure.getProblems()).singleElement().asString()
        .contains("in 'with-the-path.cockpit.base-url' ends with '/gui/api/v1'")
        .contains("the path of the API which the client OfficialTasklistApi adds to the address itself")
        .endsWith("to the address without it: 'http://127.0.0.1:"
            + Integer.getInteger(TheCockpitOfTheTests.PORT)
            + "'.");

  }

  @Test
  public void an_unknown_task_comes_as_the_rest_clients_exception() {

    cockpit.enqueue(new MockResponse.Builder().code(404).build());

    assertThatThrownBy(() -> tasklist.getUserTask("unknown", null))
        .isInstanceOf(WebApplicationException.class)
        .satisfies(e -> assertThat(((WebApplicationException) e).getResponse().getStatus()).isEqualTo(404));

  }

}
