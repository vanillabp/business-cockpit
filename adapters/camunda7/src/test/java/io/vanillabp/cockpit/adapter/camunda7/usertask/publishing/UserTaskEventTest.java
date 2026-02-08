package io.vanillabp.cockpit.adapter.camunda7.usertask.publishing;

import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCreatedEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link UserTaskEvent}.
 */
class UserTaskEventTest {

    @Test
    void constructor_setsSourceAndEvent() {
        // Arrange
        Object source = new Object();
        io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskEvent mockEvent =
                mock(UserTaskCreatedEvent.class);

        // Act
        UserTaskEvent userTaskEvent = new UserTaskEvent(source, mockEvent);

        // Assert
        assertThat(userTaskEvent.getSource()).isEqualTo(source);
        assertThat(userTaskEvent.getEvent()).isEqualTo(mockEvent);
    }

    @Test
    void getEvent_returnsWrappedEvent() {
        // Arrange
        Object source = "test-source";
        io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskEvent mockEvent =
                mock(UserTaskCreatedEvent.class);
        UserTaskEvent userTaskEvent = new UserTaskEvent(source, mockEvent);

        // Act
        io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskEvent result = userTaskEvent.getEvent();

        // Assert
        assertThat(result).isSameAs(mockEvent);
    }

    @Test
    void event_isApplicationEvent() {
        // Arrange
        Object source = new Object();
        io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskEvent mockEvent =
                mock(UserTaskCreatedEvent.class);

        // Act
        UserTaskEvent userTaskEvent = new UserTaskEvent(source, mockEvent);

        // Assert
        assertThat(userTaskEvent).isInstanceOf(org.springframework.context.ApplicationEvent.class);
    }
}
