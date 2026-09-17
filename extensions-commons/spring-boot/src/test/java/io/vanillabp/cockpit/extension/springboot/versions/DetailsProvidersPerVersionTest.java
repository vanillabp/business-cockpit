package io.vanillabp.cockpit.extension.springboot.versions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import io.vanillabp.cockpit.extension.spi.BusinessCockpitEventPublisher;
import io.vanillabp.cockpit.extension.spi.EventTransaction;
import io.vanillabp.cockpit.extension.spi.UserTaskEventKind;
import io.vanillabp.cockpit.extension.spi.UserTaskReference;
import io.vanillabp.cockpit.extension.spi.WorkflowEventKind;
import io.vanillabp.cockpit.extension.spi.WorkflowReference;
import io.vanillabp.cockpit.extension.test.support.CockpitServer;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The version of the deployed BPMN process picks the details provider, for a user task and for
 * a workflow alike. Two methods share a task, two share the workflow, and each pair is told
 * apart by the versions its methods name.
 * <p>
 * Everything is asserted where a user sees it, in the request the cockpit server receives. A
 * version no method serves is asserted there as well: what arrives is then what the BPMS
 * reported, which is the same answer a process without any provider gets.
 */
@SpringBootTest(classes = VersionsApplication.class,
    properties = {
        // an outbox store of its own. The contexts of the other test classes stay cached with
        // their pollers running, and on a shared store one of them takes the entry away
        "spring.datasource.url=jdbc:h2:mem:cockpit-versions;DB_CLOSE_DELAY=-1"
    })
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
public class DetailsProvidersPerVersionTest {

  private static final String WORKFLOW_MODULE = "test-module";

  @DynamicPropertySource
  static void cockpitServer(
      final DynamicPropertyRegistry registry) {

    registry
        .add("vanillabp.cockpit.rest.base-url", CockpitServer::baseUrl);

  }

  @Autowired
  private VersionedService workflowService;

  @Autowired
  private TransactionTemplate transactions;

  @Autowired
  private BusinessCockpitEventPublisher publisher;

  private VersionedAggregate aggregate;

  @BeforeEach
  public void startAWorkflowAndForgetWhatArrivedBefore() {

    CockpitServer.forgetRequests();
    aggregate = transactions.execute(status -> {
      final var started = new VersionedAggregate();
      started.setCustomer("Anna");
      return workflowService.processes().startWorkflow(started);
    });

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
   */
  private String reportedTask(
      final String taskDefinition,
      final String processVersion,
      final String eventId) {

    transactions
        .executeWithoutResult(status -> publisher
            .publishUserTaskEvent(
                new UserTaskReference(
                    VersionsBpmsBridge.ADAPTER_ID, WORKFLOW_MODULE, VersionedService.BPMN_PROCESS, processVersion, aggregate
                        .getId()
                        .toString(), "workflow-of-"
                            + eventId, eventId, taskDefinition, "Activity_"
                                + taskDefinition),
                UserTaskEventKind.CREATED, eventId, OffsetDateTime.now(),
                EventTransaction.CURRENT));
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
   */
  private String reportedWorkflow(
      final String processVersion,
      final String eventId) {

    transactions
        .executeWithoutResult(status -> publisher
            .publishWorkflowEvent(
                new WorkflowReference(
                    VersionsBpmsBridge.ADAPTER_ID, WORKFLOW_MODULE, VersionedService.BPMN_PROCESS, processVersion, aggregate
                        .getId()
                        .toString(), eventId),
                WorkflowEventKind.CREATED, eventId, OffsetDateTime.now(),
                EventTransaction.CURRENT));
    return CockpitServer
        .awaitRequest("/workflow/created", "\"id\":\"%s\"".formatted(eventId))
        .body();

  }

  @Test
  @DisplayName("Two providers of one user task are told apart by the version of the event")
  public void theVersionPicksBetweenTwoProvidersOfATask() {

    final var first = reportedTask(VersionedService.APPROVE, "1", "versions-task-1");
    assertTrue(first.contains(VersionedService.APPROVE_OF_THE_FIRST), first);

    final var later = reportedTask(VersionedService.APPROVE, "2", "versions-task-2");
    assertTrue(later.contains(VersionedService.APPROVE_OF_THE_LATER), later);

    final var muchLater = reportedTask(VersionedService.APPROVE, "3", "versions-task-3");
    assertTrue(muchLater.contains(VersionedService.APPROVE_OF_THE_LATER), muchLater);

  }

  @Test
  @DisplayName("A provider named for a version tag serves the version carrying that tag")
  public void aVersionTagPicksTheProviderOfThatDeployment() {

    final var tagged = reportedTask(VersionedService.ESCALATE, "3", "versions-task-4");

    assertTrue(tagged.contains(VersionedService.ESCALATE_OF_THE_TAGGED), tagged);

  }

  @Test
  @DisplayName("A task whose providers serve other versions is reported as the BPMS had it")
  public void aTaskNobodyServesInThisVersionFallsBackToThePrefilledDetails() {

    final var untouched = reportedTask(VersionedService.ESCALATE, "1", "versions-task-5");

    assertFalse(untouched.contains("servedBy"), untouched);
    assertTrue(untouched.contains(VersionsBpmsBridge.TASK_NAME), untouched);

  }

  @Test
  @DisplayName("Two providers of one workflow are told apart by the version of the event")
  public void theVersionPicksBetweenTwoProvidersOfAWorkflow() {

    final var first = reportedWorkflow("1", "versions-workflow-1");
    assertTrue(first.contains(VersionedService.WORKFLOW_OF_THE_FIRST), first);

    final var second = reportedWorkflow("2", "versions-workflow-2");
    assertTrue(second.contains(VersionedService.WORKFLOW_OF_THE_SECOND), second);

  }

  @Test
  @DisplayName("A workflow whose providers serve other versions keeps what the BPMS reported")
  public void aWorkflowNobodyServesInThisVersionFallsBackToThePrefilledDetails() {

    final var untouched = reportedWorkflow("3", "versions-workflow-3");

    assertFalse(untouched.contains("servedBy"), untouched);
    assertTrue(untouched.contains(VersionsBpmsBridge.PROCESS_NAME), untouched);

  }

}
