package io.vanillabp.cockpit.adapter.common.usertask.rest;


import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskActivatedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCancelledEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCompletedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCreatedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskSuspendedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskUpdatedEvent;

// @Mapper
public interface UserTaskRestMapper {
    io.vanillabp.cockpit.bpms.api.v1_1.UserTaskActivatedEvent map(UserTaskActivatedEvent userTaskActivatedEvent);
    io.vanillabp.cockpit.bpms.api.v1_1.UserTaskCancelledEvent map(UserTaskCancelledEvent userTaskCancelledEvent);
    io.vanillabp.cockpit.bpms.api.v1_1.UserTaskCompletedEvent map(UserTaskCompletedEvent userTaskCompletedEvent);
    io.vanillabp.cockpit.bpms.api.v1_1.UserTaskSuspendedEvent map(UserTaskSuspendedEvent userTaskSuspendedEvent);

    io.vanillabp.cockpit.bpms.api.v1_1.UserTaskCreatedEvent map(UserTaskCreatedEvent userTaskCreatedEvent);

    io.vanillabp.cockpit.bpms.api.v1_1.UserTaskUpdatedEvent map(UserTaskUpdatedEvent userTaskCreatedEvent);
}
