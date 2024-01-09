package io.vanillabp.cockpit.adapter.common.workflow;

import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowEvent;

import java.util.List;

public interface WorkflowPublishing {
    void publish(final List<WorkflowEvent> events);
}
