package io.vanillabp.cockpit.adapter.common.workflow.events;

import java.util.List;

public class WorkflowCompletedEvent extends WorkflowEventImpl {
    public WorkflowCompletedEvent(String workflowModuleId, List<String> i18nLanguages) {
        super(workflowModuleId, i18nLanguages);
    }
}