package io.vanillabp.cockpit.adapter.common.workflow;

import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowEvent;

public interface WorkflowPublishing {
    void publish(WorkflowEvent event);
}
