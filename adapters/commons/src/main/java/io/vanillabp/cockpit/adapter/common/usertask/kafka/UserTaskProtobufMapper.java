package io.vanillabp.cockpit.adapter.common.usertask.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Timestamp;
import io.vanillabp.cockpit.adapter.common.protobuf.DetailsConverter;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskActivatedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCancelledEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCompletedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCreatedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskSuspendedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.DetailsMap;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskCreatedOrUpdatedEvent;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("DuplicatedCode")
public class UserTaskProtobufMapper {

    private final String API_VERSION = "1.0";

    private final ObjectMapper objectMapper;

    public UserTaskProtobufMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public UserTaskCreatedOrUpdatedEvent map(UserTaskCreatedEvent userTaskCreatedEvent){
        UserTaskCreatedOrUpdatedEvent.Builder builder = UserTaskCreatedOrUpdatedEvent.newBuilder();
        this.addUserTaskCreatedInfo(builder, userTaskCreatedEvent);
        builder.setUpdated(false);
        return builder.build();
    }

    public UserTaskCreatedOrUpdatedEvent map(UserTaskUpdatedEvent userTaskUpdatedEvent){
        UserTaskCreatedOrUpdatedEvent.Builder builder = UserTaskCreatedOrUpdatedEvent.newBuilder();
        this.addUserTaskCreatedInfo(builder, userTaskUpdatedEvent);
        builder.setUpdated(true);
        return builder.build();
    }

    private void addUserTaskCreatedInfo(UserTaskCreatedOrUpdatedEvent.Builder builder, UserTaskCreatedEvent userTaskCreatedEvent){
        // required parameters
        builder.setId(userTaskCreatedEvent.getId());
        builder.setApiVersion(API_VERSION);
        builder.setUserTaskId(userTaskCreatedEvent.getUserTaskId());
        builder.setTimestamp(mapTimeStamp(userTaskCreatedEvent.getTimestamp()));
        builder.setBpmnProcessId(userTaskCreatedEvent.getBpmnProcessId());
        builder.setTaskDefinition(userTaskCreatedEvent.getTaskDefinition());
        builder.putAllTitle(userTaskCreatedEvent.getTitle());
        builder.setWorkflowModuleId(userTaskCreatedEvent.getWorkflowModuleId());
        builder.setUiUriPath(userTaskCreatedEvent.getUiUriPath());
        builder.setUiUriType(userTaskCreatedEvent.getUiUriType().getValue());

        // optional parameters
        Optional.ofNullable(userTaskCreatedEvent.getInitiator())
                .ifPresent(builder::setInitiator);
        Optional.ofNullable(userTaskCreatedEvent.getSource())
                .ifPresent(builder::setSource);
        Optional.ofNullable(userTaskCreatedEvent.getWorkflowModuleId())
                .ifPresent(builder::setWorkflowModuleId);
        Optional.ofNullable(userTaskCreatedEvent.getComment())
                .ifPresent(builder::setComment);
        Optional.ofNullable(userTaskCreatedEvent.getBpmnProcessVersion())
                .ifPresent(builder::setBpmnProcessVersion);
        Optional.ofNullable(userTaskCreatedEvent.getWorkflowTitle())
                .ifPresent(builder::putAllWorkflowTitle);
        Optional.ofNullable(userTaskCreatedEvent.getWorkflowId())
                .ifPresent(builder::setWorkflowId);
        Optional.ofNullable(userTaskCreatedEvent.getSubWorkflowId())
                .ifPresent(builder::setSubWorkflowId);
        Optional.ofNullable(userTaskCreatedEvent.getBusinessId())
                .ifPresent(builder::setBusinessId);
        Optional.ofNullable(userTaskCreatedEvent.getBpmnTaskId())
                .ifPresent(builder::setBpmnTaskId);
        Optional.ofNullable(userTaskCreatedEvent.getTaskDefinitionTitle())
                .ifPresent(builder::putAllTaskDefinitionTitle);
        Optional.ofNullable(userTaskCreatedEvent.getAssignee())
                .ifPresent(builder::setAssignee);
        Optional.ofNullable(userTaskCreatedEvent.getCandidateGroups())
                .ifPresent(builder::addAllCandidateGroups);
        Optional.ofNullable(userTaskCreatedEvent.getCandidateUsers())
                .ifPresent(builder::addAllCandidateUsers);
        Optional.ofNullable(userTaskCreatedEvent.getExcludedCandidateUsers())
                .ifPresent(builder::addAllExcludedCandidateUsers);
        Optional.ofNullable(userTaskCreatedEvent.getDueDate())
                .map(this::mapTimeStamp)
                .ifPresent(builder::setDueDate);
        Optional.ofNullable(userTaskCreatedEvent.getFollowUpDate())
                .map(this::mapTimeStamp)
                .ifPresent(builder::setFollowUpDate);
        Optional.ofNullable(userTaskCreatedEvent.getDetails())
                .map(this::mapDetailsToProtobuf)
                .ifPresent(builder::setDetails);
        Optional.ofNullable(userTaskCreatedEvent.getDetailsFulltextSearch())
                .ifPresent(builder::setDetailsFulltextSearch);
    }


    public io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskCompletedEvent map(
            UserTaskCompletedEvent userTaskCompletedEvent){

        io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskCompletedEvent.Builder builder =
                io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskCompletedEvent.newBuilder();

        builder.setId(userTaskCompletedEvent.getEventId());
        builder.setApiVersion(API_VERSION);
        builder.setUserTaskId(userTaskCompletedEvent.getUserTaskId());
        builder.setTimestamp(mapTimeStamp(userTaskCompletedEvent.getTimestamp()));

        // optional parameters
        Optional.ofNullable(userTaskCompletedEvent.getInitiator())
                .ifPresent(builder::setInitiator);
        Optional.ofNullable(userTaskCompletedEvent.getSource())
                .ifPresent(builder::setSource);
        Optional.ofNullable(userTaskCompletedEvent.getComment())
                .ifPresent(builder::setComment);

        return builder.build();
    }

    public io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskActivatedEvent map(UserTaskActivatedEvent userTaskEvent){
        io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskActivatedEvent.Builder builder =
                io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskActivatedEvent.newBuilder();

        builder.setId(userTaskEvent.getEventId());
        builder.setApiVersion(API_VERSION);
        builder.setUserTaskId(userTaskEvent.getUserTaskId());
        builder.setTimestamp(mapTimeStamp(userTaskEvent.getTimestamp()));

        // optional parameters
        Optional.ofNullable(userTaskEvent.getInitiator())
                .ifPresent(builder::setInitiator);
        Optional.ofNullable(userTaskEvent.getSource())
                .ifPresent(builder::setSource);
        Optional.ofNullable(userTaskEvent.getComment())
                .ifPresent(builder::setComment);

        return builder.build();
    }


    public io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskSuspendedEvent map(UserTaskSuspendedEvent userTaskEvent){
        io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskSuspendedEvent.Builder builder =
                io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskSuspendedEvent.newBuilder();

        builder.setId(userTaskEvent.getEventId());
        builder.setApiVersion(API_VERSION);
        builder.setUserTaskId(userTaskEvent.getUserTaskId());
        builder.setTimestamp(mapTimeStamp(userTaskEvent.getTimestamp()));

        // optional parameters
        Optional.ofNullable(userTaskEvent.getInitiator())
                .ifPresent(builder::setInitiator);
        Optional.ofNullable(userTaskEvent.getSource())
                .ifPresent(builder::setSource);
        Optional.ofNullable(userTaskEvent.getComment())
                .ifPresent(builder::setComment);

        return builder.build();
    }


    public io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskCancelledEvent map(UserTaskCancelledEvent userTaskEvent){
        io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskCancelledEvent.Builder builder =
                io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskCancelledEvent.newBuilder();

        builder.setId(userTaskEvent.getEventId());
        builder.setApiVersion(API_VERSION);
        builder.setUserTaskId(userTaskEvent.getUserTaskId());
        builder.setTimestamp(mapTimeStamp(userTaskEvent.getTimestamp()));

        // optional parameters
        Optional.ofNullable(userTaskEvent.getInitiator())
                .ifPresent(builder::setInitiator);
        Optional.ofNullable(userTaskEvent.getSource())
                .ifPresent(builder::setSource);
        Optional.ofNullable(userTaskEvent.getComment())
                .ifPresent(builder::setComment);

        return builder.build();
    }


    public Timestamp mapTimeStamp(OffsetDateTime value) {
        Instant instant = value.toInstant();
        return Timestamp.newBuilder()
                .setNanos(instant.getNano())
                .setSeconds(instant.getEpochSecond())
                .build();
    }

    public DetailsMap mapDetailsToProtobuf(
            final Map<String, Object> details) {

        final var tree = objectMapper.valueToTree(details);
        return DetailsConverter.mapDetailsJsonToProtobuf(tree);

    }

}
