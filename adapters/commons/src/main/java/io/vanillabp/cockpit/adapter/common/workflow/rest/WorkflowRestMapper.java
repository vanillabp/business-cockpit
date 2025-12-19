package io.vanillabp.cockpit.adapter.common.workflow.rest;

import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCancelledEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCompletedEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCreatedEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowUpdatedEvent;

// @Mapper
public interface WorkflowRestMapper {
    io.vanillabp.cockpit.bpms.api.v1_1.WorkflowCancelledEvent map(WorkflowCancelledEvent workflowCancelledEvent);
    io.vanillabp.cockpit.bpms.api.v1_1.WorkflowCompletedEvent map(WorkflowCompletedEvent workflowCompletedEvent);
    io.vanillabp.cockpit.bpms.api.v1_1.WorkflowCreatedEvent map(WorkflowCreatedEvent workflowCreatedEvent);
    io.vanillabp.cockpit.bpms.api.v1_1.WorkflowUpdatedEvent map(WorkflowUpdatedEvent workflowUpdatedEvent);
}
