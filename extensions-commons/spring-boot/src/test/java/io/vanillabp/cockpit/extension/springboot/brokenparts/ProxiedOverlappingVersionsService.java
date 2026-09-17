package io.vanillabp.cockpit.extension.springboot.brokenparts;

import org.springframework.transaction.annotation.Transactional;

import io.vanillabp.cockpit.extension.springboot.broken.BrokenAggregate;
import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetailsProvider;
import io.vanillabp.spi.service.BpmnProcess;
import io.vanillabp.spi.service.WorkflowService;

/**
 * The same defect as {@link OverlappingVersionsProviderService}, in a service Spring hands out
 * as a JDK proxy. It implements an interface and its methods are transactional, which is the
 * everyday way into a proxy. What VanillaBP reads off these methods has to survive that.
 */
@WorkflowService(workflowAggregateClass = BrokenAggregate.class,
    bpmnProcess = @BpmnProcess(bpmnProcessId = "TestProcess"))
public class ProxiedOverlappingVersionsService implements ProxiedProvider {

  @Override
  @Transactional
  @UserTaskDetailsProvider(taskDefinition = "approve", version = "1-3")
  public UserTaskDetails approveUpToTheThird(
      final PrefilledUserTaskDetails prefilled) {

    return prefilled;

  }

  @Override
  @Transactional
  @UserTaskDetailsProvider(taskDefinition = "approve", version = ">2")
  public UserTaskDetails approveFromTheThird(
      final PrefilledUserTaskDetails prefilled) {

    return prefilled;

  }

}
