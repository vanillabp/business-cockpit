package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTaskRepository;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import tools.jackson.databind.json.JsonMapper;

/**
 * A workflow module reports who must not work on a user task, who may see it although he is no
 * candidate, and how notifications are delivered. The official API shows none of it, so the
 * simulator keeps these values and shows the last report as it came in at
 * {@code GET /simulator-api/usertask/{id}/report}. The reports go over HTTP, like a workflow
 * module sends them, and the report is read without a login, like the rest of the simulator's
 * APIs.
 */
@ExtendWith(SuppressOutputExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:the-simulator-keeps-what-organizes-a-user-task",
    // the simulator does not start without a user to log in as
    "dev-shell-simulator.users[0].id=john"
})
class TheSimulatorKeepsWhatOrganizesAUserTaskTest {

  private static final String USER_TASK_ID = "task-1";

  private final HttpClient client = HttpClient.newHttpClient();

  @Value("${local.server.port}")
  private int port;

  @Autowired
  private UserTaskRepository userTasks;

  @Autowired
  private JsonMapper jsonMapper;

  @AfterEach
  void removeTheUserTasks() {

    userTasks.deleteAll();

  }

  private static String report(
      final String admittedUser,
      final String notificationDelivery) {

    return """
        {
          "id": "event-%s",
          "userTaskId": "%s",
          "timestamp": "2026-11-13T10:00:00Z",
          "workflowModuleId": "loan-approval",
          "bpmnProcessId": "loan_approval",
          "taskDefinition": "assessRisk",
          "title": { "en": "Assess the risk" },
          "uiUriPath": "/ui",
          "uiUriType": "WEBCOMPONENT",
          "candidateUsers": [ "john", "jane" ],
          "excludedCandidateUsers": [ "jane" ],
          "admittedUsers": [ "%s" ],
          "notificationDelivery": "%s"
        }
        """.formatted(admittedUser, USER_TASK_ID, admittedUser, notificationDelivery);

  }

  private int send(
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
    return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();

  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> lastReport() throws Exception {

    final var request = HttpRequest
        .newBuilder(URI.create("http://localhost:"
            + port
            + "/simulator-api/usertask/"
            + USER_TASK_ID
            + "/report"))
        .GET()
        .build();
    final var answer = client.send(request, HttpResponse.BodyHandlers.ofString());
    assertThat(answer.statusCode()).isEqualTo(200);
    return jsonMapper.readValue(answer.body(), Map.class);

  }

  @Test
  void theCreatingReportIsKeptAndShown() throws Exception {

    assertThat(send("POST", "/bpms/api/v1_1/usertask/created", report("auditor", "FORCE"))).isEqualTo(200);

    final var stored = userTasks.findById(USER_TASK_ID).orElseThrow();
    assertThat(stored.getExcludedCandidateUsers()).containsExactly("jane");
    assertThat(stored.getAdmittedUsers()).containsExactly("auditor");
    assertThat(stored.getNotificationDelivery()).isEqualTo("FORCE");

    final var report = lastReport();
    assertThat(report)
        .containsEntry("excludedCandidateUsers", List.of("jane"))
        .containsEntry("admittedUsers", List.of("auditor"))
        .containsEntry("notificationDelivery", "FORCE")
        .containsEntry("id", "event-auditor");

  }

  @Test
  void anUpdatingReportReplacesTheOneBefore() throws Exception {

    send("POST", "/bpms/api/v1_1/usertask/created", report("auditor", "FORCE"));
    assertThat(send("POST", "/bpms/api/v1_1/usertask/"
        + USER_TASK_ID
        + "/updated", report("controller", "SUPPRESS")))
        .isEqualTo(200);

    final var stored = userTasks.findById(USER_TASK_ID).orElseThrow();
    assertThat(stored.getAdmittedUsers()).containsExactly("controller");
    assertThat(stored.getNotificationDelivery()).isEqualTo("SUPPRESS");
    assertThat(lastReport())
        .containsEntry("admittedUsers", List.of("controller"))
        .containsEntry("notificationDelivery", "SUPPRESS")
        .containsEntry("id", "event-controller");

  }

  @Test
  void anUnknownUserTaskHasNoReport() throws Exception {

    final var request = HttpRequest
        .newBuilder(URI.create("http://localhost:"
            + port
            + "/simulator-api/usertask/no-such-task/report"))
        .GET()
        .build();

    assertThat(client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(404);

  }

}
