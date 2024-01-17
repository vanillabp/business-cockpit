package io.vanillabp.cockpit.adapter.camunda8.workflow.publishing;

import org.springframework.context.ApplicationEvent;

public class WorkflowAfterTransactionEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    private final String processInstanceId;
    
    public WorkflowAfterTransactionEvent(
            final Object source,
            final String processInstanceId) {
        
        super(source);
        this.processInstanceId = processInstanceId;
        
    }
    
    public String getProcessInstanceId() {
        return processInstanceId;
    }

}
