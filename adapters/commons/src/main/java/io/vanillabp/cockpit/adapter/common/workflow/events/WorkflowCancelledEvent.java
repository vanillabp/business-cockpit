package io.vanillabp.cockpit.adapter.common.workflow.events;

import java.util.List;

public class WorkflowCancelledEvent extends WorkflowEventImpl {
    public WorkflowCancelledEvent(String workflowModuleId, List<String> i18nLanguages) {
        super(workflowModuleId, i18nLanguages);
    }
}