package io.vanillabp.cockpit.adapter.camunda7.workflow.events;

import java.util.List;

import io.vanillabp.cockpit.bpms.api.v1.WorkflowCreatedOrUpdatedEvent;

public class WorkflowUpdated extends WorkflowCreated {

    public WorkflowUpdated(
            final WorkflowCreatedOrUpdatedEvent event,
            final String workflowModuleId,
            final List<String> i18nLanguages) {
        
        super(event, workflowModuleId, i18nLanguages);
        event.setUpdated(Boolean.TRUE);
        
    }

}
