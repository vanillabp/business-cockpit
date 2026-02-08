package io.vanillabp.cockpit.adapter.camunda8.workflow.publishing;

import io.vanillabp.cockpit.adapter.common.workflow.WorkflowPublishing;
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
class Camunda8WorkflowEventPublisherTest {

    @Mock
    private WorkflowPublishing workflowPublishing;

    @Mock
    private WorkflowEvent workflowEvent;

    @Mock
    private io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowEvent wrappedEvent;

    private Camunda8WorkflowEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new Camunda8WorkflowEventPublisher(workflowPublishing);
        // Clear any leftover events from previous tests by triggering rollback
        publisher.handleRollback(new ProcessWorkflowAfterTransactionEvent(this));
    }

    @AfterEach
    void tearDown() {
        // Clear events after each test to avoid interference
        publisher.handleRollback(new ProcessWorkflowAfterTransactionEvent(this));
    }

    @Test
    void addEvent_addsEventToThreadLocal() {
        // Add event
        publisher.addEvent(workflowEvent);

        // Event should be stored (verified by handle method)
        // No direct assertion possible on ThreadLocal
    }

    @Test
    void handle_publishesAddedEvents() {
        // Set up the wrapped event
        when(workflowEvent.getEvent()).thenReturn(wrappedEvent);

        // Add event
        publisher.addEvent(workflowEvent);

        // Trigger handle
        final var triggerEvent = new ProcessWorkflowAfterTransactionEvent(this);
        publisher.handle(triggerEvent);

        // Verify event was published
        verify(workflowPublishing).publish(wrappedEvent);
    }

    @Test
    void handle_clearsEventsAfterPublishing() {
        // Set up the wrapped event
        when(workflowEvent.getEvent()).thenReturn(wrappedEvent);

        // Add event and handle
        publisher.addEvent(workflowEvent);
        publisher.handle(new ProcessWorkflowAfterTransactionEvent(this));

        // Handle again - should not publish anything
        publisher.handle(new ProcessWorkflowAfterTransactionEvent(this));

        // Verify only one publish call
        verify(workflowPublishing).publish(wrappedEvent);
    }

    @Test
    void handle_withNoEvents_doesNotThrow() {
        // Trigger handle without adding events
        final var triggerEvent = new ProcessWorkflowAfterTransactionEvent(this);
        publisher.handle(triggerEvent);

        // Should not throw and should not publish anything
        verifyNoInteractions(workflowPublishing);
    }

    @Test
    void handle_publishesMultipleEvents() {
        // Create multiple events
        final var event1 = mock(WorkflowEvent.class);
        final var event2 = mock(WorkflowEvent.class);
        final var wrapped1 = mock(io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowEvent.class);
        final var wrapped2 = mock(io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowEvent.class);

        when(event1.getEvent()).thenReturn(wrapped1);
        when(event2.getEvent()).thenReturn(wrapped2);

        // Add multiple events
        publisher.addEvent(event1);
        publisher.addEvent(event2);

        // Trigger handle
        publisher.handle(new ProcessWorkflowAfterTransactionEvent(this));

        // Verify both events were published
        verify(workflowPublishing).publish(wrapped1);
        verify(workflowPublishing).publish(wrapped2);
    }

    @Test
    void handleRollback_clearsEvents() {
        // Add event
        publisher.addEvent(workflowEvent);

        // Trigger rollback
        publisher.handleRollback(new ProcessWorkflowAfterTransactionEvent(this));

        // Handle after rollback should not publish anything
        publisher.handle(new ProcessWorkflowAfterTransactionEvent(this));

        verifyNoInteractions(workflowPublishing);
    }

}
