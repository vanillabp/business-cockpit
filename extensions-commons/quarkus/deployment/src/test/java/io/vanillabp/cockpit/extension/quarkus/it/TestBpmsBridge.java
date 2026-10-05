package io.vanillabp.cockpit.extension.quarkus.it;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.vanillabp.cockpit.extension.spi.BusinessCockpitBpmsBridge;
import io.vanillabp.cockpit.extension.spi.UserTaskDetailsPrefill;
import io.vanillabp.cockpit.extension.spi.UserTaskReference;
import io.vanillabp.cockpit.extension.spi.WorkflowDetailsPrefill;
import io.vanillabp.cockpit.extension.spi.WorkflowReference;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionSynchronizationRegistry;

/**
 * The BPMS half of the extension, played by the test. It answers what an engine would answer
 * and proves that the commons half needs nothing but this interface from a BPMS.
 */
@ApplicationScoped
public class TestBpmsBridge implements BusinessCockpitBpmsBridge {

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

  @Inject
  TransactionSynchronizationRegistry transactions;

  private final List<Boolean> tasksLookedUpInATransaction = new CopyOnWriteArrayList<>();

  private final AtomicBoolean prefillsAnInitiator = new AtomicBoolean(true);

  private final AtomicBoolean namesWorkflowsRightAway = new AtomicBoolean(true);

  private final AtomicInteger workflowsStillUnwritten = new AtomicInteger();

  private final AtomicInteger workflowLookups = new AtomicInteger();

  private final AtomicBoolean reportsAChangedUserTaskRightAway = new AtomicBoolean(true);

  private final AtomicInteger tasksStillUnwritten = new AtomicInteger();

  private final AtomicInteger taskReads = new AtomicInteger();

  /**
   * @param rightAway Whether this engine builds the report of a changed user task in the
   *          application's transaction. A test switches it off to play a BPMS which writes a
   *          storage behind its engine, the way Camunda 8 does
   */
  public void reportsAChangedUserTaskRightAway(
      final boolean rightAway) {

    reportsAChangedUserTaskRightAway.set(rightAway);

  }

  /**
   * @param reads How many of the next reads of a user task answer nothing, the way a storage
   *          answers while its exporter is behind
   */
  public void writesTheTaskAfter(
      final int reads) {

    tasksStillUnwritten.set(reads);

  }

  /**
   * @return How often the extension read a user task since the last {@link #forgetLookups()}
   */
  public int taskReads() {

    return taskReads.get();

  }

  @Override
  public boolean reportsAChangedUserTaskRightAway() {

    return reportsAChangedUserTaskRightAway.get();

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
   * @param prefills Whether this engine names an initiator at all. Camunda 8 and the
   *          Process-Engine-API name none, so a test switches it off to run the way those two
   *          report
   */
  public void prefillsAnInitiator(
      final boolean prefills) {

    prefillsAnInitiator.set(prefills);

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
    taskReads.set(0);

  }

  @Override
  public String adapterId() {

    return ADAPTER_ID;

  }

  @Override
  public String adapterType() {

    return "dummy";

  }

  @Override
  public Optional<UserTaskDetailsPrefill> prefilledUserTaskDetails(
      final UserTaskReference userTask) {

    taskReads.incrementAndGet();
    if (tasksStillUnwritten.getAndUpdate(reads -> Math.max(0, reads - 1)) > 0) {
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
                .build());

  }

  @Override
  public Optional<WorkflowDetailsPrefill> prefilledWorkflowDetails(
      final WorkflowReference workflow) {

    return Optional
        .of(
            new WorkflowDetailsPrefill(
                "1", "4711", "Order handling", prefillsAnInitiator.get() ? ENGINE_INITIATOR : null));

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

    return List
        .of(userTask(workflowModuleId, bpmnProcessId, workflowAggregateId, USER_TASK_ID));

  }

  @Override
  public Optional<UserTaskReference> userTaskOfAggregate(
      final String workflowModuleId,
      final String bpmnProcessId,
      final String workflowAggregateId,
      final String userTaskId) {

    tasksLookedUpInATransaction
        .add(
            Boolean
                .valueOf(
                    transactions.getTransactionStatus() != Status.STATUS_NO_TRANSACTION));
    return Optional
        .of(userTask(workflowModuleId, bpmnProcessId, workflowAggregateId, userTaskId));

  }

  /**
   * @return A reference to the one task the test knows
   */
  public static UserTaskReference userTask(
      final String workflowModuleId,
      final String bpmnProcessId,
      final String workflowAggregateId,
      final String userTaskId) {

    return new UserTaskReference(
        ADAPTER_ID, workflowModuleId, bpmnProcessId, PROCESS_VERSION, workflowAggregateId, WORKFLOW_ID, userTaskId, "approve", "Activity_approve");

  }

}
