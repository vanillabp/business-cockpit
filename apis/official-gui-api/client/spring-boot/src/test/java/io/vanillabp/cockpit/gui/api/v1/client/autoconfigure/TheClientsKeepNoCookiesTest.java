package io.vanillabp.cockpit.gui.api.v1.client.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.service.registry.ImportHttpServices;

import io.vanillabp.cockpit.gui.api.v1.client.OfficialTasklistApi;
import io.vanillabp.cockpit.support.rest.httpservice.HttpServiceClientsMisconfigured;
import io.vanillabp.cockpit.support.rest.httpservice.HttpServiceGroupPrefix;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;

/**
 * A client of the official GUI API sends the cookie of the user's request, and an HTTP service
 * group has one HTTP client for all users. So the client keeps no cookies of its own: a cookie the
 * cockpit sets in its answer to one user must not go out with the call of the next user. The key
 * for all clients of the application does not switch cookies on for it, and the key of the group
 * itself stops the start with the reason.
 */
@ExtendWith(SuppressOutputExtension.class)
public class TheClientsKeepNoCookiesTest {

  interface LoanApprovalTasklist extends OfficialTasklistApi {
  }

  /** A workflow module with a group of its own, at a prefix of its own. */
  @Configuration
  @ImportHttpServices(group = "loan-approval-cockpit", types = LoanApprovalTasklist.class)
  static class LoanApproval {

    @Bean
    HttpServiceGroupPrefix loanApprovalCockpitKeys() {
      return HttpServiceGroupPrefix.of("loan-approval-cockpit", "loan-approval.cockpit");
    }

  }

  private final MockWebServer cockpit = new MockWebServer();

  @AfterEach
  void stop() {
    RequestContextHolder.resetRequestAttributes();
    cockpit.close();
  }

  @Test
  public void a_cookie_the_cockpit_sets_for_one_user_does_not_go_along_with_the_call_of_the_next_user() {

    try (var context = new SpringApplicationBuilder(TestApplication.class)
        .web(WebApplicationType.NONE)
        .run("--spring.http.clients.cookie-handling=enable",
            "--"
                + OfficialGuiApiClientAutoConfiguration.ADDRESS
                + "="
                + Cockpit.address(cockpit))) {

      final var tasklist = context.getBean(OfficialTasklistApi.class);
      cockpit.enqueue(new MockResponse.Builder()
          .code(200)
          .addHeader("Content-Type", "application/json")
          .addHeader("Set-Cookie", "bc=renewed-login-of-alice; Path=/; HttpOnly")
          .body("""
              {"id": "task-1", "workflowModuleId": "loan-approval", "assignee": {"id": "alice"}}""")
          .build());
      cockpit.enqueue(Cockpit.userTask("task-1", "bob"));

      callAs("bc=login-of-alice", () -> tasklist.getUserTask("task-1", null));
      callAs("bc=login-of-bob", () -> tasklist.getUserTask("task-1", null));

      assertThat(Cockpit.next(cockpit).getHeaders().get("Cookie")).isEqualTo("bc=login-of-alice");
      assertThat(Cockpit.next(cockpit).getHeaders().get("Cookie"))
          .as("the call of the second user carries the second user's cookie only")
          .isEqualTo("bc=login-of-bob");

    }

  }

  @Test
  public void cookie_handling_of_the_group_of_the_auto_configuration_stops_the_start() {

    final var problems = problemsOf(new Class<?>[]{
        TestApplication.class
    }, OfficialGuiApiClientAutoConfiguration.KEYS
        + ".cookie-handling=enable",
        OfficialGuiApiClientAutoConfiguration.ADDRESS
            + "=https://cockpit.example.com");

    assertThat(problems).containsExactly("""
        The HTTP service group 'official-gui-api' sends the cookie of the user's request along, so it \
        keeps no cookies of its own. With 'spring.http.serviceclient.official-gui-api.cookie-handling: \
        enable' its one HTTP client would keep one store of cookies for all users, and a cookie the \
        server sets in its answer to one user would go out with the calls of other users. Remove \
        the key, or set it to 'disable'.""");

  }

  @Test
  public void cookie_handling_of_the_group_of_a_workflow_module_stops_the_start() {

    final var problems = problemsOf(new Class<?>[]{
        TestApplication.class, LoanApproval.class
    }, "loan-approval.cockpit.base-url=https://cockpit.example.com",
        "loan-approval.cockpit.cookie-handling=enable-when-possible");

    assertThat(problems).singleElement().asString()
        .contains("'loan-approval-cockpit' sends the cookie of the user's request along")
        .contains("'loan-approval.cockpit.cookie-handling: enable-when-possible'");

  }

  @Test
  public void cookie_handling_switched_off_for_the_group_is_allowed() {

    try (var context = new SpringApplicationBuilder(TestApplication.class)
        .web(WebApplicationType.NONE)
        .run("--"
            + OfficialGuiApiClientAutoConfiguration.KEYS
            + ".cookie-handling=disable",
            "--"
                + OfficialGuiApiClientAutoConfiguration.ADDRESS
                + "=https://cockpit.example.com")) {

      assertThat(context.getBean(OfficialTasklistApi.class)).isNotNull();

    }

  }

  private static void callAs(
      final String cookie,
      final Runnable call) {

    final var usersRequest = new MockHttpServletRequest();
    usersRequest.addHeader("Cookie", cookie);
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(usersRequest));
    try {
      call.run();
    } finally {
      RequestContextHolder.resetRequestAttributes();
    }

  }

  /** The problems the support module lists, wherever they are in the chain of causes. */
  private static List<String> problemsOf(
      final Class<?>[] configurations,
      final String... properties) {

    try (var context = new SpringApplicationBuilder(configurations)
        .web(WebApplicationType.NONE)
        .run(Arrays.stream(properties).map(property -> "--"
            + property).toArray(String[]::new))) {
      throw new AssertionError("The application started, but it should have stopped");
    } catch (RuntimeException failure) {
      for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
        if (cause instanceof HttpServiceClientsMisconfigured misconfigured) {
          return misconfigured.getProblems();
        }
      }
      throw failure;
    }

  }

}
