package io.vanillabp.cockpit.adapter.common.usertask.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserTaskLifecycleEventTest {

    private UserTaskLifecycleEvent event;

    @BeforeEach
    void setUp() {
        event = new UserTaskActivatedEvent();
    }

    @Test
    void setAndGetEventId() {
        event.setEventId("event-123");
        assertThat(event.getEventId()).isEqualTo("event-123");
    }

    @Test
    void setAndGetWorkflowModuleId() {
        event.setWorkflowModuleId("module-456");
        assertThat(event.getWorkflowModuleId()).isEqualTo("module-456");
    }

    @Test
    void setAndGetUserTaskId() {
        event.setUserTaskId("task-789");
        assertThat(event.getUserTaskId()).isEqualTo("task-789");
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
    void defaultConstructorCreatesEmptyObject() {
        UserTaskLifecycleEvent newEvent = new UserTaskActivatedEvent();
        assertThat(newEvent.getEventId()).isNull();
        assertThat(newEvent.getWorkflowModuleId()).isNull();
        assertThat(newEvent.getUserTaskId()).isNull();
        assertThat(newEvent.getInitiator()).isNull();
        assertThat(newEvent.getTimestamp()).isNull();
        assertThat(newEvent.getSource()).isNull();
        assertThat(newEvent.getComment()).isNull();
    }
}
