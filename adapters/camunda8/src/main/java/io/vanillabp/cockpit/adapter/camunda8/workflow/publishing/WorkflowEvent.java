package io.vanillabp.cockpit.adapter.camunda8.workflow.publishing;

import org.springframework.context.ApplicationEvent;

public class WorkflowEvent extends ApplicationEvent {
    private static final long serialVersionUID = 1L;

    final io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowEvent event;

    public WorkflowEvent(
            final Object source,
            final io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowEvent event) {

        super(source);
        this.event = event;

    }

    public io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowEvent getEvent() {
        return event;
    }
    
}
