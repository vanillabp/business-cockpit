package io.vanillabp.cockpit.adapter.common.workflow.kafka;


import com.google.protobuf.Timestamp;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCancelledEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCompletedEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCreatedEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCreatedOrUpdatedEvent;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressWarnings("DuplicatedCode")
public class WorkflowProtobufMapper {

    private final String API_VERSION = "1.0";

    public io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCreatedOrUpdatedEvent map(
            WorkflowCreatedEvent workflowCreatedEvent) {

        WorkflowCreatedOrUpdatedEvent.Builder builder = WorkflowCreatedOrUpdatedEvent.newBuilder();
        fillWorkflowCreatedOrUpdatedEvent(workflowCreatedEvent, builder);
        builder.setUpdated(false);
        return builder.build();
    }


    public io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCreatedOrUpdatedEvent map(
            WorkflowUpdatedEvent workflowUpdatedEvent) {

        WorkflowCreatedOrUpdatedEvent.Builder builder = WorkflowCreatedOrUpdatedEvent.newBuilder();
        fillWorkflowCreatedOrUpdatedEvent(workflowUpdatedEvent, builder);
        builder.setUpdated(true);
        return builder.build();
    }


    private void fillWorkflowCreatedOrUpdatedEvent(
            WorkflowCreatedEvent workflowUpdatedEvent,
            WorkflowCreatedOrUpdatedEvent.Builder builder) {

        // required parameters
        builder.setId(workflowUpdatedEvent.getId());
        builder.setApiVersion(API_VERSION);
        builder.setWorkflowId(workflowUpdatedEvent.getWorkflowId());
        builder.setTimestamp(mapTimestamp(workflowUpdatedEvent.getTimestamp()));
        builder.setBpmnProcessId(workflowUpdatedEvent.getBpmnProcessId());
        builder.setWorkflowModule(workflowUpdatedEvent.getWorkflowModule());
        builder.setWorkflowProviderApiUriPath(workflowUpdatedEvent.getWorkflowProviderApiUriPath());
        builder.setUiUriPath(workflowUpdatedEvent.getUiUriPath());
        builder.setUiUriType(workflowUpdatedEvent.getUiUriType().getValue());
        builder.setWorkflowProviderApiUriPath(workflowUpdatedEvent.getWorkflowProviderApiUriPath());

        // optional parameters
        builder.setBusinessId(workflowUpdatedEvent.getBusinessId());
        builder.setInitiator(workflowUpdatedEvent.getInitiator());
        builder.setSource(workflowUpdatedEvent.getSource());
        builder.putAllTitle(workflowUpdatedEvent.getTitle());
        builder.setComment(workflowUpdatedEvent.getComment());
        builder.setBpmnProcessVersion(workflowUpdatedEvent.getBpmnProcessVersion());
        builder.setWorkflowModuleUri(workflowUpdatedEvent.getWorkflowModuleUri());
        builder.putAllDetails(mapDetails(workflowUpdatedEvent.getDetails()));
        builder.setDetailsFulltextSearch(workflowUpdatedEvent.getDetailsFulltextSearch());
    }

    public io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCompletedEvent map(WorkflowCompletedEvent workflowEvent) {
        io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCompletedEvent.Builder builder =
                io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCompletedEvent.newBuilder();

        // required properties
        builder.setId(workflowEvent.getId());
        builder.setApiVersion(API_VERSION);
        builder.setWorkflowId(workflowEvent.getWorkflowId());
        builder.setTimestamp(mapTimestamp(workflowEvent.getTimestamp()));

        // optional properties
        Optional.ofNullable(workflowEvent.getInitiator())
                .ifPresent(builder::setInitiator);
        Optional.ofNullable(workflowEvent.getSource())
                .ifPresent(builder::setSource);
        Optional.ofNullable(workflowEvent.getComment())
                .ifPresent(builder::setComment);
        Optional.ofNullable(workflowEvent.getBpmnProcessId())
                .ifPresent(builder::setBpmnProcessId);
        Optional.ofNullable(workflowEvent.getBpmnProcessVersion())
                .ifPresent(builder::setBpmnProcessVersion);

        return builder.build();
    }


    public io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCancelledEvent map(WorkflowCancelledEvent workflowEvent) {
        io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCancelledEvent.Builder builder =
                io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCancelledEvent.newBuilder();

        // required properties
        builder.setId(workflowEvent.getId());
        builder.setApiVersion(API_VERSION);
        builder.setWorkflowId(workflowEvent.getWorkflowId());
        builder.setTimestamp(mapTimestamp(workflowEvent.getTimestamp()));

        // optional properties
        Optional.ofNullable(workflowEvent.getInitiator())
                .ifPresent(builder::setInitiator);
        Optional.ofNullable(workflowEvent.getSource())
                .ifPresent(builder::setSource);
        Optional.ofNullable(workflowEvent.getComment())
                .ifPresent(builder::setComment);
        Optional.ofNullable(workflowEvent.getBpmnProcessId())
                .ifPresent(builder::setBpmnProcessId);
        Optional.ofNullable(workflowEvent.getBpmnProcessVersion())
                .ifPresent(builder::setBpmnProcessVersion);

        return builder.build();
    }

    public Timestamp mapTimestamp(OffsetDateTime value) {
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
