package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTaskRepository;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.WorkflowRepository;
import io.vanillabp.cockpit.gui.api.v1.NotificationConfiguration;
import io.vanillabp.cockpit.gui.api.v1.NotificationWorkflow;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import tools.jackson.databind.json.JsonMapper;

/**
 * A user interface which shows the page of the notification settings reads and writes
 * {@code /official-api/gui/api/v1/notifications/**}. The simulator sends no notifications, so it offers
 * no medium, but it keeps what a user configures and lists the workflows the user could be
 * notified about. Like the cockpit, it answers only a user who is signed in.
 */
@ExtendWith(SuppressOutputExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:the-simulator-answers-the-notification-settings", "dev-shell-simulator.users[0].id=john", "dev-shell-simulator.users[1].id=jane"
})
class TheSimulatorAnswersTheNotificationSettingsTest {

  private static final String NOTIFICATIONS = "/official-api/gui/api/v1/notifications";

  @Value("${local.server.port}")
  private int port;

  @Autowired
  private JsonMapper jsonMapper;

  @Autowired
  private UserTaskRepository userTasks;

  @Autowired
  private WorkflowRepository workflows;

  @AfterEach
  void removeWhatWasReported() {

    userTasks.deleteAll();
    workflows.deleteAll();

  }

  @Test
  void withoutALoginEveryPathAnswers401() throws Exception {

    final var browser = new Browser(port);

    for (final var path : List.of("/media", "/config", "/recipient-config", "/workflows")) {
      assertThat(browser.get(NOTIFICATIONS + path).statusCode())
          .as(path)
          .isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }
    assertThat(browser.send("PUT", NOTIFICATIONS
        + "/config", "{}").statusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED.value());

  }

  @Test
  void theSimulatorOffersNoMedium() throws Exception {

    final var browser = new Browser(port).signIn("john");

    final var media = browser.get(NOTIFICATIONS
        + "/media");
    assertThat(media.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(jsonMapper.readValue(media.body(), List.class)).isEmpty();

    final var recipients = browser.get(NOTIFICATIONS
        + "/recipient-config");
    assertThat(recipients.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(jsonMapper.readValue(recipients.body(), List.class)).isEmpty();

    assertThat(browser.send("PUT", NOTIFICATIONS
        + "/recipient-config/email", """
            { "emailAddress": "john@doe.com" }
            """).statusCode())
        .isEqualTo(HttpStatus.NOT_FOUND.value());

  }

  @Test
  void eachUserKeepsHisOwnConfiguration() throws Exception {

    final var john = new Browser(port).signIn("john");
    final var jane = new Browser(port).signIn("jane");

    assertThat(configurationOf(john).getGlobalAllViaMedium()).isNull();

    assertThat(john.send("PUT", NOTIFICATIONS
        + "/config", """
            {
              "globalAllViaMedium": { "email": true },
              "perWorkflow": { "loan-approval#loan_approval": { "none": true } }
            }
            """).statusCode())
        .isEqualTo(HttpStatus.OK.value());

    final var configuration = configurationOf(john);
    assertThat(configuration.getGlobalAllViaMedium()).isEqualTo(Map.of("email", true));
    assertThat(configuration.getPerWorkflow().get("loan-approval#loan_approval").getNone()).isTrue();
    assertThat(configurationOf(jane).getGlobalAllViaMedium()).isNull();

  }

  @Test
  void theWorkflowsAreThoseOfTheCases() throws Exception {

    final var browser = new Browser(port).signIn("john");
    assertThat(browser.send("POST", "/bpms/api/v1_1/workflow/created", """
        {
          "id": "event-1",
          "workflowId": "workflow-1",
          "timestamp": "2026-11-18T10:00:00Z",
          "workflowModuleId": "loan-approval",
          "bpmnProcessId": "loan_approval",
          "title": { "en": "Loan approval" }
        }
        """).statusCode()).isEqualTo(HttpStatus.OK.value());
    // a task of a called process first: the list still names the process of the case, with the
    // title of a task which sits in that process
    reportUserTask(browser, "task-1", "risk_assessment", "Risk assessment");
    reportUserTask(browser, "task-2", "loan_approval", "Loan approval");

    final var answer = browser.get(NOTIFICATIONS
        + "/workflows");

    assertThat(answer.statusCode()).isEqualTo(HttpStatus.OK.value());
    final var listed = jsonMapper.readValue(answer.body(), NotificationWorkflow[].class);
    assertThat(listed).hasSize(1);
    assertThat(listed[0].getWorkflowModuleId()).isEqualTo("loan-approval");
    assertThat(listed[0].getBpmnProcessId()).isEqualTo("loan_approval");
    assertThat(listed[0].getWorkflowTitle()).isEqualTo(Map.of("en", "Loan approval"));

  }

  private NotificationConfiguration configurationOf(
      final Browser browser) throws Exception {

    final var answer = browser.get(NOTIFICATIONS
        + "/config");
    assertThat(answer.statusCode()).isEqualTo(HttpStatus.OK.value());
    return jsonMapper.readValue(answer.body(), NotificationConfiguration.class);

  }

  private static void reportUserTask(
      final Browser browser,
      final String userTaskId,
      final String bpmnProcessId,
      final String workflowTitle) throws Exception {

    assertThat(browser.send("POST", "/bpms/api/v1_1/usertask/created", """
        {
          "id": "event-%s",
          "userTaskId": "%s",
          "workflowId": "workflow-1",
          "timestamp": "2026-11-18T10:00:00Z",
          "workflowModuleId": "loan-approval",
          "bpmnProcessId": "%s",
          "workflowTitle": { "en": "%s" },
          "taskDefinition": "assessRisk",
          "title": { "en": "Assess the risk" },
          "uiUriPath": "/ui",
          "uiUriType": "WEBCOMPONENT"
        }
        """.formatted(userTaskId, userTaskId, bpmnProcessId, workflowTitle)).statusCode())
        .isEqualTo(HttpStatus.OK.value());

  }

}
