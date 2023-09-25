package io.vanillabp.cockpit.adapter.camunda7.workflow.events;

import io.vanillabp.cockpit.bpms.api.v1.WorkflowCompletedEvent;

public class WorkflowCancelled extends WorkflowCompleted {

    public WorkflowCancelled(
            final WorkflowCompletedEvent event,
            final String apiVersion) {
        super(event, apiVersion);
    }

}
