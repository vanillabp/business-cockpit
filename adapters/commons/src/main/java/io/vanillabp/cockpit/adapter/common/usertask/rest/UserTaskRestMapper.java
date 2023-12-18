package io.vanillabp.cockpit.adapter.common.usertask.rest;


import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskActivatedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCancelledEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCompletedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCreatedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskSuspendedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskUpdatedEvent;

// @Mapper
public interface UserTaskRestMapper {
    io.vanillabp.cockpit.bpms.api.v1.UserTaskActivatedEvent map(UserTaskActivatedEvent userTaskActivatedEvent);
    io.vanillabp.cockpit.bpms.api.v1.UserTaskCancelledEvent map(UserTaskCancelledEvent userTaskCancelledEvent);
    io.vanillabp.cockpit.bpms.api.v1.UserTaskCompletedEvent map(UserTaskCompletedEvent userTaskCompletedEvent);
    io.vanillabp.cockpit.bpms.api.v1.UserTaskSuspendedEvent map(UserTaskSuspendedEvent userTaskSuspendedEvent);

    // @Mapping(target = "updated", constant = "false")
    io.vanillabp.cockpit.bpms.api.v1.UserTaskCreatedOrUpdatedEvent map(UserTaskCreatedEvent userTaskCreatedEvent);

    // @Mapping(target = "updated", constant = "true")
    io.vanillabp.cockpit.bpms.api.v1.UserTaskCreatedOrUpdatedEvent map(UserTaskUpdatedEvent userTaskCreatedEvent);
}
