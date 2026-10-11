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
 * Providers whose ranges cover the deployed version 2, for the user task and for the workflow.
 */
@WorkflowService(workflowAggregateClass = DeployedVersionAggregate.class,
    bpmnProcess = @BpmnProcess(bpmnProcessId = DeployedVersionApplication.BPMN_PROCESS))
public class CoveringProvidersService {

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
  @WorkflowDetailsProvider(version = "2")
  public WorkflowDetails workflowOfTheSecondVersion(
      final PrefilledWorkflowDetails prefilled) {

    return prefilled;

  }

}
