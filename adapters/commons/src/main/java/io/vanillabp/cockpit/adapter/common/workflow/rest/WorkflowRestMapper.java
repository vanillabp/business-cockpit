package io.vanillabp.cockpit.adapter.common.workflow.rest;

import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCancelledEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCompletedEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCreatedEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowUpdatedEvent;

// @Mapper
public interface WorkflowRestMapper {
    io.vanillabp.cockpit.bpms.api.v1.WorkflowCancelledEvent map(WorkflowCancelledEvent workflowCancelledEvent);
    io.vanillabp.cockpit.bpms.api.v1.WorkflowCompletedEvent map(WorkflowCompletedEvent workflowCompletedEvent);

//    @Mapping(target = "updated", constant = "false")
    io.vanillabp.cockpit.bpms.api.v1.WorkflowCreatedOrUpdatedEvent map(WorkflowCreatedEvent workflowCreatedEvent);
//    @Mapping(target = "updated", constant = "true")
    io.vanillabp.cockpit.bpms.api.v1.WorkflowCreatedOrUpdatedEvent map(WorkflowUpdatedEvent workflowUpdatedEvent);
}
