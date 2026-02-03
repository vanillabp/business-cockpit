package io.vanillabp.cockpit.adapter.common.workflow.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowLifecycleEventTest {

    private WorkflowLifecycleEvent event;

    @BeforeEach
    void setUp() {
        event = new WorkflowLifecycleEvent();
    }

    @Test
    void defaultConstructorCreatesEmptyEvent() {
        assertThat(event.getEventId()).isNull();
        assertThat(event.getWorkflowId()).isNull();
        assertThat(event.getWorkflowModuleId()).isNull();
        assertThat(event.getInitiator()).isNull();
        assertThat(event.getTimestamp()).isNull();
        assertThat(event.getSource()).isNull();
        assertThat(event.getComment()).isNull();
        assertThat(event.getBpmnProcessId()).isNull();
        assertThat(event.getBpmnProcessVersion()).isNull();
    }

    @Test
    void setAndGetEventId() {
        event.setEventId("event-123");
        assertThat(event.getEventId()).isEqualTo("event-123");
    }

    @Test
    void setAndGetWorkflowId() {
        event.setWorkflowId("workflow-456");
        assertThat(event.getWorkflowId()).isEqualTo("workflow-456");
    }

    @Test
    void setAndGetWorkflowModuleId() {
        event.setWorkflowModuleId("module-789");
        assertThat(event.getWorkflowModuleId()).isEqualTo("module-789");
    }

    @Test
    void setAndGetInitiator() {
        event.setInitiator("user@example.com");
        assertThat(event.getInitiator()).isEqualTo("user@example.com");
    }

    @Test
    void setAndGetTimestamp() {
        OffsetDateTime timestamp = OffsetDateTime.now();
        event.setTimestamp(timestamp);
        assertThat(event.getTimestamp()).isEqualTo(timestamp);
    }

    @Test
    void setAndGetSource() {
        event.setSource("camunda8");
        assertThat(event.getSource()).isEqualTo("camunda8");
    }

    @Test
    void setAndGetComment() {
        event.setComment("Test comment");
        assertThat(event.getComment()).isEqualTo("Test comment");
    }

    @Test
    void setAndGetBpmnProcessId() {
        event.setBpmnProcessId("process-001");
        assertThat(event.getBpmnProcessId()).isEqualTo("process-001");
    }

    @Test
    void setAndGetBpmnProcessVersion() {
        event.setBpmnProcessVersion("1.0.0");
        assertThat(event.getBpmnProcessVersion()).isEqualTo("1.0.0");
    }

    @Test
    void implementsWorkflowEvent() {
        assertThat(event).isInstanceOf(WorkflowEvent.class);
    }
}
