package io.vanillabp.cockpit.extension.quarkus.it;

import io.vanillabp.spi.cockpit.workflow.PrefilledWorkflowDetails;
import io.vanillabp.spi.cockpit.workflow.WorkflowDetails;
import io.vanillabp.spi.cockpit.workflow.WorkflowDetailsProvider;
import io.vanillabp.spi.service.BpmnProcess;
import io.vanillabp.spi.service.WorkflowService;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * A provider of the workflow which serves version 1 only, while this start deployed version 2.
 */
@ApplicationScoped
@WorkflowService(workflowAggregateClass = TestAggregate.class,
    bpmnProcess = @BpmnProcess(bpmnProcessId = DeployedVersionProcessVersions.BPMN_PROCESS))
public class DeployedVersionUncoveredWorkflowService {

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
