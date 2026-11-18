package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.client.autoconfigure.HttpClientAutoConfiguration;
import org.springframework.boot.http.client.autoconfigure.service.HttpServiceClientPropertiesAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.service.HttpServiceClientAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTaskRepository;
import io.vanillabp.cockpit.gui.api.v1.client.OfficialCurrentUserApi;
import io.vanillabp.cockpit.gui.api.v1.client.OfficialTasklistApi;
import io.vanillabp.cockpit.gui.api.v1.client.autoconfigure.OfficialGuiApiClientAutoConfiguration;
import io.vanillabp.cockpit.support.rest.httpservice.HttpServiceClientConventionsAutoConfiguration;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A workflow module points the generated client of the official GUI API at the simulator by one
 * key, the address of the simulator with the prefix of the cockpit's official GUI API, and nothing
 * else changes. The client adds {@code /gui/api/v1} to the address, as it does for the cockpit, and
 * the simulator answers below {@code /official-api}. The workflow module is an application of its
 * own here, with the auto-configurations of the client, so the test builds no client by hand.
 */
@ExtendWith(SuppressOutputExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:a-generated-client-reaches-the-simulator-by-its-address", "dev-shell-simulator.users[0].id=john", "dev-shell-simulator.users[0].email=john@doe.com", "dev-shell-simulator.users[0].first-name=John", "dev-shell-simulator.users[0].last-name=Doe", "dev-shell-simulator.users[0].groups=RISK_ASSESSMENT"
})
class AGeneratedClientReachesTheSimulatorByItsAddressTest {

  private static final String USER_TASK_ID = "task-1";

  private final HttpClient http = HttpClient.newHttpClient();

  @Value("${local.server.port}")
  private int port;

  @Autowired
  private UserTaskRepository userTasks;

  @AfterEach
  void cleanUp() {

    userTasks.deleteAll();
    RequestContextHolder.resetRequestAttributes();

  }

  /**
   * The workflow module: an application with the client of the official GUI API, whose address is
   * the simulator with the prefix {@code /official-api}.
   */
  private ApplicationContextRunner workflowModule() {

    return new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            JacksonAutoConfiguration.class,
            HttpMessageConvertersAutoConfiguration.class,
            HttpClientAutoConfiguration.class,
            HttpServiceClientPropertiesAutoConfiguration.class,
            RestClientAutoConfiguration.class,
            HttpServiceClientAutoConfiguration.class,
            HttpServiceClientConventionsAutoConfiguration.class,
            OfficialGuiApiClientAutoConfiguration.class))
        .withPropertyValues(OfficialGuiApiClientAutoConfiguration.ADDRESS
            + "=http://localhost:"
            + port
            + "/official-api");

  }

  private HttpResponse<String> send(
      final String method,
      final String path,
      final String body) throws Exception {

    final var request = HttpRequest
        .newBuilder(URI.create("http://localhost:"
            + port
            + path))
        .header("Content-Type", "application/json")
        .method(method, HttpRequest.BodyPublishers.ofString(body))
        .build();
    return http.send(request, HttpResponse.BodyHandlers.ofString());

  }

  @Test
  void aUserTaskReportedToTheSimulatorIsReadThroughTheClient() throws Exception {

    final var report = send("POST", "/bpms/api/v1_1/usertask/created", """
        {
          "id": "event-1",
          "userTaskId": "%s",
          "timestamp": "2026-11-18T10:00:00Z",
          "workflowModuleId": "loan-approval",
          "bpmnProcessId": "loan_approval",
          "taskDefinition": "assessRisk",
          "title": { "en": "Assess the risk" },
          "uiUriPath": "/ui",
          "uiUriType": "WEBCOMPONENT"
        }
        """.formatted(USER_TASK_ID));
    assertThat(report.statusCode()).isEqualTo(HttpStatus.OK.value());

    workflowModule().run(context -> {
      final var answer = context
          .getBean(OfficialTasklistApi.class)
          .getUserTask(USER_TASK_ID, null);

      assertThat(answer.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(answer.getBody().getId()).isEqualTo(USER_TASK_ID);
      assertThat(answer.getBody().getTaskDefinition()).isEqualTo("assessRisk");
    });

  }

  @Test
  void theUserSignedInIsReadThroughTheClientWithTheUsersCookie() throws Exception {

    final var signIn = send("POST", "/dev-shell/user/john", "");
    assertThat(signIn.statusCode()).isEqualTo(HttpStatus.OK.value());
    final var loginCookie = signIn
        .headers()
        .firstValue("Set-Cookie")
        .orElseThrow()
        .split(";")[0];
    final var usersRequest = new MockHttpServletRequest();
    usersRequest.addHeader("Cookie", loginCookie);
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(usersRequest));

    workflowModule().run(context -> {
      final var answer = context
          .getBean(OfficialCurrentUserApi.class)
          .currentUser();

      assertThat(answer.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(answer.getBody().getId()).isEqualTo("john");
      assertThat(answer.getBody().getGroups())
          .extracting(group -> group.getId())
          .containsExactly("RISK_ASSESSMENT");
    });

  }

}
