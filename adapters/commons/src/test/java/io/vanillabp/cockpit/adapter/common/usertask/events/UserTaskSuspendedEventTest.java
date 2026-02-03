package io.vanillabp.cockpit.adapter.common.usertask.events;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTaskSuspendedEventTest {

    @Test
    void getEventType_throwsUnsupportedOperationException() {
        UserTaskSuspendedEvent event = new UserTaskSuspendedEvent();

        assertThatThrownBy(event::getEventType)
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Not supported yet.");
    }

    @Test
    void extendsUserTaskLifecycleEvent() {
        UserTaskSuspendedEvent event = new UserTaskSuspendedEvent();

        assertThat(event).isInstanceOf(UserTaskLifecycleEvent.class);
    }

    @Test
    void implementsUserTaskEvent() {
        UserTaskSuspendedEvent event = new UserTaskSuspendedEvent();

        assertThat(event).isInstanceOf(UserTaskEvent.class);
    }

    @Test
    void canSetAndGetProperties() {
        UserTaskSuspendedEvent event = new UserTaskSuspendedEvent();

        event.setEventId("event-123");
        event.setUserTaskId("task-456");
        event.setWorkflowModuleId("module-789");

        assertThat(event.getEventId()).isEqualTo("event-123");
        assertThat(event.getUserTaskId()).isEqualTo("task-456");
        assertThat(event.getWorkflowModuleId()).isEqualTo("module-789");
    }
}
