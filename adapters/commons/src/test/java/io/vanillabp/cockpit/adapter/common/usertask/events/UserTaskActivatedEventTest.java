package io.vanillabp.cockpit.adapter.common.usertask.events;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTaskActivatedEventTest {

    @Test
    void getEventType_throwsUnsupportedOperationException() {
        UserTaskActivatedEvent event = new UserTaskActivatedEvent();

        assertThatThrownBy(event::getEventType)
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Not supported yet.");
    }

    @Test
    void extendsUserTaskLifecycleEvent() {
        UserTaskActivatedEvent event = new UserTaskActivatedEvent();

        assertThat(event).isInstanceOf(UserTaskLifecycleEvent.class);
    }

    @Test
    void implementsUserTaskEvent() {
        UserTaskActivatedEvent event = new UserTaskActivatedEvent();

        assertThat(event).isInstanceOf(UserTaskEvent.class);
    }

    @Test
    void canSetAndGetProperties() {
        UserTaskActivatedEvent event = new UserTaskActivatedEvent();

        event.setEventId("event-123");
        event.setUserTaskId("task-456");
        event.setWorkflowModuleId("module-789");

        assertThat(event.getEventId()).isEqualTo("event-123");
        assertThat(event.getUserTaskId()).isEqualTo("task-456");
        assertThat(event.getWorkflowModuleId()).isEqualTo("module-789");
    }
}
