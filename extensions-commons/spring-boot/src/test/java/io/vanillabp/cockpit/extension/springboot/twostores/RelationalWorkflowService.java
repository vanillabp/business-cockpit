package io.vanillabp.cockpit.extension.springboot.twostores;

import org.springframework.stereotype.Service;

import io.vanillabp.spi.service.BpmnProcess;
import io.vanillabp.spi.service.WorkflowService;

/**
 * What ties the BPMN process 'TestProcess' to the aggregate living in the first store. An event
 * a BPMS observed names this process and no class, and this annotation is what turns the one
 * into the other.
 */
@Service
@WorkflowService(workflowAggregateClass = RelationalAggregate.class,
    bpmnProcess = @BpmnProcess(bpmnProcessId = "TestProcess"))
public class RelationalWorkflowService {
}
