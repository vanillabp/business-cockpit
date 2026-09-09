package io.vanillabp.cockpit.extension.quarkus.it;

import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetailsProvider;
import io.vanillabp.spi.service.BpmnProcess;
import io.vanillabp.spi.service.WorkflowService;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * A workflow service which writes the reserved <code>version</code> attribute, the way an
 * application upgrading from version 1 does where it believed the attribute worked.
 */
@ApplicationScoped
@WorkflowService(workflowAggregateClass = TestAggregate.class,
    bpmnProcess = @BpmnProcess(bpmnProcessId = "TestProcess"))
public class VersionedProviderService {

  @UserTaskDetailsProvider(taskDefinition = "approve", version = "2")
  public UserTaskDetails approve(
      final PrefilledUserTaskDetails prefilled) {

    return prefilled;

  }

}
