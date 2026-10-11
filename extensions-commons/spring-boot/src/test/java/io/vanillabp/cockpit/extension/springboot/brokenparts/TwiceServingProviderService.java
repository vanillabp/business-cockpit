package io.vanillabp.cockpit.extension.springboot.brokenparts;

import io.vanillabp.cockpit.extension.springboot.broken.BrokenAggregate;
import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetailsProvider;
import io.vanillabp.spi.service.BpmnProcess;
import io.vanillabp.spi.service.WorkflowService;

/**
 * A workflow service whose two methods claim the same user task. Nothing decides which of them
 * would run, so the application must not start.
 */
@WorkflowService(workflowAggregateClass = BrokenAggregate.class,
    bpmnProcess = @BpmnProcess(bpmnProcessId = "TestProcess"))
public class TwiceServingProviderService {

  @UserTaskDetailsProvider(taskDefinition = "approve")
  public UserTaskDetails oneOfThem(
      final PrefilledUserTaskDetails prefilled) {

    return prefilled;

  }

  @UserTaskDetailsProvider(taskDefinition = "approve")
  public UserTaskDetails theOtherOne(
      final PrefilledUserTaskDetails prefilled) {

    return prefilled;

  }

}
