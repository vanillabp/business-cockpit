package io.vanillabp.cockpit.extension.springboot.test;

import java.time.OffsetDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

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

  private final AtomicBoolean knowsTheTask = new AtomicBoolean(true);

  private final List<UserTaskReference> userTasksRead = new LinkedList<>();

  @Override
  public String adapterId() {

    return ADAPTER_ID;

  }

  @Override
  public String adapterType() {

    return "dummy";

  }

  /**
   * @param knows Whether the engine still knows the task - what a test switches off to prove
   *          that a task which ended while its entry waited is not reported
   */
  public void knowsTheTask(
      final boolean knows) {

    knowsTheTask.set(knows);

  }

  /**
   * @return Every task the extension asked about, in order
   */
  public List<UserTaskReference> userTasksRead() {

    return List.copyOf(userTasksRead);

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
                .initiator("the-engine")
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
        .of(new WorkflowDetailsPrefill("1", "4711", "Order handling", "the-engine"));

  }

  @Override
  public List<WorkflowReference> workflowsOfAggregate(
      final String workflowModuleId,
      final String bpmnProcessId,
      final String workflowAggregateId) {

    return List
        .of(
            new WorkflowReference(
                ADAPTER_ID, workflowModuleId, bpmnProcessId, workflowAggregateId, WORKFLOW_ID));

  }

  @Override
  public List<UserTaskReference> userTasksOfAggregate(
      final String workflowModuleId,
      final String bpmnProcessId,
      final String workflowAggregateId,
      final List<String> userTaskIds) {

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

    return knowsTheTask.get()
        ? Optional
            .of(
                userTask(
                    workflowModuleId, bpmnProcessId, workflowAggregateId, userTaskId, "approve"))
        : Optional.empty();

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

    return new UserTaskReference(
        ADAPTER_ID, workflowModuleId, bpmnProcessId, workflowAggregateId, WORKFLOW_ID, userTaskId, taskDefinition, "Activity_"
            + taskDefinition);

  }

}
