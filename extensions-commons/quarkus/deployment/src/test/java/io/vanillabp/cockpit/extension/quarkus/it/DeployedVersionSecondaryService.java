package io.vanillabp.cockpit.extension.quarkus.it;

import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetailsProvider;
import io.vanillabp.spi.cockpit.workflow.PrefilledWorkflowDetails;
import io.vanillabp.spi.cockpit.workflow.WorkflowDetails;
import io.vanillabp.spi.cockpit.workflow.WorkflowDetailsProvider;
import io.vanillabp.spi.service.BpmnProcess;
import io.vanillabp.spi.service.WorkflowService;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * A workflow service which calls a process on its own aggregate. This start deployed version 2 of
 * the process at the top and version 1 of the called one.
 * <p>
 * The provider of the workflow serves version 2. The called process never gets a workflow report
 * of its own, so that provider is not checked against version 1. The provider of the called
 * process' user task serves version 1 only. The task belongs to the model of the called process,
 * so it is checked against version 1, and the start runs.
 */
@ApplicationScoped
@WorkflowService(workflowAggregateClass = TestAggregate.class,
    bpmnProcess = @BpmnProcess(bpmnProcessId = DeployedVersionProcessVersions.BPMN_PROCESS),
    secondaryBpmnProcesses = @BpmnProcess(bpmnProcessId = DeployedVersionProcessVersions.CALLED_PROCESS))
public class DeployedVersionSecondaryService {

  /**
   * @param prefilled What the BPMS reported
   * @return The details
   */
  @UserTaskDetailsProvider(taskDefinition = DeployedVersionUserTasks.APPROVE, version = ">=1")
  public UserTaskDetails approveOfEveryVersion(
      final PrefilledUserTaskDetails prefilled) {

    return prefilled;

  }

  /**
   * @param prefilled What the BPMS reported
   * @return The details
   */
  @UserTaskDetailsProvider(taskDefinition = DeployedVersionUserTasks.CHECK, version = "1")
  public UserTaskDetails checkOfTheFirstVersion(
      final PrefilledUserTaskDetails prefilled) {

    return prefilled;

  }

  /**
   * @param prefilled What the BPMS reported
   * @return The details
   */
  @WorkflowDetailsProvider(version = "2")
  public WorkflowDetails workflowOfTheSecondVersion(
      final PrefilledWorkflowDetails prefilled) {

    return prefilled;

  }

}
