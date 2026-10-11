package io.vanillabp.cockpit.extension.springboot.deployedversionparts;

import io.vanillabp.cockpit.extension.springboot.deployedversion.DeployedVersionAggregate;
import io.vanillabp.cockpit.extension.springboot.deployedversion.DeployedVersionApplication;
import io.vanillabp.spi.service.BpmnProcess;
import io.vanillabp.spi.service.WorkflowService;

/**
 * A workflow service without any details provider. Providers are optional.
 */
@WorkflowService(workflowAggregateClass = DeployedVersionAggregate.class,
    bpmnProcess = @BpmnProcess(bpmnProcessId = DeployedVersionApplication.BPMN_PROCESS))
public class NoProviderService {


}
