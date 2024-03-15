package io.vanillabp.cockpit.adapter.common.workflowmodule.kafka;

import com.google.protobuf.Timestamp;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.RegisterWorkflowModuleEvent;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;

public class WorkflowModuleProtobufMapper {

    public RegisterWorkflowModuleEvent map(
            final io.vanillabp.cockpit.adapter.common.workflowmodule.events.RegisterWorkflowModuleEvent event) {

        if (event == null) {
            return null;
        }
        final var builder = RegisterWorkflowModuleEvent
                .newBuilder()
                .setId(event.getEventId())
                .setTimestamp(mapTimestamp(event.getTimestamp()))
                .setWorkflowModuleId(event.getId())
                .setUri(event.getUri());

        Optional.ofNullable(event.getSource())
                .ifPresent(builder::setSource);
        Optional.ofNullable(event.getTaskProviderApiUriPath())
                .ifPresent(builder::setTaskProviderApiUriPath);
        Optional.ofNullable(event.getWorkflowProviderApiUriPath())
                .ifPresent(builder::setWorkflowProviderApiUriPath);

        return builder.build();

    }

    public Timestamp mapTimestamp(OffsetDateTime value) {
        Instant instant = value.toInstant();
        return Timestamp.newBuilder()
                .setNanos(instant.getNano())
                .setSeconds(instant.getEpochSecond())
                .build();
    }

}
