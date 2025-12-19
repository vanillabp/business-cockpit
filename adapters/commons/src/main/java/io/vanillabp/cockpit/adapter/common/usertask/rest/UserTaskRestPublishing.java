package io.vanillabp.cockpit.adapter.common.usertask.rest;

import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.cockpit.adapter.common.usertask.UserTaskPublishing;
import io.vanillabp.cockpit.adapter.common.usertask.UserTaskPublishingBase;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskActivatedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCancelledEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCompletedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCreatedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskSuspendedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.v1_1.BpmsApi;
import io.vanillabp.springboot.adapter.VanillaBpProperties;
import jakarta.annotation.PostConstruct;
import java.util.Optional;

public class UserTaskRestPublishing extends UserTaskPublishingBase implements UserTaskPublishing {

    private final Optional<BpmsApi> bpmsApi;

    private final UserTaskRestMapper userTaskMapper;

    public UserTaskRestPublishing(
            final String workerId,
            final Optional<BpmsApi> bpmsApiV1_1,
            final VanillaBpCockpitProperties properties,
            final UserTaskRestMapper userTaskMapper) {

        super(workerId, properties);
        this.bpmsApi = bpmsApiV1_1;
        this.userTaskMapper = userTaskMapper;
        
    }

    @PostConstruct
    @javax.annotation.PostConstruct
    public void validateAutowiring() {
        
        if (bpmsApi.isPresent()) {
            return;
        }
        
        throw new RuntimeException("You have to configure either '"
                + VanillaBpProperties.PREFIX
                + ".cockpit.rest' or '"
                + VanillaBpProperties.PREFIX
                + ".cockpit.kafka' to define were to send user task events to!");
        
    }

    @Override
    public void publish(UserTaskEvent eventObject) {

        if (eventObject instanceof UserTaskUpdatedEvent userTaskUpdatedEvent){
            editUserTaskCreatedOrUpdatedEvent(userTaskUpdatedEvent);
            final var event = this.userTaskMapper.map(userTaskUpdatedEvent);
            bpmsApi.get().userTaskUpdatedEvent(eventObject.getUserTaskId(), event);

        } else if (eventObject instanceof UserTaskCreatedEvent userTaskCreatedEvent){
            editUserTaskCreatedOrUpdatedEvent(userTaskCreatedEvent);
            final var event = this.userTaskMapper.map(userTaskCreatedEvent);
            bpmsApi.get().userTaskCreatedEvent(event);

        } else if (eventObject instanceof UserTaskCompletedEvent userTaskCompletedEvent) {

            final var event = userTaskMapper.map(userTaskCompletedEvent);
            bpmsApi.get().userTaskCompletedEvent(eventObject.getUserTaskId(), event);

        } else if (eventObject instanceof UserTaskCancelledEvent userTaskCancelledEvent) {

            final var event = userTaskMapper.map(userTaskCancelledEvent);
            bpmsApi.get().userTaskCancelledEvent(eventObject.getUserTaskId(), event);

        } else if (eventObject instanceof UserTaskActivatedEvent userTaskActivatedEvent) {

            final var event = userTaskMapper.map(userTaskActivatedEvent);
            bpmsApi.get().userTaskActivatedEvent(eventObject.getUserTaskId(), event);

        } else if (eventObject instanceof UserTaskSuspendedEvent userTaskSuspendedEvent) {

            final var event = userTaskMapper.map(userTaskSuspendedEvent);
            bpmsApi.get().userTaskSuspendedEvent(eventObject.getUserTaskId(), event);

        } else {

            throw new RuntimeException(
                    "Unsupported event type '"
                    + eventObject.getClass().getName()
                    + "'!");
        }
        
    }

}
