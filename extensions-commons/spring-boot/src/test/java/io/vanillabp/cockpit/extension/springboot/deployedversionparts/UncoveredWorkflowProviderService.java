package io.vanillabp.cockpit.extension.springboot.deployedversionparts;

import io.vanillabp.cockpit.extension.springboot.deployedversion.DeployedVersionAggregate;
import io.vanillabp.cockpit.extension.springboot.deployedversion.DeployedVersionApplication;
import io.vanillabp.spi.cockpit.workflow.PrefilledWorkflowDetails;
import io.vanillabp.spi.cockpit.workflow.WorkflowDetails;
import io.vanillabp.spi.cockpit.workflow.WorkflowDetailsProvider;
import io.vanillabp.spi.service.BpmnProcess;
import io.vanillabp.spi.service.WorkflowService;

/**
 * A provider of the workflow which serves version 1 only, while this start deployed version 2.
 */
@WorkflowService(workflowAggregateClass = DeployedVersionAggregate.class,
    bpmnProcess = @BpmnProcess(bpmnProcessId = DeployedVersionApplication.BPMN_PROCESS))
public class UncoveredWorkflowProviderService {

  /**
   * @param prefilled What the BPMS reported
   * @return The details
   */
  @WorkflowDetailsProvider(version = "1")
  public WorkflowDetails workflowOfTheFirstVersion(
      final PrefilledWorkflowDetails prefilled) {

    return prefilled;

  }

}
