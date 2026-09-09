package io.vanillabp.cockpit.extension.springboot.twostores;

import org.springframework.stereotype.Service;

import io.vanillabp.spi.service.BpmnProcess;
import io.vanillabp.spi.service.WorkflowService;

/** The same for the BPMN process whose aggregate lives in the second store. */
@Service
@WorkflowService(workflowAggregateClass = DocumentAggregate.class,
    bpmnProcess = @BpmnProcess(bpmnProcessId = "SecondProcess"))
public class DocumentWorkflowService {
}
