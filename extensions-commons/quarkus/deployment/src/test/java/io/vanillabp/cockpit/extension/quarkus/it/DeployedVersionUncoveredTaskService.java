package io.vanillabp.cockpit.extension.quarkus.it;

import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetailsProvider;
import io.vanillabp.spi.service.BpmnProcess;
import io.vanillabp.spi.service.WorkflowService;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * A provider of the user task which serves version 1 only, while this start deployed version 2.
 */
@ApplicationScoped
@WorkflowService(workflowAggregateClass = TestAggregate.class,
    bpmnProcess = @BpmnProcess(bpmnProcessId = DeployedVersionProcessVersions.BPMN_PROCESS))
public class DeployedVersionUncoveredTaskService {

  /**
   * @param prefilled What the BPMS reported
   * @return The details
   */
  @UserTaskDetailsProvider(taskDefinition = DeployedVersionUserTasks.APPROVE, version = "1")
  public UserTaskDetails approveOfTheFirstVersion(
      final PrefilledUserTaskDetails prefilled) {

    return prefilled;

  }

}
