package io.vanillabp.cockpit.adapter.common.workflow.kafka;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Timestamp;
import io.vanillabp.cockpit.adapter.common.protobuf.DetailsConverter;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCancelledEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCompletedEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCreatedEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.DetailsMap;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCreatedOrUpdatedEvent;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("DuplicatedCode")
public class WorkflowProtobufMapper {

    private final String API_VERSION = "1.0";

    private final ObjectMapper objectMapper;

    public WorkflowProtobufMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

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
        Optional.ofNullable(workflowUpdatedEvent.getBusinessId())
                        .ifPresent(builder::setBusinessId);
        Optional.ofNullable(workflowUpdatedEvent.getInitiator())
                .ifPresent(builder::setInitiator);
        Optional.ofNullable(workflowUpdatedEvent.getSource())
                .ifPresent(builder::setSource);
        Optional.ofNullable(workflowUpdatedEvent.getTitle())
                .ifPresent(builder::putAllTitle);
        Optional.ofNullable(workflowUpdatedEvent.getComment())
                .ifPresent(builder::setComment);
        Optional.ofNullable(workflowUpdatedEvent.getBpmnProcessVersion())
                .ifPresent(builder::setBpmnProcessVersion);
        Optional.ofNullable(workflowUpdatedEvent.getWorkflowModuleUri())
                .ifPresent(builder::setWorkflowModuleUri);
        Optional.ofNullable(workflowUpdatedEvent.getDetails())
                .map(this::mapDetailsToProtobuf)
                .ifPresent(builder::setDetails);
        Optional.ofNullable(workflowUpdatedEvent.getDetailsFulltextSearch())
                .ifPresent(builder::setDetailsFulltextSearch);
        Optional.ofNullable(workflowUpdatedEvent.getComment())
                .ifPresent(builder::setComment);
        Optional.ofNullable(workflowUpdatedEvent.getAccessibleToUsers())
                .stream()
                .flatMap(Collection::stream)
                .forEach(builder::addAccessibleToUsers);
        Optional.ofNullable(workflowUpdatedEvent.getAccessibleToGroups())
                .stream()
                .flatMap(Collection::stream)
                .forEach(builder::addAccessibleToGroups);
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

    public DetailsMap mapDetailsToProtobuf(
            final Map<String, Object> details) {

        final var tree = objectMapper.valueToTree(details);
        return DetailsConverter.mapDetailsJsonToProtobuf(tree);

    }

}
