package io.vanillabp.cockpit.adapter.camunda8.usertask;

import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8UserTaskCreatedEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8UserTaskEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8UserTaskLifecycleEvent;
import io.vanillabp.cockpit.adapter.camunda8.wiring.Camunda8UserTaskConnectable;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskEventImpl;
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
class Camunda8UserTaskEventHandlerTest {

    @Mock
    private Camunda8UserTaskConnectable connectable;

    @Mock
    private Camunda8UserTaskHandler taskHandler;

    private Camunda8UserTaskEventHandler eventHandler;

    @BeforeEach
    void setUp() {
        eventHandler = new Camunda8UserTaskEventHandler();
    }

    @Test
    void addTaskHandler_registersTenantId() {
        // Add a handler with tenant
        when(connectable.getTenantId()).thenReturn("tenant-1");
        eventHandler.addTaskHandler(connectable, taskHandler);

        // Handler should be registered (verified through processEvent behavior)
    }

    @Test
    void processCreatedEvent_delegatesToMatchingHandler() {
        // Set up connectable
        when(connectable.getTenantId()).thenReturn("tenant-1");
        when(connectable.getBpmnProcessId()).thenReturn("order-process");
        when(connectable.getTaskDefinition()).thenReturn("review-task");
        eventHandler.addTaskHandler(connectable, taskHandler);

        // Create event
        final var event = mock(Camunda8UserTaskCreatedEvent.class);
        when(event.getTenantId()).thenReturn("tenant-1");
        when(event.getElementId()).thenReturn("Activity_Review");
        when(event.getBpmnProcessId()).thenReturn("order-process");
        when(event.getWorkflowDefinitionVersion()).thenReturn(1);
        when(event.getProcessInstanceKey()).thenReturn(12345L);
        when(event.getFormKey()).thenReturn("review-task");

        // Process event
        eventHandler.processCreatedEvent(event);

        // Verify handler was notified
        verify(taskHandler).notify(event);
    }

    @Test
    void processLifecycleEvent_delegatesToMatchingHandler() {
        // Set up connectable
        when(connectable.getTenantId()).thenReturn("tenant-1");
        when(connectable.getBpmnProcessId()).thenReturn("order-process");
        when(connectable.getTaskDefinition()).thenReturn("review-task");
        eventHandler.addTaskHandler(connectable, taskHandler);

        // Create lifecycle event
        final var event = mock(Camunda8UserTaskLifecycleEvent.class);
        when(event.getTenantId()).thenReturn("tenant-1");
        when(event.getElementId()).thenReturn("Activity_Review");
        when(event.getBpmnProcessId()).thenReturn("order-process");
        when(event.getWorkflowDefinitionVersion()).thenReturn(1);
        when(event.getProcessInstanceKey()).thenReturn(12345L);
        when(event.getFormKey()).thenReturn("review-task");

        // Process event
        eventHandler.processLifecycleEvent(event);

        // Verify handler was notified
        verify(taskHandler).notify(event);
    }

    @Test
    void processEvent_withNoMatchingHandler_forUnknownTenant_doesNotCall() {
        // No handlers registered
        @SuppressWarnings("unchecked")
        Consumer<Camunda8UserTaskHandler> consumer = mock(Consumer.class);
        eventHandler.processEvent("unknown-tenant", "Activity_1", "order-process", 1, 12345L, "task-def", consumer);

        // Consumer should not be called
        verify(consumer, never()).accept(any());
    }

    @Test
    void processEvent_withNoMatchingTaskDefinition_forKnownTenant_logsDebug() {
        // Add a handler with different task definition
        when(connectable.getTenantId()).thenReturn("tenant-1");
        when(connectable.getBpmnProcessId()).thenReturn("order-process");
        when(connectable.getTaskDefinition()).thenReturn("other-task");
        eventHandler.addTaskHandler(connectable, taskHandler);

        // Process event for different task definition
        @SuppressWarnings("unchecked")
        Consumer<Camunda8UserTaskHandler> consumer = mock(Consumer.class);
        eventHandler.processEvent("tenant-1", "Activity_1", "order-process", 1, 12345L, "review-task", consumer);

        // Consumer should not be called
        verify(consumer, never()).accept(any());
    }

    @Test
    void processEvent_withMatchingHandler_invokesConsumer() {
        // Set up connectable with matching criteria
        when(connectable.getTenantId()).thenReturn("tenant-1");
        when(connectable.getBpmnProcessId()).thenReturn("order-process");
        when(connectable.getTaskDefinition()).thenReturn("review-task");
        eventHandler.addTaskHandler(connectable, taskHandler);

        // Process event with consumer
        @SuppressWarnings("unchecked")
        Consumer<Camunda8UserTaskHandler> consumer = mock(Consumer.class);
        eventHandler.processEvent("tenant-1", "Activity_1", "order-process", 1, 12345L, "review-task", consumer);

        // Consumer should be called with the handler
        verify(consumer).accept(taskHandler);
    }

    @Test
    void getUserTaskEvent_withMatchingHandler_returnsUserTaskEvent() {
        // Set up connectable
        when(connectable.getTenantId()).thenReturn("tenant-1");
        when(connectable.getBpmnProcessId()).thenReturn("order-process");
        when(connectable.getTaskDefinition()).thenReturn("review-task");
        eventHandler.addTaskHandler(connectable, taskHandler);

        // Create user task event
        final var event = mock(Camunda8UserTaskEvent.class);
        when(event.getTenantId()).thenReturn("tenant-1");
        when(event.getElementId()).thenReturn("Activity_Review");
        when(event.getBpmnProcessId()).thenReturn("order-process");
        when(event.getProcessDefinitionVersion()).thenReturn(1);
        when(event.getProcessInstanceKey()).thenReturn(12345L);
        when(event.getTaskDefinition()).thenReturn("review-task");

        // Set up handler response
        final var expectedUserTask = mock(UserTaskEventImpl.class);
        when(taskHandler.getUserTask(event, null)).thenReturn(expectedUserTask);

        // Get user task event
        final var result = eventHandler.getUserTaskEvent(event, null);

        // Verify result
        assertThat(result).isSameAs(expectedUserTask);
    }

    @Test
    void getUserTaskEvent_withNoMatchingHandler_returnsNull() {
        // No handlers registered
        final var event = mock(Camunda8UserTaskEvent.class);
        when(event.getTenantId()).thenReturn("unknown-tenant");
        when(event.getElementId()).thenReturn("Activity_Review");
        when(event.getBpmnProcessId()).thenReturn("order-process");
        when(event.getProcessDefinitionVersion()).thenReturn(1);
        when(event.getProcessInstanceKey()).thenReturn(12345L);
        when(event.getTaskDefinition()).thenReturn("review-task");

        // Get user task event
        final var result = eventHandler.getUserTaskEvent(event, null);

        // Should return null
        assertThat(result).isNull();
    }

    @Test
    void processEvent_withNullTenant_mapsToDefault() {
        // Set up connectable with null tenant
        when(connectable.getTenantId()).thenReturn(null);
        when(connectable.getBpmnProcessId()).thenReturn("order-process");
        when(connectable.getTaskDefinition()).thenReturn("review-task");
        eventHandler.addTaskHandler(connectable, taskHandler);

        // Process event with null tenant
        @SuppressWarnings("unchecked")
        Consumer<Camunda8UserTaskHandler> consumer = mock(Consumer.class);
        eventHandler.processEvent(null, "Activity_1", "order-process", 1, 12345L, "review-task", consumer);

        // Consumer should be called
        verify(consumer).accept(taskHandler);
    }

}
