package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTaskRepository;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.WorkflowModuleRepository;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.WorkflowRepository;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The simulator takes reports of version 1.1 of the BPMS API only. Version 1.1 has existed for more
 * than a year, so a workflow module which still reports through version 1 gets a current adapter to
 * be tried in the dev shell. The cockpit itself still takes version 1.
 *
 * <p>The reports go over HTTP, like a workflow module sends them.
 */
@ExtendWith(SuppressOutputExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:the-simulator-reads-only-version-1-1-of-the-bpms-api",
    // the simulator does not start without a user to log in as
    "dev-shell-simulator.users[0].id=john"
})
class TheSimulatorReadsOnlyVersion1_1OfTheBpmsApiTest {

  private static final String USER_TASK_REPORT = """
      {
        "id": "event-1",
        "userTaskId": "task-1",
        "timestamp": "2026-11-17T10:00:00Z",
        "workflowModuleId": "loan-approval",
        "bpmnProcessId": "loan_approval",
        "taskDefinition": "assessRisk",
        "title": { "en": "Assess the risk" },
        "uiUriPath": "/ui",
        "uiUriType": "WEBCOMPONENT"
      }
      """;

  private static final String WORKFLOW_REPORT = """
      {
        "id": "event-2",
        "workflowId": "workflow-1",
        "timestamp": "2026-11-17T10:00:00Z",
        "workflowModuleId": "loan-approval",
        "bpmnProcessId": "loan_approval",
        "title": { "en": "Loan approval" }
      }
      """;

  private static final String REGISTRATION = """
      {
        "uri": "http://localhost:8080/wm/loan-approval",
        "taskProviderApiUriPath": "/wm/loan-approval/api/v1/business-cockpit",
        "workflowProviderApiUriPath": "/wm/loan-approval/api/v1/business-cockpit"
      }
      """;

  private final HttpClient client = HttpClient.newHttpClient();

  @Value("${local.server.port}")
  private int port;

  @Autowired
  private UserTaskRepository userTasks;

  @Autowired
  private WorkflowRepository workflows;

  @Autowired
  private WorkflowModuleRepository workflowModules;

  @AfterEach
  void removeWhatWasStored() {

    userTasks.deleteAll();
    workflows.deleteAll();
    workflowModules.deleteAll();

  }

  private int post(
      final String path,
      final String body) throws Exception {

    final var request = HttpRequest
        .newBuilder(URI.create("http://localhost:"
            + port
            + path))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();
    return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();

  }

  private static String bodyFor(
      final String path) {

    if (path.startsWith("/workflow-module/")) {
      return REGISTRATION;
    }
    if (path.startsWith("/workflow/")) {
      return WORKFLOW_REPORT;
    }
    return USER_TASK_REPORT;

  }

  @ParameterizedTest
  @ValueSource(strings = {
      "/workflow-module/loan-approval", "/usertask/created", "/usertask/task-1/updated", "/usertask/task-1/completed", "/workflow/created", "/workflow/workflow-1/updated"
  })
  void aReportOfVersion1IsAnswered404(
      final String path) throws Exception {

    assertThat(post("/bpms/api/v1"
        + path, bodyFor(path))).isEqualTo(404);
    assertThat(userTasks.count()).isZero();
    assertThat(workflows.count()).isZero();
    assertThat(workflowModules.count()).isZero();

  }

  @Test
  void theSameReportOfVersion1_1IsStored() throws Exception {

    assertThat(post("/bpms/api/v1_1/usertask/created", USER_TASK_REPORT)).isEqualTo(200);
    assertThat(userTasks.findById("task-1")).isPresent();

  }

}
