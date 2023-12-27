package io.vanillabp.cockpit.adapter.common.usertask.kafka;

import com.google.protobuf.Timestamp;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskActivatedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCancelledEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCompletedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCreatedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskSuspendedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskCreatedOrUpdatedEvent;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressWarnings("DuplicatedCode")
public class UserTaskProtobufMapper {

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
        builder.setUserTaskId(userTaskCreatedEvent.getUserTaskId());
        builder.setTimestamp(mapTimeStamp(userTaskCreatedEvent.getTimestamp()));
        builder.setBpmnProcessId(userTaskCreatedEvent.getBpmnProcessId());
        builder.setTaskDefinition(userTaskCreatedEvent.getTaskDefinition());
        builder.putAllTitle(userTaskCreatedEvent.getTitle());
        builder.setWorkflowModuleId(userTaskCreatedEvent.getWorkflowModule());
        builder.setTaskProviderApiUriPath(userTaskCreatedEvent.getTaskProviderApiUriPath());
        builder.setUiUriPath(userTaskCreatedEvent.getUiUriPath());
        builder.setUiUriType(userTaskCreatedEvent.getUiUriType().getValue());

        // optional parameters
        Optional.ofNullable(userTaskCreatedEvent.getInitiator())
                .ifPresent(builder::setInitiator);
        Optional.ofNullable(userTaskCreatedEvent.getSource())
                .ifPresent(builder::setSource);
        Optional.ofNullable(userTaskCreatedEvent.getWorkflowModule())
                .ifPresent(builder::setWorkflowModule);
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
        Optional.ofNullable(userTaskCreatedEvent.getWorkflowModuleUri())
                .ifPresent(builder::setWorkflowModuleUri);
        Optional.ofNullable(userTaskCreatedEvent.getAssignee())
                .ifPresent(builder::setAssignee);
        Optional.ofNullable(userTaskCreatedEvent.getCandidateGroups())
                .ifPresent(builder::addAllCandidateGroups);
        Optional.ofNullable(userTaskCreatedEvent.getCandidateUsers())
                .ifPresent(builder::addAllCandidateUsers);
        Optional.ofNullable(userTaskCreatedEvent.getDueDate())
                .map(this::mapTimeStamp)
                .ifPresent(builder::setDueDate);
        Optional.ofNullable(userTaskCreatedEvent.getFollowUpDate())
                .map(this::mapTimeStamp)
                .ifPresent(builder::setFollowUpDate);
        builder.putAllDetails(
                mapDetails(userTaskCreatedEvent.getDetails()));
        Optional.ofNullable(userTaskCreatedEvent.getDetailsFulltextSearch())
                .ifPresent(builder::setDetailsFulltextSearch);
    }


    public io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskCompletedEvent map(
            UserTaskCompletedEvent userTaskCompletedEvent){

        io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskCompletedEvent.Builder builder =
                io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskCompletedEvent.newBuilder();

        builder.setId(userTaskCompletedEvent.getId());
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

        builder.setId(userTaskEvent.getId());
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

        builder.setId(userTaskEvent.getId());
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

        builder.setId(userTaskEvent.getId());
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

    public Map<String, String> mapDetails(Map<String, Object> stringMap){
        return stringMap
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        stringObjectEntry -> stringObjectEntry.getValue().toString()
                ));
    }
}
