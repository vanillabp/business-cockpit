package io.vanillabp.cockpit.adapter.common.workflow.kafka;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Timestamp;
import io.vanillabp.cockpit.adapter.common.protobuf.DetailsConverter;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCancelledEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCompletedEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCreatedEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowEventImpl;
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

    public io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCreatedOrUpdatedEvent map(
            WorkflowCompletedEvent workflowEvent) {

        WorkflowCreatedOrUpdatedEvent.Builder builder = WorkflowCreatedOrUpdatedEvent.newBuilder();
        fillWorkflowCreatedOrUpdatedEvent(workflowEvent, builder);
        builder.setUpdated(true);
        return builder.build();

    }

    public io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCreatedOrUpdatedEvent map(
            WorkflowCancelledEvent workflowEvent) {

        WorkflowCreatedOrUpdatedEvent.Builder builder = WorkflowCreatedOrUpdatedEvent.newBuilder();
        fillWorkflowCreatedOrUpdatedEvent(workflowEvent, builder);
        builder.setUpdated(true);
        return builder.build();

    }

    private void fillWorkflowCreatedOrUpdatedEvent(
            WorkflowEventImpl workflowUpdatedEvent,
            WorkflowCreatedOrUpdatedEvent.Builder builder) {

        // required parameters
        builder.setId(workflowUpdatedEvent.getEventId());
        builder.setApiVersion(API_VERSION);
        builder.setWorkflowId(workflowUpdatedEvent.getWorkflowId());
        builder.setTimestamp(mapTimestamp(workflowUpdatedEvent.getTimestamp()));
        builder.setBpmnProcessId(workflowUpdatedEvent.getBpmnProcessId());
        builder.setWorkflowModuleId(workflowUpdatedEvent.getWorkflowModuleId());
        builder.setUiUriPath(workflowUpdatedEvent.getUiUriPath());
        builder.setUiUriType(workflowUpdatedEvent.getUiUriType().getValue());

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
