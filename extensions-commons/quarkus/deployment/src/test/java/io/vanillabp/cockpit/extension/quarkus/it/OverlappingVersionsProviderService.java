package io.vanillabp.cockpit.extension.quarkus.it;

import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetailsProvider;
import io.vanillabp.spi.service.BpmnProcess;
import io.vanillabp.spi.service.WorkflowService;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Two providers of one user task whose version ranges overlap. Versions of their own are what
 * lets two methods name the same task, so ranges which share a version leave nothing to decide
 * by. Version three is served by both of these.
 */
@ApplicationScoped
@WorkflowService(workflowAggregateClass = TestAggregate.class,
    bpmnProcess = @BpmnProcess(bpmnProcessId = "TestProcess"))
public class OverlappingVersionsProviderService {

  /**
   * @param prefilled What the BPMS reported
   * @return The details
   */
  @UserTaskDetailsProvider(taskDefinition = "approve", version = "1-3")
  public UserTaskDetails approveUpToTheThird(
      final PrefilledUserTaskDetails prefilled) {

    return prefilled;

  }

  /**
   * @param prefilled What the BPMS reported
   * @return The details
   */
  @UserTaskDetailsProvider(taskDefinition = "approve", version = ">2")
  public UserTaskDetails approveFromTheThird(
      final PrefilledUserTaskDetails prefilled) {

    return prefilled;

  }

}
