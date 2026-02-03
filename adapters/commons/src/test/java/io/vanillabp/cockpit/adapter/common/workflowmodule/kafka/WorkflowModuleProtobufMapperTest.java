package io.vanillabp.cockpit.adapter.common.workflowmodule.kafka;

import io.vanillabp.cockpit.adapter.common.workflowmodule.events.RegisterWorkflowModuleEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowModuleProtobufMapperTest {

    private WorkflowModuleProtobufMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new WorkflowModuleProtobufMapper();
    }

    @Test
    void map_withNull_returnsNull() {
        assertThat(mapper.map(null)).isNull();
    }

    @Test
    void map_mapsRequiredFields() {
        RegisterWorkflowModuleEvent event = createRegisterWorkflowModuleEvent();

        io.vanillabp.cockpit.bpms.api.protobuf.v1.RegisterWorkflowModuleEvent result = mapper.map(event);

        assertThat(result.getId()).isEqualTo("event-123");
        assertThat(result.getWorkflowModuleId()).isEqualTo("test-module");
        assertThat(result.getUri()).isEqualTo("http://localhost:8080");
    }

    @Test
    void map_mapsOptionalFields() {
        RegisterWorkflowModuleEvent event = createRegisterWorkflowModuleEvent();
        event.setSource("camunda7");
        event.setTaskProviderApiUriPath("/api/tasks");
        event.setWorkflowProviderApiUriPath("/api/workflows");
        event.setAccessibleToGroups(Arrays.asList("admin", "users"));

        io.vanillabp.cockpit.bpms.api.protobuf.v1.RegisterWorkflowModuleEvent result = mapper.map(event);

        assertThat(result.getSource()).isEqualTo("camunda7");
        assertThat(result.getTaskProviderApiUriPath()).isEqualTo("/api/tasks");
        assertThat(result.getWorkflowProviderApiUriPath()).isEqualTo("/api/workflows");
        assertThat(result.getAccessibleToGroupsList()).containsExactly("admin", "users");
    }

    @Test
    void map_withNullOptionalFields_doesNotFail() {
        RegisterWorkflowModuleEvent event = createRegisterWorkflowModuleEvent();
        event.setSource(null);
        event.setTaskProviderApiUriPath(null);
        event.setWorkflowProviderApiUriPath(null);
        event.setAccessibleToGroups(null);

        io.vanillabp.cockpit.bpms.api.protobuf.v1.RegisterWorkflowModuleEvent result = mapper.map(event);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("event-123");
    }

    @Test
    void mapTimestamp_mapsCorrectly() {
        OffsetDateTime timestamp = OffsetDateTime.of(2024, 1, 15, 10, 30, 45, 123456789, ZoneOffset.UTC);

        com.google.protobuf.Timestamp result = mapper.mapTimestamp(timestamp);

        assertThat(result.getSeconds()).isEqualTo(timestamp.toInstant().getEpochSecond());
        assertThat(result.getNanos()).isEqualTo(123456789);
    }

    private RegisterWorkflowModuleEvent createRegisterWorkflowModuleEvent() {
        RegisterWorkflowModuleEvent event = new RegisterWorkflowModuleEvent();
        OffsetDateTime timestamp = OffsetDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneOffset.UTC);

        event.setEventId("event-123");
        event.setId("test-module");
        event.setTimestamp(timestamp);
        event.setUri("http://localhost:8080");

        return event;
    }
}
