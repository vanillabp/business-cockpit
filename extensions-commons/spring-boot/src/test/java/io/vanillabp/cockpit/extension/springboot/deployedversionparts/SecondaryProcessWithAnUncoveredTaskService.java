package io.vanillabp.cockpit.extension.springboot.deployedversionparts;

import io.vanillabp.cockpit.extension.springboot.deployedversion.DeployedVersionAggregate;
import io.vanillabp.cockpit.extension.springboot.deployedversion.DeployedVersionApplication;
import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetailsProvider;
import io.vanillabp.spi.cockpit.workflow.PrefilledWorkflowDetails;
import io.vanillabp.spi.cockpit.workflow.WorkflowDetails;
import io.vanillabp.spi.cockpit.workflow.WorkflowDetailsProvider;
import io.vanillabp.spi.service.BpmnProcess;
import io.vanillabp.spi.service.WorkflowService;

/**
 * A workflow service which calls a process on its own aggregate. This start deployed version 2 of
 * the process at the top and version 1 of the called one. The provider of the called process'
 * user task serves version 2 only. That is the version of the process at the top, and it does
 * not count for a task of the called process, so the start ends.
 */
@WorkflowService(workflowAggregateClass = DeployedVersionAggregate.class,
    bpmnProcess = @BpmnProcess(bpmnProcessId = DeployedVersionApplication.BPMN_PROCESS),
    secondaryBpmnProcesses = @BpmnProcess(bpmnProcessId = DeployedVersionApplication.CALLED_PROCESS))
public class SecondaryProcessWithAnUncoveredTaskService {

  /**
   * @param prefilled What the BPMS reported
   * @return The details
   */
  @UserTaskDetailsProvider(taskDefinition = DeployedVersionApplication.APPROVE, version = ">=1")
  public UserTaskDetails approveOfEveryVersion(
      final PrefilledUserTaskDetails prefilled) {

    return prefilled;

  }

  /**
   * @param prefilled What the BPMS reported
   * @return The details
   */
  @UserTaskDetailsProvider(taskDefinition = DeployedVersionApplication.CHECK, version = "2")
  public UserTaskDetails checkOfTheSecondVersion(
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
