package io.vanillabp.cockpit.adapter.common.workflow.events;

import java.util.List;

public class WorkflowUpdatedEvent extends WorkflowEventImpl {
    public WorkflowUpdatedEvent(String workflowModuleId, List<String> i18nLanguages) {
        super(workflowModuleId, i18nLanguages);
    }
}