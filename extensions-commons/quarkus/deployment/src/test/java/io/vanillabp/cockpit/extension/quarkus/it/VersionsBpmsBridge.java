package io.vanillabp.cockpit.extension.quarkus.it;

import java.util.List;
import java.util.Optional;

import io.vanillabp.cockpit.extension.spi.BusinessCockpitBpmsBridge;
import io.vanillabp.cockpit.extension.spi.UserTaskDetailsPrefill;
import io.vanillabp.cockpit.extension.spi.UserTaskReference;
import io.vanillabp.cockpit.extension.spi.WorkflowDetailsPrefill;
import io.vanillabp.cockpit.extension.spi.WorkflowReference;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * The BPMS half of the versioned application, played by the test. It answers about a task and a
 * workflow whatever the reference asked for, and it reports the version the reference carried.
 * <p>
 * That is what a real BPMS half does too: the version travels with the event the engine sent,
 * so the version a prefill reports and the version a details provider is picked by are the same
 * deployment.
 */
@ApplicationScoped
public class VersionsBpmsBridge implements BusinessCockpitBpmsBridge {

  /** The adapter id the test application configures for its BPMS double. */
  public static final String ADAPTER_ID = "test";

  /** The BPMN name of the user task, which is the title where no provider wrote one. */
  public static final String TASK_NAME = "Approve the order";

  /** The BPMN name of the process, which is the title of a workflow nobody reported about. */
  public static final String PROCESS_NAME = "Order handling";

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
                .bpmnProcessVersion(userTask.processVersion())
                .businessId("4711")
                .bpmnTaskName(TASK_NAME)
                .bpmnProcessName(PROCESS_NAME)
                .build());

  }

  @Override
  public Optional<WorkflowDetailsPrefill> prefilledWorkflowDetails(
      final WorkflowReference workflow) {

    return Optional
        .of(
            new WorkflowDetailsPrefill(
                workflow.processVersion(), "4711", PROCESS_NAME, "the-engine"));

  }

  @Override
  public List<WorkflowReference> workflowsOfAggregate(
      final String workflowModuleId,
      final String bpmnProcessId,
      final String workflowAggregateId) {

    return List.of();

  }

  @Override
  public List<UserTaskReference> userTasksOfAggregate(
      final String workflowModuleId,
      final String bpmnProcessId,
      final String workflowAggregateId,
      final List<String> userTaskIds) {

    return List.of();

  }

  @Override
  public Optional<UserTaskReference> userTaskOfAggregate(
      final String workflowModuleId,
      final String bpmnProcessId,
      final String workflowAggregateId,
      final String userTaskId) {

    return Optional.empty();

  }

}
