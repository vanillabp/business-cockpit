package io.vanillabp.cockpit.extension.quarkus.it;

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
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
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
import jakarta.inject.Inject;

/**
 * The configuration in the shape version 1 of the Business Cockpit had, written the way an
 * application which upgrades already has it: a list as a list, a group hierarchy as a map of
 * lists, a workflow and one of its user tasks with sections of their own, a timeout as a number
 * of milliseconds next to one written with a unit, and a workflow module whose own defaults file
 * says something the application did not.
 * <p>
 * This is the twin of the Spring Boot test asserting the same tree. On this platform a section
 * the mapping does not declare ends the startup rather than being ignored, so a test which only
 * read the values would not notice that half the tree never arrived.
 */
@ExtendWith(SuppressOutputExtension.class)
public class ConfigurationMatrixTest {

  private static final String WORKFLOW_MODULE = "test-module";

  private static final String BPMN_PROCESS = "TestProcess";

  @RegisterExtension
  static final QuarkusExtensionTest extensionTest = new QuarkusExtensionTest()
      .withApplicationRoot(
          jar -> jar
              .addAsResource("matrix.yaml", "application.yaml")
              .addAsResource("matrix-test-module.yaml", "test-module.yaml")
              .addAsResource("test-module/processes/dummy/TestProcess.bpmn")
              .addAsResource(
                  "workflow-module-descriptor/workflow-module", "META-INF/workflow-module")
              .addClass(TestAggregate.class)
              .addClass(TestAggregatePersistence.class)
              .addClass(TestWorkflowService.class)
              .addClass(TestBpmsBridge.class)
              .addClass(TestWorkflowAwareness.class)
              .addClass(TestWorkflowModuleDetails.class));

  @Inject
  CockpitSettings settings;

  @Inject
  MigrationAdapterProperties properties;

  @Inject
  BusinessCockpitEventPublisher publisher;

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
                TestBpmsBridge
                    .userTask(
                        WORKFLOW_MODULE, BPMN_PROCESS, "no-aggregate",
                        TestBpmsBridge.USER_TASK_ID),
                UserTaskEventKind.CREATED, "bpms-event-switched-off", OffsetDateTime.now(),
                EventTransaction.CURRENT),
        "a user task was reported although user tasks are switched off");

    assertFalse(
        publisher
            .publishWorkflowEvent(
                new WorkflowReference(
                    TestBpmsBridge.ADAPTER_ID, WORKFLOW_MODULE, BPMN_PROCESS, "no-aggregate", TestBpmsBridge.WORKFLOW_ID),
                WorkflowEventKind.CREATED, "bpms-event-switched-off", OffsetDateTime.now(),
                EventTransaction.CURRENT),
        "a workflow was reported although the workflow list is switched off");

  }

}
