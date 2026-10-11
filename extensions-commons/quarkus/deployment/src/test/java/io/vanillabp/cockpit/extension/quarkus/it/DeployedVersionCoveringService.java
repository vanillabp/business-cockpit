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
 * Providers whose ranges cover the deployed version 2, and one for a task the model does not have,
 * of a version the BPMS does not hold.
 */
@ApplicationScoped
@WorkflowService(workflowAggregateClass = TestAggregate.class,
    bpmnProcess = @BpmnProcess(bpmnProcessId = DeployedVersionProcessVersions.BPMN_PROCESS))
public class DeployedVersionCoveringService {

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
  @UserTaskDetailsProvider(taskDefinition = DeployedVersionUserTasks.AUDIT, version = "9")
  public UserTaskDetails auditOfAVersionNobodyDeploys(
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
