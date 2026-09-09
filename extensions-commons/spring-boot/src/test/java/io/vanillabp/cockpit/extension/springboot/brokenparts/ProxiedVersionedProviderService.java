package io.vanillabp.cockpit.extension.springboot.brokenparts;

import org.springframework.transaction.annotation.Transactional;

import io.vanillabp.cockpit.extension.springboot.broken.BrokenAggregate;
import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetailsProvider;
import io.vanillabp.spi.service.BpmnProcess;
import io.vanillabp.spi.service.WorkflowService;

/**
 * The same defect as {@link VersionedProviderService}, in a service Spring hands out as a JDK
 * proxy: it implements an interface and its method is transactional, which is the everyday way
 * into a proxy. What the extension checks has to survive that.
 */
@WorkflowService(workflowAggregateClass = BrokenAggregate.class,
    bpmnProcess = @BpmnProcess(bpmnProcessId = "TestProcess"))
public class ProxiedVersionedProviderService implements ProxiedProvider {

  @Override
  @Transactional
  @UserTaskDetailsProvider(taskDefinition = "approve", version = "2")
  public UserTaskDetails approve(
      final PrefilledUserTaskDetails prefilled) {

    return prefilled;

  }

}
