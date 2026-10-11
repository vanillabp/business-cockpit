package io.vanillabp.cockpit.extension.springboot.deployedversionparts;

import io.vanillabp.cockpit.extension.springboot.deployedversion.DeployedVersionAggregate;
import io.vanillabp.cockpit.extension.springboot.deployedversion.DeployedVersionApplication;
import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetailsProvider;
import io.vanillabp.spi.service.BpmnProcess;
import io.vanillabp.spi.service.WorkflowService;

/**
 * A provider for a task the deployed model does not have, of a version the BPMS does not hold.
 */
@WorkflowService(workflowAggregateClass = DeployedVersionAggregate.class,
    bpmnProcess = @BpmnProcess(bpmnProcessId = DeployedVersionApplication.BPMN_PROCESS))
public class ProviderOfATaskTheModelLacksService {

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
  @UserTaskDetailsProvider(taskDefinition = DeployedVersionApplication.AUDIT, version = "9")
  public UserTaskDetails auditOfAVersionNobodyDeploys(
      final PrefilledUserTaskDetails prefilled) {

    return prefilled;

  }

}
