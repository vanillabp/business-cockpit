package io.vanillabp.cockpit.extension.springboot.test;

import java.time.OffsetDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.transaction.support.TransactionSynchronizationManager;

import io.vanillabp.cockpit.extension.spi.BusinessCockpitBpmsBridge;
import io.vanillabp.cockpit.extension.spi.UserTaskDetailsPrefill;
import io.vanillabp.cockpit.extension.spi.UserTaskReference;
import io.vanillabp.cockpit.extension.spi.WorkflowDetailsPrefill;
import io.vanillabp.cockpit.extension.spi.WorkflowReference;

/**
 * The BPMS half of the extension, played by the test.
 * <p>
 * It answers what an engine would answer and it records what it was asked, so that a test can
 * raise an event and then read what the extension did with it. It is also what proves the
 * commons half needs nothing but this interface from a BPMS.
 */
public class RecordingBpmsBridge implements BusinessCockpitBpmsBridge {

  /** The adapter id of the BPMS double the test application configures. */
  public static final String ADAPTER_ID = "test";

  /** The task the test raises events for. */
  public static final String USER_TASK_ID = "task-1";

  /** The workflow the test raises events for. */
  public static final String WORKFLOW_ID = "workflow-1";

  /** The deployed version this BPMS double runs its workflows on. */
  public static final String PROCESS_VERSION = "1";

  /** The initiator this engine prefills, the way Camunda 7 answers for a case it started. */
  public static final String ENGINE_INITIATOR = "the-engine";

  /** When the engine created the task and started the workflow, as an end reports it. */
  public static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-10-01T08:00:00Z");

  private final AtomicBoolean knowsTheTask = new AtomicBoolean(true);

  private final AtomicBoolean prefillsAnInitiator = new AtomicBoolean(true);

  private final List<UserTaskReference> userTasksRead = new LinkedList<>();

  private final List<Boolean> tasksLookedUpInATransaction = new CopyOnWriteArrayList<>();

  private final AtomicBoolean namesWorkflowsRightAway = new AtomicBoolean(true);

  private final AtomicInteger workflowsStillUnwritten = new AtomicInteger();

  private final AtomicInteger workflowLookups = new AtomicInteger();

  @Override
  public String adapterId() {

    return ADAPTER_ID;

  }

  @Override
  public String adapterType() {

    return "dummy";

  }

  /**
   * @param knows Whether this engine says anything about the task. A test switches it off to prove
   *          that a task an engine is silent about is not reported, and that the silence ends
   *          nothing
   */
  public void knowsTheTask(
      final boolean knows) {

    knowsTheTask.set(knows);

  }

  /**
   * @param prefills Whether this engine names an initiator at all. Camunda 8 and the
   *          Process-Engine-API name none, so a test switches it off to run the way those two
   *          report
   */
  public void prefillsAnInitiator(
      final boolean prefills) {

    prefillsAnInitiator.set(prefills);

  }

  /**
   * @param rightAway Whether this engine names the workflows of a changed aggregate in the
   *          application's transaction. A test switches it off to play a BPMS which writes a
   *          storage behind its engine, the way Camunda 8 does
   */
  public void namesWorkflowsRightAway(
      final boolean rightAway) {

    namesWorkflowsRightAway.set(rightAway);

  }

  /**
   * @param lookups How many of the next lookups of the workflows of an aggregate answer nothing,
   *          the way a storage answers while its exporter is behind
   */
  public void writesTheWorkflowAfter(
      final int lookups) {

    workflowsStillUnwritten.set(lookups);

  }

  /**
   * @return How often the extension asked for the workflows of an aggregate since the last
   *         {@link #forgetLookups()}
   */
  public int workflowLookups() {

    return workflowLookups.get();

  }

  /**
   * @return Every task the extension asked about, in order
   */
  public List<UserTaskReference> userTasksRead() {

    return List.copyOf(userTasksRead);

  }

  /**
   * @return For every lookup of one task of an aggregate, whether a transaction was open while
   *         this engine was asked. A test reads it to see that a question of
   *         <code>BusinessCockpitService</code> asks the BPMS and reads the aggregate in one unit
   *         of work
   */
  public List<Boolean> tasksLookedUpInATransaction() {

    return List.copyOf(tasksLookedUpInATransaction);

  }

  /**
   * Starts a fresh record of the lookups, so that the setup of a test is not mistaken for the run
   * under test.
   */
  public void forgetLookups() {

    tasksLookedUpInATransaction.clear();
    workflowLookups.set(0);

  }

  @Override
  public Optional<UserTaskDetailsPrefill> prefilledUserTaskDetails(
      final UserTaskReference userTask) {

    userTasksRead.add(userTask);
    if (!knowsTheTask.get()) {
      return Optional.empty();
    }
    return Optional
        .of(
            UserTaskDetailsPrefill
                .builder()
                .bpmnProcessVersion("1")
                .businessId("4711")
                .bpmnTaskName("Approve the order")
                .bpmnProcessName("Order handling")
                .initiator(prefillsAnInitiator.get() ? ENGINE_INITIATOR : null)
                .assignee("anna")
                .candidateUsers(List.of("bert"))
                .dueDate(OffsetDateTime.now().plusDays(1))
                .variables(Map.of("amount", 250))
                .createdAt(CREATED_AT)
                .build());

  }

  @Override
  public Optional<WorkflowDetailsPrefill> prefilledWorkflowDetails(
      final WorkflowReference workflow) {

    return Optional
        .of(
            new WorkflowDetailsPrefill(
                "1", "4711", "Order handling", prefillsAnInitiator.get() ? ENGINE_INITIATOR : null, CREATED_AT));

  }

  @Override
  public List<WorkflowReference> workflowsOfAggregate(
      final String workflowModuleId,
      final String bpmnProcessId,
      final String workflowAggregateId) {

    workflowLookups.incrementAndGet();
    if (workflowsStillUnwritten.getAndUpdate(lookups -> Math.max(0, lookups - 1)) > 0) {
      return List.of();
    }
    return List
        .of(
            new WorkflowReference(
                ADAPTER_ID, workflowModuleId, bpmnProcessId, PROCESS_VERSION, workflowAggregateId, WORKFLOW_ID));

  }

  @Override
  public Optional<List<WorkflowReference>> workflowsOfAggregateRightAway(
      final String workflowModuleId,
      final String bpmnProcessId,
      final String workflowAggregateId) {

    if (!namesWorkflowsRightAway.get()) {
      return Optional.empty();
    }
    return BusinessCockpitBpmsBridge.super.workflowsOfAggregateRightAway(workflowModuleId, bpmnProcessId,
        workflowAggregateId);

  }

  @Override
  public List<UserTaskReference> userTasksOfAggregate(
      final String workflowModuleId,
      final String bpmnProcessId,
      final String workflowAggregateId,
      final List<String> userTaskIds) {

    // an engine which says nothing about its tasks names none of them. That is what a search of
    // a storage written behind the engine answers while it is behind, and the test uses it to
    // read what the commons half makes of an empty answer
    if (!knowsTheTask.get()) {
      return List.of();
    }
    return List
        .of(
            userTask(
                workflowModuleId, bpmnProcessId, workflowAggregateId,
                userTaskIds.isEmpty() ? USER_TASK_ID : userTaskIds.getFirst(), "approve"));

  }

  @Override
  public Optional<UserTaskReference> userTaskOfAggregate(
      final String workflowModuleId,
      final String bpmnProcessId,
      final String workflowAggregateId,
      final String userTaskId) {

    tasksLookedUpInATransaction
        .add(Boolean.valueOf(TransactionSynchronizationManager.isActualTransactionActive()));
    return knowsTheTask.get()
        ? Optional
            .of(
                userTask(
                    workflowModuleId, bpmnProcessId, workflowAggregateId, userTaskId, "approve"))
        : Optional.empty();

  }

  /**
   * @param taskDefinition Which details provider the task is to be matched against
   * @param processVersion Which version of the deployed process the task came from,
   *          <code>null</code> for a BPMS which reports none
   * @return A reference to a task of the given aggregate
   */
  public static UserTaskReference userTask(
      final String workflowModuleId,
      final String bpmnProcessId,
      final String workflowAggregateId,
      final String userTaskId,
      final String taskDefinition,
      final String processVersion) {

    return new UserTaskReference(
        ADAPTER_ID, workflowModuleId, bpmnProcessId, processVersion, workflowAggregateId, WORKFLOW_ID, userTaskId, taskDefinition, "Activity_"
            + taskDefinition);

  }

  /**
   * @param taskDefinition Which details provider the task is to be matched against
   * @return A reference to a task of the given aggregate
   */
  public static UserTaskReference userTask(
      final String workflowModuleId,
      final String bpmnProcessId,
      final String workflowAggregateId,
      final String userTaskId,
      final String taskDefinition) {

    return userTask(
        workflowModuleId, bpmnProcessId, workflowAggregateId, userTaskId, taskDefinition,
        PROCESS_VERSION);

  }

}
