package io.vanillabp.cockpit.extension.quarkus.it;

import io.vanillabp.spi.service.BpmnProcess;
import io.vanillabp.spi.service.WorkflowService;
import jakarta.enterprise.context.ApplicationScoped;

/** What ties the BPMN process 'SecondProcess' to the aggregate living in the second store. */
@ApplicationScoped
@WorkflowService(workflowAggregateClass = SecondAggregate.class,
    bpmnProcess = @BpmnProcess(bpmnProcessId = "SecondProcess"))
public class SecondWorkflowService {
}
