package io.vanillabp.cockpit.extension.quarkus.it;

import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetailsProvider;
import io.vanillabp.spi.service.BpmnProcess;
import io.vanillabp.spi.service.WorkflowService;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * A workflow service whose two methods claim every user task, one written as a task definition
 * and one as an element id. Both say the same thing, nothing says which of them to call, so the
 * application is not allowed to start.
 */
@ApplicationScoped
@WorkflowService(workflowAggregateClass = TestAggregate.class,
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
