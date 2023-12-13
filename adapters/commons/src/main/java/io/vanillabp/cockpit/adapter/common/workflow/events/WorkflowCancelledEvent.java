package io.vanillabp.cockpit.adapter.common.workflow.events;

public class WorkflowCancelledEvent extends WorkflowLifecycleEvent {
    public WorkflowCancelledEvent(String apiVersion){
        super(apiVersion);
    }
}