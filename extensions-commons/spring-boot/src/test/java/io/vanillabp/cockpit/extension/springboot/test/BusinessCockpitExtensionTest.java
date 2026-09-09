package io.vanillabp.cockpit.extension.springboot.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

import io.vanillabp.cockpit.extension.BusinessCockpitExtension;
import io.vanillabp.cockpit.extension.spi.BusinessCockpitEventPublisher;
import io.vanillabp.cockpit.extension.spi.EventTransaction;
import io.vanillabp.cockpit.extension.spi.UserTaskEventKind;
import io.vanillabp.cockpit.extension.spi.UserTaskReference;
import io.vanillabp.cockpit.extension.spi.WorkflowEventKind;
import io.vanillabp.cockpit.extension.spi.WorkflowReference;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The Business Cockpit extension inside a booted application, from the event a BPMS reported to
 * the request the cockpit server receives.
 * <p>
 * Nothing here mocks the way between the two: the workflow aggregate is persisted, the outbox
 * entry is written in the application's transaction, the dispatch happens after the commit on
 * the outbox's own thread, the application's details provider runs, and a cockpit server of the
 * test reads what arrives.
 */
@SpringBootTest(classes = TestApplication.class)
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
public class BusinessCockpitExtensionTest {

  private static final String WORKFLOW_MODULE = "test-module";

  private static final String BPMN_PROCESS = "TestProcess";

  @DynamicPropertySource
  static void cockpitServer(
      final DynamicPropertyRegistry registry) {

    registry
        .add("vanillabp.cockpit.rest.base-url", CockpitServer::baseUrl);

  }

  @Autowired
  private TestWorkflowService workflowService;

  @Autowired
  private TestAggregateRepository aggregates;

  @Autowired
  private TransactionTemplate transactions;

  @Autowired
  private BusinessCockpitEventPublisher publisher;

  @Autowired
  private RecordingBpmsBridge bridge;

  @Autowired
  private BusinessCockpitExtension extension;

  @BeforeEach
  public void forgetWhatArrivedBefore() {

    CockpitServer.forgetRequests();
    bridge.knowsTheTask(true);

  }

  private TestAggregate aStartedWorkflow() {

    return transactions.execute(status -> {
      final var aggregate = new TestAggregate();
      aggregate.setCustomer("Anna");
      return workflowService.processes().startWorkflow(aggregate);
    });

  }

  private UserTaskReference userTaskOf(
      final TestAggregate aggregate,
      final String taskDefinition) {

    return RecordingBpmsBridge
        .userTask(
            WORKFLOW_MODULE, BPMN_PROCESS, aggregate.getId().toString(),
            RecordingBpmsBridge.USER_TASK_ID, taskDefinition);

  }

  @Test
  @DisplayName("A user task the BPMS reported reaches the cockpit, enriched by the application")
  public void aReportedUserTaskReachesTheCockpit() {

    final var aggregate = aStartedWorkflow();

    transactions
        .executeWithoutResult(status -> publisher
            .publishUserTaskEvent(
                userTaskOf(aggregate, "approve"), UserTaskEventKind.CREATED, "bpms-event-1",
                OffsetDateTime.now(), EventTransaction.CURRENT));

    final var request = CockpitServer.awaitRequest("/usertask/created");
    assertTrue(request.body().contains("\"id\":\"bpms-event-1\""), request.body());
    assertTrue(request.body().contains("\"taskDefinition\":\"approve\""), request.body());
    assertTrue(request.body().contains("\"customer\":\"Anna\""), request.body());
    assertTrue(request.body().contains("\"event\":\"CREATED\""), request.body());
    assertTrue(request.body().contains("\"candidateGroups\":[\"approvers\"]"), request.body());
    assertTrue(request.body().contains("Approve the order"), request.body());
    assertTrue(request.body().contains("\"assignee\":\"anna\""), request.body());
    assertTrue(request.body().contains("\"workflowModuleId\":\"test-module\""), request.body());

  }

  @Test
  @DisplayName("What the details provider wrote into the aggregate is committed with the dispatch")
  public void whatTheProviderWroteIsCommitted() {

    final var aggregate = aStartedWorkflow();

    transactions
        .executeWithoutResult(status -> publisher
            .publishUserTaskEvent(
                userTaskOf(aggregate, "approve"), UserTaskEventKind.CREATED, "bpms-event-2",
                OffsetDateTime.now(), EventTransaction.CURRENT));

    CockpitServer.awaitRequest("/usertask/created");
    assertEquals(
        TestWorkflowService.APPROVE_NOTE,
        aggregates.findById(aggregate.getId()).orElseThrow().getNote());

  }

  @Test
  @DisplayName("A rolled back transaction reports nothing")
  public void aRollbackReportsNothing() {

    final var aggregate = aStartedWorkflow();

    assertThrows(
        IllegalStateException.class,
        () -> transactions.executeWithoutResult(status -> {
          publisher
              .publishUserTaskEvent(
                  userTaskOf(aggregate, "approve"), UserTaskEventKind.CREATED, "bpms-event-3",
                  OffsetDateTime.now(), EventTransaction.CURRENT);
          throw new IllegalStateException("the business transaction failed");
        }));

    CockpitServer.awaitQuiet();
    assertTrue(
        CockpitServer
            .received()
            .stream()
            .noneMatch(request -> request.body().contains("bpms-event-3")),
        "an event of a rolled back transaction was reported");

  }

  @Test
  @DisplayName("Pending updates of one task collapse into one report")
  public void pendingUpdatesCollapse() {

    final var aggregate = aStartedWorkflow();
    final var userTask = userTaskOf(aggregate, "approve");

    final var scheduled = transactions.execute(status -> {
      final var first = publisher
          .publishUserTaskEvent(
              userTask, UserTaskEventKind.UPDATED, "bpms-event-4", OffsetDateTime.now(),
              EventTransaction.CURRENT);
      final var second = publisher
          .publishUserTaskEvent(
              userTask, UserTaskEventKind.UPDATED, "bpms-event-5", OffsetDateTime.now(),
              EventTransaction.CURRENT);
      return List.of(first, second);
    });

    assertTrue(scheduled.get(0), "the first update was not scheduled");
    assertFalse(scheduled.get(1), "the second update was scheduled although one was pending");

  }

  @Test
  @DisplayName("A cockpit server which refuses the report is tried again")
  public void aRefusedReportIsRetried() {

    final var aggregate = aStartedWorkflow();
    CockpitServer.refuseNextRequests(1);

    transactions
        .executeWithoutResult(status -> publisher
            .publishUserTaskEvent(
                userTaskOf(aggregate, "approve"), UserTaskEventKind.CREATED, "bpms-event-6",
                OffsetDateTime.now(), EventTransaction.CURRENT));

    final var request = CockpitServer.awaitRequest("/usertask/created");
    assertTrue(request.body().contains("bpms-event-6"), request.body());

  }

  @Test
  @DisplayName("A task the BPMS has forgotten is not reported")
  public void aForgottenTaskIsNotReported() {

    final var aggregate = aStartedWorkflow();
    bridge.knowsTheTask(false);

    transactions
        .executeWithoutResult(status -> publisher
            .publishUserTaskEvent(
                userTaskOf(aggregate, "approve"), UserTaskEventKind.CREATED, "bpms-event-7",
                OffsetDateTime.now(), EventTransaction.CURRENT));

    CockpitServer.awaitQuiet();
    assertTrue(
        CockpitServer
            .received()
            .stream()
            .noneMatch(request -> request.path().endsWith("/usertask/created")),
        "a task the BPMS no longer knows was reported");

  }

  @Test
  @DisplayName("A completed task is reported without asking the application for details")
  public void aCompletedTaskIsReportedAsIs() {

    final var aggregate = aStartedWorkflow();

    transactions
        .executeWithoutResult(status -> publisher
            .publishUserTaskEvent(
                userTaskOf(aggregate, "approve"), UserTaskEventKind.COMPLETED, "bpms-event-8",
                OffsetDateTime.now(), EventTransaction.CURRENT));

    final var request = CockpitServer.awaitRequest("/usertask/task-1/completed");
    assertTrue(request.body().contains("bpms-event-8"), request.body());
    assertFalse(request.body().contains("\"customer\":\"Anna\""), request.body());

  }

  @Test
  @DisplayName("A workflow the BPMS reported reaches the cockpit, enriched by the application")
  public void aReportedWorkflowReachesTheCockpit() {

    final var aggregate = aStartedWorkflow();

    transactions
        .executeWithoutResult(status -> publisher
            .publishWorkflowEvent(
                new WorkflowReference(
                    RecordingBpmsBridge.ADAPTER_ID, WORKFLOW_MODULE, BPMN_PROCESS, aggregate.getId()
                        .toString(), RecordingBpmsBridge.WORKFLOW_ID),
                WorkflowEventKind.CREATED, "bpms-event-9", OffsetDateTime.now(),
                EventTransaction.NEW));

    final var request = CockpitServer.awaitRequest("/workflow/created");
    assertTrue(request.body().contains("\"customer\":\"Anna\""), request.body());
    assertTrue(request.body().contains("workflow of Anna"), request.body());
    assertTrue(request.body().contains("Order handling"), request.body());
    assertTrue(request.body().contains("\"businessId\":\"4711\""), request.body());

  }

  @Test
  @DisplayName("aggregateChanged reports the workflows of the aggregate")
  public void aggregateChangedReportsTheWorkflows() {

    final var aggregate = aStartedWorkflow();

    transactions
        .executeWithoutResult(status -> workflowService.businessCockpit()
            .aggregateChanged(aggregates.findById(aggregate.getId()).orElseThrow()));

    final var request = CockpitServer.awaitRequest("/workflow/workflow-1/updated");
    assertTrue(request.body().contains("\"updated\":true"), request.body());

  }

  @Test
  @DisplayName("aggregateChanged with task ids reports those tasks")
  public void aggregateChangedReportsTheNamedTasks() {

    final var aggregate = aStartedWorkflow();

    transactions
        .executeWithoutResult(status -> workflowService.businessCockpit()
            .aggregateChanged(
                aggregates.findById(aggregate.getId()).orElseThrow(),
                RecordingBpmsBridge.USER_TASK_ID));

    final var request = CockpitServer.awaitRequest("/usertask/task-1/updated");
    assertTrue(request.body().contains("\"customer\":\"Anna\""), request.body());

  }

  @Test
  @DisplayName("getUserTask answers what the cockpit would show, and reports nothing")
  public void getUserTaskAnswersWithoutReporting() {

    final var aggregate = aStartedWorkflow();

    final var userTask = transactions
        .execute(status -> workflowService.businessCockpit()
            .getUserTask(
                aggregates.findById(aggregate.getId()).orElseThrow(),
                RecordingBpmsBridge.USER_TASK_ID));

    assertNotNull(userTask);
    assertTrue(userTask.isPresent());
    assertEquals(RecordingBpmsBridge.USER_TASK_ID, userTask.get().getId());
    assertEquals("Anna", userTask.get().getDetails().get("customer"));
    assertEquals("Approve the order", userTask.get().getTitle().get("en"));
    CockpitServer.awaitQuiet();
    assertTrue(
        CockpitServer.received().stream().noneMatch(request -> request.path().contains("usertask")),
        "reading a user task reported something to the cockpit");

  }

  @Test
  @DisplayName("The workflow module registers itself while the application starts")
  public void theWorkflowModuleRegistersItself() {

    final var request = CockpitServer.awaitRegistration();

    assertEquals("/bpms/api/v1_1/workflow-module/test-module", request.path());
    assertTrue(
        request.body().contains("http://localhost:8081/test-module"), request.body());
    assertTrue(request.body().contains("\"clerks\""), request.body());
    assertTrue(request.body().contains("\"group\":\"TEAM_LEAD\""), request.body());
    assertTrue(request.body().contains("/task-provider"), request.body());

  }

  @Test
  @DisplayName("A provider matched by its method name may answer with an object of its own")
  public void aProviderMayAnswerWithItsOwnObject() {

    final var aggregate = aStartedWorkflow();

    transactions
        .executeWithoutResult(status -> publisher
            .publishUserTaskEvent(
                userTaskOf(aggregate, "inspect"), UserTaskEventKind.CREATED, "bpms-event-11",
                OffsetDateTime.now(), EventTransaction.CURRENT));

    final var request = CockpitServer.awaitRequest("/usertask/created");
    assertTrue(request.body().contains(OwnUserTaskDetails.COMMENT), request.body());
    assertTrue(request.body().contains("\"assignee\":\"inspector\""), request.body());
    assertTrue(request.body().contains("\"inspected\":true"), request.body());
    assertTrue(request.body().contains("\"notificationDelivery\":\"SUPPRESS\""), request.body());
    // what the provider left alone stays what the BPMS reported
    assertTrue(request.body().contains("\"bpmnProcessVersion\":\"1\""), request.body());

  }

  @Test
  @DisplayName("A task no method serves is reported with what the BPMS said about it")
  public void aTaskWithoutAProviderPassesThrough() {

    final var aggregate = aStartedWorkflow();

    transactions
        .executeWithoutResult(status -> publisher
            .publishUserTaskEvent(
                userTaskOf(aggregate, "somethingNobodyServes"), UserTaskEventKind.CREATED,
                "bpms-event-12", OffsetDateTime.now(), EventTransaction.CURRENT));

    final var request = CockpitServer.awaitRequest("/usertask/created");
    assertTrue(request.body().contains("bpms-event-12"), request.body());
    assertTrue(request.body().contains("Approve the order"), request.body());
    assertTrue(request.body().contains("\"details\":{}"), request.body());

  }

  @Test
  @DisplayName("A provider matched by the BPMN element id runs for that element")
  public void aProviderMatchedByElementIdRuns() {

    final var aggregate = aStartedWorkflow();

    transactions
        .executeWithoutResult(status -> publisher
            .publishUserTaskEvent(
                userTaskOf(aggregate, "decide"), UserTaskEventKind.CREATED, "bpms-event-13",
                OffsetDateTime.now(), EventTransaction.CURRENT));

    final var request = CockpitServer.awaitRequest("/usertask/created");
    assertTrue(request.body().contains("the BPMN element id"), request.body());

  }

  @Test
  @DisplayName("A report which was to ride the caller's transaction says so where there is none")
  public void aReportWithoutATransactionIsRefused() {

    final var aggregate = aStartedWorkflow();

    // the runner the workflow aggregate's writes go through demands the transaction rather
    // than opening one behind the caller's back, and the extension says what that means for a
    // BPMS half in front of the platform's own wording
    final var failure = assertThrows(
        IllegalStateException.class,
        () -> publisher
            .publishUserTaskEvent(
                userTaskOf(aggregate, "approve"), UserTaskEventKind.CREATED, "bpms-event-14",
                OffsetDateTime.now(), EventTransaction.CURRENT));

    assertTrue(failure.getMessage().contains("EventTransaction.CURRENT"), failure.getMessage());
    assertTrue(failure.getMessage().contains("EventTransaction.NEW"), failure.getMessage());
    assertTrue(
        failure.getMessage().contains(RecordingBpmsBridge.ADAPTER_ID), failure.getMessage());
    assertTrue(
        failure.getCause() instanceof IllegalTransactionStateException,
        String.valueOf(failure.getCause()));

  }

  @Test
  @DisplayName("A report naming an aggregate other than the one VanillaBP serves the process with is refused")
  public void aReportForTheWrongAggregateIsRefused() {

    final var aggregate = aStartedWorkflow();

    // the class decides the outbox store and the transaction, so a report which names another
    // one would be committed next to the workflow instead of with it
    final var failure = assertThrows(
        IllegalStateException.class,
        () -> extension
            .publishUserTaskEvent(
                userTaskOf(aggregate, "approve"), UserTaskEventKind.CREATED, "bpms-event-15",
                OffsetDateTime.now(), EventTransaction.NEW, OwnUserTaskDetails.class));

    assertTrue(failure.getMessage().contains(BPMN_PROCESS), failure.getMessage());
    assertTrue(
        failure.getMessage().contains(TestAggregate.class.getName()), failure.getMessage());
    assertTrue(
        failure.getMessage().contains(OwnUserTaskDetails.class.getName()), failure.getMessage());

  }

  @Test
  @DisplayName("An event of an unconfigured BPMS names the adapters which are configured")
  public void anUnknownAdapterIsNamed() {

    final var aggregate = aStartedWorkflow();
    final var elsewhere = new UserTaskReference(
        "another-bpms", WORKFLOW_MODULE, BPMN_PROCESS, aggregate.getId()
            .toString(), RecordingBpmsBridge.WORKFLOW_ID, "task-9", "approve", "Activity_approve");

    transactions
        .executeWithoutResult(status -> publisher
            .publishUserTaskEvent(
                elsewhere, UserTaskEventKind.CREATED, "bpms-event-10", OffsetDateTime.now(),
                EventTransaction.CURRENT));

    CockpitServer.awaitQuiet();
    assertTrue(
        CockpitServer
            .received()
            .stream()
            .noneMatch(request -> request.body().contains("bpms-event-10")),
        "an event of an unknown BPMS was reported");

  }

}
