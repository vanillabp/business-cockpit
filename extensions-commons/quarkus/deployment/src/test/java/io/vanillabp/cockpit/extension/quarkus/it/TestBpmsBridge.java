package io.vanillabp.cockpit.extension.quarkus.it;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.vanillabp.cockpit.extension.spi.BusinessCockpitBpmsBridge;
import io.vanillabp.cockpit.extension.spi.UserTaskDetailsPrefill;
import io.vanillabp.cockpit.extension.spi.UserTaskReference;
import io.vanillabp.cockpit.extension.spi.WorkflowDetailsPrefill;
import io.vanillabp.cockpit.extension.spi.WorkflowReference;
import jakarta.enterprise.context.ApplicationScoped;

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

    return Optional.of(new WorkflowDetailsPrefill("1", "4711", "Order handling", "the-engine"));

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
        .of(userTask(workflowModuleId, bpmnProcessId, workflowAggregateId, USER_TASK_ID));

  }

  @Override
  public Optional<UserTaskReference> userTaskOfAggregate(
      final String workflowModuleId,
      final String bpmnProcessId,
      final String workflowAggregateId,
      final String userTaskId) {

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
        ADAPTER_ID, workflowModuleId, bpmnProcessId, workflowAggregateId, WORKFLOW_ID, userTaskId, "approve", "Activity_approve");

  }

}
