package io.vanillabp.cockpit.extension.springboot.twostores;

import org.springframework.stereotype.Service;

import io.vanillabp.spi.service.BpmnProcess;
import io.vanillabp.spi.service.WorkflowService;

/** Ties the BPMN process 'SecondProcess' to the aggregate in the second store. */
@Service
@WorkflowService(workflowAggregateClass = DocumentAggregate.class,
    bpmnProcess = @BpmnProcess(bpmnProcessId = "SecondProcess"))
public class DocumentWorkflowService {
}
