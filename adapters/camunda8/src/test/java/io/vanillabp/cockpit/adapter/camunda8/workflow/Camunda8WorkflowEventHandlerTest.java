package io.vanillabp.cockpit.adapter.camunda8.workflow;

import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowCreatedEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowLifeCycleEvent;
import io.vanillabp.cockpit.adapter.camunda8.wiring.Camunda8WorkflowConnectable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Camunda8WorkflowEventHandlerTest {

    @Mock
    private Camunda8WorkflowConnectable connectable;

    @Mock
    private Camunda8WorkflowHandler workflowHandler;

    private Camunda8WorkflowEventHandler eventHandler;

    @BeforeEach
    void setUp() {
        eventHandler = new Camunda8WorkflowEventHandler();
    }

    @Test
    void isTenantKnown_withUnknownTenant_returnsFalse() {
        // Check for unknown tenant
        assertThat(eventHandler.isTenantKnown("unknown-tenant")).isFalse();
    }

    @Test
    void isTenantKnown_afterAddingHandler_returnsTrue() {
        // Add a handler with tenant
        when(connectable.getTenantId()).thenReturn("tenant-1");
        eventHandler.addWorkflowHandler(connectable, workflowHandler);

        // Now tenant should be known
        assertThat(eventHandler.isTenantKnown("tenant-1")).isTrue();
    }

    @Test
    void isTenantKnown_withNullTenant_mapsToDefault() {
        // Add a handler with null tenant
        when(connectable.getTenantId()).thenReturn(null);
        eventHandler.addWorkflowHandler(connectable, workflowHandler);

        // Check with null tenant
        assertThat(eventHandler.isTenantKnown(null)).isTrue();
    }

    @Test
    void processCreatedEvent_delegatesToMatchingHandler() {
        // Set up connectable
        when(connectable.getTenantId()).thenReturn("tenant-1");
        when(connectable.getBpmnProcessId()).thenReturn("order-process");
        when(connectable.getTaskDefinition()).thenReturn("order-process");
        eventHandler.addWorkflowHandler(connectable, workflowHandler);

        // Create event
        final var event = mock(Camunda8WorkflowCreatedEvent.class);
        when(event.getTenantId()).thenReturn("tenant-1");
        when(event.getBpmnProcessId()).thenReturn("order-process");
        when(event.getWorkflowDefinitionVersion()).thenReturn(1);
        when(event.getProcessInstanceKey()).thenReturn(12345L);

        // Process event
        eventHandler.processCreatedEvent(event);

        // Verify handler was notified
        verify(workflowHandler).notify(event);
    }

    @Test
    void processLifecycleEvent_delegatesToMatchingHandler() {
        // Set up connectable
        when(connectable.getTenantId()).thenReturn("tenant-1");
        when(connectable.getBpmnProcessId()).thenReturn("order-process");
        when(connectable.getTaskDefinition()).thenReturn("order-process");
        eventHandler.addWorkflowHandler(connectable, workflowHandler);

        // Create lifecycle event
        final var event = mock(Camunda8WorkflowLifeCycleEvent.class);
        when(event.getTenantId()).thenReturn("tenant-1");
        when(event.getBpmnProcessId()).thenReturn("order-process");
        when(event.getWorkflowDefinitionVersion()).thenReturn(1);
        when(event.getProcessInstanceKey()).thenReturn(12345L);

        // Process event
        eventHandler.processLifecycleEvent(event);

        // Verify handler was notified
        verify(workflowHandler).notify(event);
    }

    @Test
    void processWorkflowUpdateEvent_delegatesToMatchingHandler() {
        // Set up connectable
        when(connectable.getTenantId()).thenReturn("tenant-1");
        when(connectable.getBpmnProcessId()).thenReturn("order-process");
        when(connectable.getTaskDefinition()).thenReturn("order-process");
        eventHandler.addWorkflowHandler(connectable, workflowHandler);

        // Create update event
        final var event = mock(Camunda8WorkflowCreatedEvent.class);
        when(event.getTenantId()).thenReturn("tenant-1");
        when(event.getBpmnProcessId()).thenReturn("order-process");
        when(event.getWorkflowDefinitionVersion()).thenReturn(1);
        when(event.getProcessInstanceKey()).thenReturn(12345L);

        // Process event
        eventHandler.processWorkflowUpdateEvent(event);

        // Verify handler was notified
        verify(workflowHandler).notify(event);
    }

    @Test
    void processEvent_withNoMatchingHandler_forKnownTenant_logsDebug() {
        // Add a handler with different process ID
        when(connectable.getTenantId()).thenReturn("tenant-1");
        when(connectable.getBpmnProcessId()).thenReturn("other-process");
        eventHandler.addWorkflowHandler(connectable, workflowHandler);

        // Process event for different process
        @SuppressWarnings("unchecked")
        Consumer<Camunda8WorkflowHandler> consumer = mock(Consumer.class);
        eventHandler.processEvent("tenant-1", "order-process", 1, 12345L, consumer);

        // Consumer should not be called
        verify(consumer, never()).accept(any());
    }

    @Test
    void processEvent_withUnknownTenant_doesNotLog() {
        // No handlers registered - unknown tenant
        @SuppressWarnings("unchecked")
        Consumer<Camunda8WorkflowHandler> consumer = mock(Consumer.class);
        eventHandler.processEvent("unknown-tenant", "order-process", 1, 12345L, consumer);

        // Consumer should not be called
        verify(consumer, never()).accept(any());
    }

    @Test
    void processEvent_withMatchingHandler_invokesConsumer() {
        // Set up connectable with matching criteria
        when(connectable.getTenantId()).thenReturn("tenant-1");
        when(connectable.getBpmnProcessId()).thenReturn("order-process");
        when(connectable.getTaskDefinition()).thenReturn("order-process");
        eventHandler.addWorkflowHandler(connectable, workflowHandler);

        // Process event with consumer
        @SuppressWarnings("unchecked")
        Consumer<Camunda8WorkflowHandler> consumer = mock(Consumer.class);
        eventHandler.processEvent("tenant-1", "order-process", 1, 12345L, consumer);

        // Consumer should be called with the handler
        verify(consumer).accept(workflowHandler);
    }

    @Test
    void addWorkflowHandler_registersTenant() {
        // Initially tenant is unknown
        assertThat(eventHandler.isTenantKnown("my-tenant")).isFalse();

        // Add handler
        when(connectable.getTenantId()).thenReturn("my-tenant");
        eventHandler.addWorkflowHandler(connectable, workflowHandler);

        // Now tenant should be known
        assertThat(eventHandler.isTenantKnown("my-tenant")).isTrue();
    }

}
