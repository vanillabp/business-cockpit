package io.vanillabp.cockpit.adapter.common.workflow.events;

import java.util.List;

public class WorkflowCreatedEvent extends WorkflowEventImpl {
    public WorkflowCreatedEvent(String workflowModuleId, List<String> i18nLanguages) {
        super(workflowModuleId, i18nLanguages);
    }
}

