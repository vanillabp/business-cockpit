package io.vanillabp.cockpit.extension.quarkus.it;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

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

    return List
        .of(
            new WorkflowReference(
                ADAPTER_ID, workflowModuleId, bpmnProcessId, PROCESS_VERSION, workflowAggregateId, WORKFLOW_ID));

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
