package io.vanillabp.cockpit.extension.quarkus.it;

import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetailsProvider;
import io.vanillabp.spi.service.BpmnProcess;
import io.vanillabp.spi.service.WorkflowService;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * A workflow service whose two methods claim the same user task. Which of them would run is
 * decided by nothing, so the application is not allowed to start.
 */
@ApplicationScoped
@WorkflowService(workflowAggregateClass = TestAggregate.class,
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
