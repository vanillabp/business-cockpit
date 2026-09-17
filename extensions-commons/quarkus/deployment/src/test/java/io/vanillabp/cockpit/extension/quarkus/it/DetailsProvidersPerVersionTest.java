package io.vanillabp.cockpit.extension.quarkus.it;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.cockpit.extension.config.ConfigurationKeys;
import io.vanillabp.cockpit.extension.spi.BusinessCockpitEventPublisher;
import io.vanillabp.cockpit.extension.spi.EventTransaction;
import io.vanillabp.cockpit.extension.spi.UserTaskEventKind;
import io.vanillabp.cockpit.extension.spi.UserTaskReference;
import io.vanillabp.cockpit.extension.spi.WorkflowEventKind;
import io.vanillabp.cockpit.extension.spi.WorkflowReference;
import io.vanillabp.cockpit.extension.test.support.CockpitServer;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import jakarta.inject.Inject;
import jakarta.transaction.UserTransaction;

/**
 * The version of the deployed BPMN process picks the details provider, for a user task and for
 * a workflow alike. A provider which serves a version this application never deploys is named
 * while it boots.
 * <p>
 * It asserts the same thing as the Spring Boot test of this name, because a neutral core being
 * right says nothing about a platform's glue ever calling it.
 */
@ExtendWith(SuppressOutputExtension.class)
public class DetailsProvidersPerVersionTest {

  private static final String WORKFLOW_MODULE = "test-module";

  @RegisterExtension
  static final QuarkusExtensionTest extensionTest = new QuarkusExtensionTest()
      .withApplicationRoot(
          jar -> jar
              .addAsResource("business-cockpit.yaml", "application.yaml")
              .addAsResource("test-module/processes/dummy/TestProcess.bpmn")
              .addAsResource(
                  "workflow-module-descriptor/workflow-module", "META-INF/workflow-module")
              .addClass(TestAggregate.class)
              .addClass(TestAggregatePersistence.class)
              .addClass(VersionedService.class)
              .addClass(VersionsBpmsBridge.class)
              .addClass(VersionsProcessVersions.class)
              .addClass(TestWorkflowAwareness.class)
              .addClass(TestWorkflowModuleDetails.class)
              // the test class runs in the application's class loader, so what it talks to the
              // cockpit server with has to be reachable from there as well
              .addClass(CockpitServer.class))
      .overrideRuntimeConfigKey(
          "vanillabp.cockpit.rest.base-url", CockpitServer.baseUrl())
      .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue())
      .assertLogRecords(records -> {
        final var report = records
            .stream()
            .map(DetailsProvidersPerVersionTest::rendered)
            .filter(message -> message.contains("auditOfAVersionNobodyDeploys"))
            .findFirst()
            .orElseThrow(
                () -> new AssertionError(
                    "nothing was said about the provider of a version nobody deploys: "
                        + records.stream().map(DetailsProvidersPerVersionTest::rendered).toList()));

        assertTrue(report.contains(ConfigurationKeys.EXTENSION_ID), report);
        assertTrue(report.contains("the method never runs"), report);
        // the versions the BPMS does hold belong in the message: without them the developer
        // cannot tell a typo in the range from a deployment which never happened
        assertTrue(report.contains("held: 1, 2, 3"), report);
      });

  /**
   * One log record as a reader sees it. A record may arrive with its values already in the text
   * or with them beside it, and a test which only read the text would assert against '{}'.
   *
   * @param record The record
   * @return Its message with the values of the record in it
   */
  private static String rendered(
      final LogRecord record) {

    var message = record.getMessage() == null
        ? ""
        : record.getMessage();
    final var parameters = record.getParameters();
    if (parameters == null) {
      return message;
    }
    for (final var parameter : parameters) {
      message = message.replaceFirst("\\{\\}", String.valueOf(parameter));
    }
    return message;

  }

  @Inject
  VersionedService workflowService;

  @Inject
  BusinessCockpitEventPublisher publisher;

  @Inject
  UserTransaction transaction;

  private TestAggregate aggregate;

  @BeforeEach
  public void startAWorkflowAndForgetWhatArrivedBefore() throws Exception {

    CockpitServer.forgetRequests();
    transaction.begin();
    try {
      final var started = new TestAggregate();
      started.setCustomer("Anna");
      aggregate = workflowService.processes().startWorkflow(started);
      transaction.commit();
    } catch (final RuntimeException e) {
      transaction.rollback();
      throw e;
    }

  }

  /**
   * Raises one user task event of the given version and answers with what the cockpit server
   * got for it.
   *
   * @param taskDefinition The task definition, whose element id is 'Activity_' plus this text
   * @param processVersion The version of the deployed process the event comes from
   * @param eventId What tells this event's request from the ones of the other tests. It is the
   *          task's id as well, because the outbox keeps one entry per task and kind
   * @return The body of the request the cockpit server received
   * @throws Exception If the transaction around the event fails
   */
  private String reportedTask(
      final String taskDefinition,
      final String processVersion,
      final String eventId) throws Exception {

    transaction.begin();
    publisher
        .publishUserTaskEvent(
            new UserTaskReference(
                VersionsBpmsBridge.ADAPTER_ID, WORKFLOW_MODULE, VersionedService.BPMN_PROCESS, processVersion, aggregate
                    .getId()
                    .toString(), "workflow-of-"
                        + eventId, eventId, taskDefinition, "Activity_"
                            + taskDefinition),
            UserTaskEventKind.CREATED, eventId, OffsetDateTime.now(), EventTransaction.CURRENT);
    transaction.commit();
    return CockpitServer
        .awaitRequest("/usertask/created", "\"id\":\"%s\"".formatted(eventId))
        .body();

  }

  /**
   * Raises one workflow event of the given version and answers with what the cockpit server got
   * for it.
   *
   * @param processVersion The version of the deployed process the event comes from
   * @param eventId What tells this event's request from the ones of the other tests
   * @return The body of the request the cockpit server received
   * @throws Exception If the transaction around the event fails
   */
  private String reportedWorkflow(
      final String processVersion,
      final String eventId) throws Exception {

    transaction.begin();
    publisher
        .publishWorkflowEvent(
            new WorkflowReference(
                VersionsBpmsBridge.ADAPTER_ID, WORKFLOW_MODULE, VersionedService.BPMN_PROCESS, processVersion, aggregate
                    .getId()
                    .toString(), eventId),
            WorkflowEventKind.CREATED, eventId, OffsetDateTime.now(), EventTransaction.CURRENT);
    transaction.commit();
    return CockpitServer
        .awaitRequest("/workflow/created", "\"id\":\"%s\"".formatted(eventId))
        .body();

  }

  @Test
  @DisplayName("Two providers of one user task are told apart by the version of the event")
  public void theVersionPicksBetweenTwoProvidersOfATask() throws Exception {

    final var first = reportedTask(VersionedService.APPROVE, "1", "versions-task-1");
    assertTrue(first.contains(VersionedService.APPROVE_OF_THE_FIRST), first);

    final var later = reportedTask(VersionedService.APPROVE, "2", "versions-task-2");
    assertTrue(later.contains(VersionedService.APPROVE_OF_THE_LATER), later);

    final var muchLater = reportedTask(VersionedService.APPROVE, "3", "versions-task-3");
    assertTrue(muchLater.contains(VersionedService.APPROVE_OF_THE_LATER), muchLater);

  }

  @Test
  @DisplayName("A provider named for a version tag serves the version carrying that tag")
  public void aVersionTagPicksTheProviderOfThatDeployment() throws Exception {

    final var tagged = reportedTask(VersionedService.ESCALATE, "3", "versions-task-4");

    assertTrue(tagged.contains(VersionedService.ESCALATE_OF_THE_TAGGED), tagged);

  }

  @Test
  @DisplayName("A task whose providers serve other versions is reported as the BPMS had it")
  public void aTaskNobodyServesInThisVersionFallsBackToThePrefilledDetails() throws Exception {

    final var untouched = reportedTask(VersionedService.ESCALATE, "1", "versions-task-5");

    assertFalse(untouched.contains("servedBy"), untouched);
    assertTrue(untouched.contains(VersionsBpmsBridge.TASK_NAME), untouched);

  }

  @Test
  @DisplayName("Two providers of one workflow are told apart by the version of the event")
  public void theVersionPicksBetweenTwoProvidersOfAWorkflow() throws Exception {

    final var first = reportedWorkflow("1", "versions-workflow-1");
    assertTrue(first.contains(VersionedService.WORKFLOW_OF_THE_FIRST), first);

    final var second = reportedWorkflow("2", "versions-workflow-2");
    assertTrue(second.contains(VersionedService.WORKFLOW_OF_THE_SECOND), second);

  }

  @Test
  @DisplayName("A workflow whose providers serve other versions keeps what the BPMS reported")
  public void aWorkflowNobodyServesInThisVersionFallsBackToThePrefilledDetails() throws Exception {

    final var untouched = reportedWorkflow("3", "versions-workflow-3");

    assertFalse(untouched.contains("servedBy"), untouched);
    assertTrue(untouched.contains(VersionsBpmsBridge.PROCESS_NAME), untouched);

  }

}
