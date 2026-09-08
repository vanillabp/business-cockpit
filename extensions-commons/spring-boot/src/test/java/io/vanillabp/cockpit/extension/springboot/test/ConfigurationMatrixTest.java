package io.vanillabp.cockpit.extension.springboot.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.vanillabp.cockpit.extension.config.BusinessCockpitConfiguration;
import io.vanillabp.cockpit.extension.config.CockpitSettings;
import io.vanillabp.cockpit.extension.config.RestConnection;
import io.vanillabp.cockpit.extension.spi.BusinessCockpitEventPublisher;
import io.vanillabp.cockpit.extension.spi.EventTransaction;
import io.vanillabp.cockpit.extension.spi.UserTaskEventKind;
import io.vanillabp.cockpit.extension.spi.WorkflowEventKind;
import io.vanillabp.cockpit.extension.spi.WorkflowReference;
import io.vanillabp.integration.adapter.migration.config.MigrationAdapterProperties;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The configuration in the shape version 1 of the Business Cockpit had, written the way an
 * application which upgrades already has it: a list as a list, a group hierarchy as a map of
 * lists, a workflow and one of its user tasks with sections of their own, a timeout as a number
 * of milliseconds next to one written with a unit, and a workflow module whose own defaults file
 * says something the application did not.
 * <p>
 * What is asserted is what only a booted application shows: that this platform's binding carries
 * every one of those shapes into the extension. The Quarkus twin of this test asserts the same
 * tree, because a binding is exactly the part a neutral core cannot be right about on its own.
 */
@SpringBootTest(classes = TestApplication.class,
    properties = {
        "spring.config.additional-location=classpath:/matrix/"
    })
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
public class ConfigurationMatrixTest {

  private static final String WORKFLOW_MODULE = "test-module";

  private static final String BPMN_PROCESS = "TestProcess";

  @Autowired
  private CockpitSettings settings;

  @Autowired
  private MigrationAdapterProperties properties;

  @Autowired
  private BusinessCockpitEventPublisher publisher;

  private BusinessCockpitConfiguration configuration() {

    return BusinessCockpitConfiguration.readAndValidate(properties, settings, false);

  }

  @Test
  @DisplayName("A timeout is read as version 1 wrote it and as this platform writes one")
  public void bothSpellingsOfATimeoutAreRead() {

    final var rest = configuration().getRest();

    assertEquals(Duration.ofSeconds(2), rest.connectTimeout());
    assertEquals(Duration.ofSeconds(20), rest.readTimeout());

  }

  @Test
  @DisplayName("The token client is configured for itself, down to its own proxy")
  public void theTokenClientIsConfiguredForItself() {

    final var oauth = configuration().getRest().oauth();

    assertEquals("taxi-ride", oauth.clientId());
    assertEquals(Duration.ofSeconds(30), oauth.readTimeout());
    assertEquals(RestConnection.DEFAULT_CONNECT_TIMEOUT, oauth.connectTimeout());
    assertEquals("token-proxy.internal", oauth.proxy().host());
    assertEquals(8080, oauth.proxy().port());
    assertFalse(oauth.verifySsl());

  }

  @Test
  @DisplayName("A list is a list and a group hierarchy is a map of lists, as in version 1")
  public void theShapesOfVersionOneAreBound() {

    final var module = configuration().workflowModule(WORKFLOW_MODULE);

    assertEquals(List.of("en", "de"), module.i18nLanguages());
    assertEquals(
        List.of("TEAM_MEMBER", "ASSISTANT"),
        List.copyOf(module.groupHierarchy().get("TEAM_LEAD")));

  }

  @Test
  @DisplayName("A workflow and one of its user tasks say what they differ from the module in")
  public void aWorkflowAndAUserTaskSayTheirOwn() {

    final var module = configuration().workflowModule(WORKFLOW_MODULE);

    assertEquals(List.of("fr"), module.i18nLanguages(BPMN_PROCESS));
    assertEquals("fr", module.bpmnDescriptionLanguage(BPMN_PROCESS));
    assertEquals("rides", module.templatePathOfWorkflow(BPMN_PROCESS));
    assertEquals("approval", module.templatePathOfUserTask(BPMN_PROCESS, "approve"));

  }

  @Test
  @DisplayName("What the workflow module's own file says is read where the application is silent")
  public void theWorkflowModulesOwnFileIsRead() {

    assertEquals(
        "shipped-with-the-module",
        configuration().workflowModule(WORKFLOW_MODULE).templatePath());

  }

  @Test
  @DisplayName("A Kafka setting keeps the dots it was written with, and so does the half's key")
  public void theOpenSectionsKeepWhatWasWrittenIntoThem() {

    assertEquals("SSL", settings.kafka().properties().get("security.protocol"));
    assertEquals("taxi-ride", settings.kafka().properties().get("client.id"));
    assertEquals("25", settings.processEngineApi().rememberedUserTasks());

  }

  @Test
  @DisplayName("Producer settings alone choose no transport")
  public void producerSettingsAloneChooseNoTransport() {

    assertNull(configuration().getKafka());
    assertNotNull(configuration().getRest());

  }

  @Test
  @DisplayName("The two switches of version 1 stop what they name from being reported")
  public void theTwoSwitchesStopTheReports() {

    final var configuration = configuration();
    assertFalse(configuration.isUserTasksEnabled());
    assertFalse(configuration.isWorkflowListEnabled());

    assertFalse(
        publisher
            .publishUserTaskEvent(
                RecordingBpmsBridge
                    .userTask(
                        WORKFLOW_MODULE, BPMN_PROCESS, "no-aggregate",
                        RecordingBpmsBridge.USER_TASK_ID, "approve"),
                UserTaskEventKind.CREATED, "bpms-event-switched-off", OffsetDateTime.now(),
                EventTransaction.CURRENT),
        "a user task was reported although user tasks are switched off");

    assertFalse(
        publisher
            .publishWorkflowEvent(
                new WorkflowReference(
                    RecordingBpmsBridge.ADAPTER_ID, WORKFLOW_MODULE, BPMN_PROCESS, "no-aggregate", RecordingBpmsBridge.WORKFLOW_ID),
                WorkflowEventKind.CREATED, "bpms-event-switched-off", OffsetDateTime.now(),
                EventTransaction.CURRENT),
        "a workflow was reported although the workflow list is switched off");

  }

}
