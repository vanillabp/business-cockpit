package io.vanillabp.cockpit.adapter.common.workflow.events;

public class WorkflowCompletedEvent extends WorkflowLifecycleEvent {
    public WorkflowCompletedEvent(String apiVersion) {
        super(apiVersion);
    }
}