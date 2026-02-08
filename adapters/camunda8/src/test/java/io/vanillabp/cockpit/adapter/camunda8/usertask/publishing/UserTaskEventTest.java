package io.vanillabp.cockpit.adapter.camunda8.usertask.publishing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class UserTaskEventTest {

    @Mock
    private io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskEvent mockEvent;

    @Test
    void constructor_withSourceAndEvent_createsInstance() {
        // Create event with source and wrapped event
        final var source = new Object();
        final var event = new UserTaskEvent(source, mockEvent);

        // Event should be created with correct source
        assertThat(event).isNotNull();
        assertThat(event.getSource()).isSameAs(source);
    }

    @Test
    void getEvent_returnsWrappedEvent() {
        // Create event with wrapped event
        final var source = new Object();
        final var event = new UserTaskEvent(source, mockEvent);

        // Should return the wrapped event
        assertThat(event.getEvent()).isSameAs(mockEvent);
    }

    @Test
    void event_extendsApplicationEvent() {
        // Create event
        final var source = new Object();
        final var event = new UserTaskEvent(source, mockEvent);

        // Should be an ApplicationEvent
        assertThat(event).isInstanceOf(org.springframework.context.ApplicationEvent.class);
    }

}
