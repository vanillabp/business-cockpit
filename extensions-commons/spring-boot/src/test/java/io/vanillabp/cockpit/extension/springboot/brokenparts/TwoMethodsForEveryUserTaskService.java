package io.vanillabp.cockpit.extension.springboot.brokenparts;

import io.vanillabp.cockpit.extension.springboot.broken.BrokenAggregate;
import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetailsProvider;
import io.vanillabp.spi.service.BpmnProcess;
import io.vanillabp.spi.service.WorkflowService;

/**
 * A workflow service whose two methods claim every user task, one written as a task definition
 * and one as an element id. Both say the same, and nothing says which one to call, so the
 * application must not start.
 */
@WorkflowService(workflowAggregateClass = BrokenAggregate.class,
    bpmnProcess = @BpmnProcess(bpmnProcessId = "TestProcess"))
public class TwoMethodsForEveryUserTaskService {

  @UserTaskDetailsProvider(taskDefinition = UserTaskDetailsProvider.ALL)
  public UserTaskDetails everyTaskByItsTaskDefinition(
      final PrefilledUserTaskDetails prefilled) {

    return prefilled;

  }

  @UserTaskDetailsProvider(id = UserTaskDetailsProvider.ALL)
  public UserTaskDetails everyTaskByItsElementId(
      final PrefilledUserTaskDetails prefilled) {

    return prefilled;

  }

}
