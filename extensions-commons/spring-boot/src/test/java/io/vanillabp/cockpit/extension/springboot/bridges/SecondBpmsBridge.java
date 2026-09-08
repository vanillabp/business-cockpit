package io.vanillabp.cockpit.extension.springboot.bridges;

import java.util.List;
import java.util.Optional;

import io.vanillabp.cockpit.extension.spi.BusinessCockpitBpmsBridge;
import io.vanillabp.cockpit.extension.spi.UserTaskDetailsPrefill;
import io.vanillabp.cockpit.extension.spi.UserTaskReference;
import io.vanillabp.cockpit.extension.spi.WorkflowDetailsPrefill;
import io.vanillabp.cockpit.extension.spi.WorkflowReference;

/** A BPMS half which knows nothing: the test asks which bridge serves an adapter, no more. */
public class SecondBpmsBridge implements BusinessCockpitBpmsBridge {

  private final String adapterId;

  public SecondBpmsBridge(
      final String adapterId) {

    this.adapterId = adapterId;

  }

  @Override
  public String adapterId() {

    return adapterId;

  }

  @Override
  public String adapterType() {

    return "dummy";

  }

  @Override
  public Optional<UserTaskDetailsPrefill> prefilledUserTaskDetails(
      final UserTaskReference userTask) {

    return Optional.empty();

  }

  @Override
  public Optional<WorkflowDetailsPrefill> prefilledWorkflowDetails(
      final WorkflowReference workflow) {

    return Optional.empty();

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
