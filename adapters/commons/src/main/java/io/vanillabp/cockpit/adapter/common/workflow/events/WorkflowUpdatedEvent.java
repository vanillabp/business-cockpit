package io.vanillabp.cockpit.adapter.common.workflow.events;

import java.util.List;

public class WorkflowUpdatedEvent extends WorkflowCreatedEvent {

    public WorkflowUpdatedEvent(String workflowModuleId, List<String> i18nLanguages, String apiVersion) {
        super(workflowModuleId, i18nLanguages, apiVersion);
    }
}