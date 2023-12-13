package io.vanillabp.cockpit.adapter.camunda7.workflow.publishing;

import org.springframework.context.ApplicationEvent;

public class WorkflowEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    final io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowEvent event;

    final String apiVersion;

    public WorkflowEvent(
            final Object source,
            final io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowEvent event,
            final String apiVersion) {

        super(source);
        this.event = event;
        this.apiVersion = apiVersion;

    }

    public io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowEvent getEvent() {
        return event;
    }

    public String getApiVersion() {
        return apiVersion;
    }

}
