package io.vanillabp.cockpit.extension.quarkus.it;

import io.vanillabp.spi.service.BpmnProcess;
import io.vanillabp.spi.service.WorkflowService;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * A workflow service without any details provider. Providers are optional.
 */
@ApplicationScoped
@WorkflowService(workflowAggregateClass = TestAggregate.class,
    bpmnProcess = @BpmnProcess(bpmnProcessId = DeployedVersionProcessVersions.BPMN_PROCESS))
public class DeployedVersionNoProviderService {

}
