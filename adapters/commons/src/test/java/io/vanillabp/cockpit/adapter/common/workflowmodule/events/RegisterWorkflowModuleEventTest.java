package io.vanillabp.cockpit.adapter.common.workflowmodule.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterWorkflowModuleEventTest {

    private RegisterWorkflowModuleEvent event;

    @BeforeEach
    void setUp() {
        event = new RegisterWorkflowModuleEvent();
    }

    @Test
    void defaultConstructorCreatesEmptyEvent() {
        assertThat(event.getId()).isNull();
        assertThat(event.getEventId()).isNull();
        assertThat(event.getTimestamp()).isNull();
        assertThat(event.getSource()).isNull();
        assertThat(event.getUri()).isNull();
        assertThat(event.getTaskProviderApiUriPath()).isNull();
        assertThat(event.getWorkflowProviderApiUriPath()).isNull();
        assertThat(event.getAccessibleToGroups()).isNull();
    }

    @Test
    void setAndGetId() {
        event.setId("module-123");
        assertThat(event.getId()).isEqualTo("module-123");
    }

    @Test
    void setAndGetEventId() {
        event.setEventId("event-456");
        assertThat(event.getEventId()).isEqualTo("event-456");
    }

    @Test
    void setAndGetTimestamp() {
        OffsetDateTime timestamp = OffsetDateTime.now();
        event.setTimestamp(timestamp);
        assertThat(event.getTimestamp()).isEqualTo(timestamp);
    }

    @Test
    void setAndGetSource() {
        event.setSource("camunda7");
        assertThat(event.getSource()).isEqualTo("camunda7");
    }

    @Test
    void setAndGetUri() {
        event.setUri("http://localhost:8080");
        assertThat(event.getUri()).isEqualTo("http://localhost:8080");
    }

    @Test
    void setAndGetTaskProviderApiUriPath() {
        event.setTaskProviderApiUriPath("/api/tasks");
        assertThat(event.getTaskProviderApiUriPath()).isEqualTo("/api/tasks");
    }

    @Test
    void setAndGetWorkflowProviderApiUriPath() {
        event.setWorkflowProviderApiUriPath("/api/workflows");
        assertThat(event.getWorkflowProviderApiUriPath()).isEqualTo("/api/workflows");
    }

    @Test
    void setAndGetAccessibleToGroups() {
        List<String> groups = Arrays.asList("group1", "group2");
        event.setAccessibleToGroups(groups);
        assertThat(event.getAccessibleToGroups()).isEqualTo(groups);
    }

    @Test
    void implementsWorkflowModuleEvent() {
        assertThat(event).isInstanceOf(WorkflowModuleEvent.class);
    }

    @Test
    void canSetAllPropertiesTogether() {
        OffsetDateTime timestamp = OffsetDateTime.now();
        List<String> groups = Arrays.asList("admin", "users");

        event.setId("test-module");
        event.setEventId("event-001");
        event.setTimestamp(timestamp);
        event.setSource("test-source");
        event.setUri("http://test.example.com");
        event.setTaskProviderApiUriPath("/tasks");
        event.setWorkflowProviderApiUriPath("/workflows");
        event.setAccessibleToGroups(groups);

        assertThat(event.getId()).isEqualTo("test-module");
        assertThat(event.getEventId()).isEqualTo("event-001");
        assertThat(event.getTimestamp()).isEqualTo(timestamp);
        assertThat(event.getSource()).isEqualTo("test-source");
        assertThat(event.getUri()).isEqualTo("http://test.example.com");
        assertThat(event.getTaskProviderApiUriPath()).isEqualTo("/tasks");
        assertThat(event.getWorkflowProviderApiUriPath()).isEqualTo("/workflows");
        assertThat(event.getAccessibleToGroups()).isEqualTo(groups);
    }
}
