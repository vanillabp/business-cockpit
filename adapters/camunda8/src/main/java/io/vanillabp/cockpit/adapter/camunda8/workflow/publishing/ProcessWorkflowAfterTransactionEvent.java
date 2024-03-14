package io.vanillabp.cockpit.adapter.camunda8.workflow.publishing;

import org.springframework.context.ApplicationEvent;

public class ProcessWorkflowAfterTransactionEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    public ProcessWorkflowAfterTransactionEvent(Object source) {
        super(source);
    }

}
