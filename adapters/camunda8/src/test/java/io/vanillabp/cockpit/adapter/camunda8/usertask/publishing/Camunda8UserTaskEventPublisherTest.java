package io.vanillabp.cockpit.adapter.camunda8.usertask.publishing;

import io.vanillabp.cockpit.adapter.common.usertask.UserTaskPublishing;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Camunda8UserTaskEventPublisherTest {

    @Mock
    private UserTaskPublishing userTaskPublishing;

    @Mock
    private io.vanillabp.cockpit.adapter.camunda8.usertask.publishing.UserTaskEvent userTaskEvent;

    @Mock
    private UserTaskEvent wrappedEvent;

    private Camunda8UserTaskEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new Camunda8UserTaskEventPublisher(userTaskPublishing);
        // Clear any leftover events from previous tests by triggering rollback
        publisher.handleRollback(new ProcessUserTaskAfterTransactionEvent(this));
    }

    @AfterEach
    void tearDown() {
        // Clear events after each test to avoid interference
        publisher.handleRollback(new ProcessUserTaskAfterTransactionEvent(this));
    }

    @Test
    void addEvent_addsEventToThreadLocal() {
        // Add event
        publisher.addEvent(userTaskEvent);

        // Event should be stored (verified by handle method)
        // No direct assertion possible on ThreadLocal
    }

    @Test
    void handle_publishesAddedEvents() {
        // Set up the wrapped event
        when(userTaskEvent.getEvent()).thenReturn(wrappedEvent);

        // Add event
        publisher.addEvent(userTaskEvent);

        // Trigger handle
        final var triggerEvent = new ProcessUserTaskAfterTransactionEvent(this);
        publisher.handle(triggerEvent);

        // Verify event was published
        verify(userTaskPublishing).publish(wrappedEvent);
    }

    @Test
    void handle_clearsEventsAfterPublishing() {
        // Set up the wrapped event
        when(userTaskEvent.getEvent()).thenReturn(wrappedEvent);

        // Add event and handle
        publisher.addEvent(userTaskEvent);
        publisher.handle(new ProcessUserTaskAfterTransactionEvent(this));

        // Handle again - should not publish anything
        publisher.handle(new ProcessUserTaskAfterTransactionEvent(this));

        // Verify only one publish call
        verify(userTaskPublishing).publish(wrappedEvent);
    }

    @Test
    void handle_withNoEvents_doesNotThrow() {
        // Trigger handle without adding events
        final var triggerEvent = new ProcessUserTaskAfterTransactionEvent(this);
        publisher.handle(triggerEvent);

        // Should not throw and should not publish anything
        verifyNoInteractions(userTaskPublishing);
    }

    @Test
    void handle_publishesMultipleEvents() {
        // Create multiple events
        final var event1 = mock(io.vanillabp.cockpit.adapter.camunda8.usertask.publishing.UserTaskEvent.class);
        final var event2 = mock(io.vanillabp.cockpit.adapter.camunda8.usertask.publishing.UserTaskEvent.class);
        final var wrapped1 = mock(UserTaskEvent.class);
        final var wrapped2 = mock(UserTaskEvent.class);

        when(event1.getEvent()).thenReturn(wrapped1);
        when(event2.getEvent()).thenReturn(wrapped2);

        // Add multiple events
        publisher.addEvent(event1);
        publisher.addEvent(event2);

        // Trigger handle
        publisher.handle(new ProcessUserTaskAfterTransactionEvent(this));

        // Verify both events were published
        verify(userTaskPublishing).publish(wrapped1);
        verify(userTaskPublishing).publish(wrapped2);
    }

    @Test
    void handleRollback_clearsEvents() {
        // Add event
        publisher.addEvent(userTaskEvent);

        // Trigger rollback
        publisher.handleRollback(new ProcessUserTaskAfterTransactionEvent(this));

        // Handle after rollback should not publish anything
        publisher.handle(new ProcessUserTaskAfterTransactionEvent(this));

        verifyNoInteractions(userTaskPublishing);
    }

}
