package io.vanillabp.cockpit.adapter.camunda8.workflow.publishing;

import org.springframework.context.ApplicationEvent;

public class ProcessWorkflowEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    public ProcessWorkflowEvent(Object source) {
        super(source);
    }

}
