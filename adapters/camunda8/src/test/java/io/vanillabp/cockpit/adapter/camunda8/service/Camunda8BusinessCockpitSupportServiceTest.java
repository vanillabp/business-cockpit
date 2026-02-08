package io.vanillabp.cockpit.adapter.camunda8.service;

import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8UserTaskEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowEvent;
import io.vanillabp.cockpit.adapter.camunda8.usertask.Camunda8UserTaskEventHandler;
import io.vanillabp.cockpit.adapter.camunda8.usertask.Camunda8UserTaskHandler;
import io.vanillabp.cockpit.adapter.camunda8.workflow.Camunda8WorkflowEventHandler;
import io.vanillabp.cockpit.adapter.camunda8.workflow.Camunda8WorkflowHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Camunda8BusinessCockpitSupportServiceTest {

    @Mock
    private Camunda8WorkflowEventHandler workflowEventHandler;

    @Mock
    private Camunda8UserTaskEventHandler userTaskEventHandler;

    @Mock
    private Camunda8WorkflowEvent workflowEvent;

    @Mock
    private Camunda8UserTaskEvent userTaskEvent;

    @Mock
    private Camunda8WorkflowHandler workflowHandler;

    @Mock
    private Camunda8UserTaskHandler userTaskHandler;

    @Captor
    private ArgumentCaptor<Consumer<Camunda8WorkflowHandler>> workflowConsumerCaptor;

    @Captor
    private ArgumentCaptor<Consumer<Camunda8UserTaskHandler>> userTaskConsumerCaptor;

    private Camunda8BusinessCockpitSupportService service;

    @BeforeEach
    void setUp() {
        service = new Camunda8BusinessCockpitSupportService(workflowEventHandler, userTaskEventHandler);
    }

    @Test
    void processAggregateChangedEvent_withWorkflowEvent_delegatesToHandler() {
        // Set up event properties
        when(workflowEvent.getTenantId()).thenReturn("tenant-1");
        when(workflowEvent.getBpmnProcessId()).thenReturn("order-process");
        when(workflowEvent.getProcessDefinitionVersion()).thenReturn(1);
        when(workflowEvent.getProcessInstanceKey()).thenReturn(12345L);

        // Process the event
        service.processAggregateChangedEvent(workflowEvent);

        // Verify delegation to handler
        verify(workflowEventHandler).processEvent(
                eq("tenant-1"),
                eq("order-process"),
                eq(1),
                eq(12345L),
                any());
    }

    @Test
    void processAggregateChangedEvent_withWorkflowEvent_callsNotifyOnHandler() {
        // Set up event properties
        when(workflowEvent.getTenantId()).thenReturn("tenant-1");
        when(workflowEvent.getBpmnProcessId()).thenReturn("order-process");
        when(workflowEvent.getProcessDefinitionVersion()).thenReturn(1);
        when(workflowEvent.getProcessInstanceKey()).thenReturn(12345L);

        // Capture the consumer and invoke it
        doAnswer(invocation -> {
            Consumer<Camunda8WorkflowHandler> consumer = invocation.getArgument(4);
            consumer.accept(workflowHandler);
            return null;
        }).when(workflowEventHandler).processEvent(anyString(), anyString(), anyInt(), anyLong(), any());

        // Process the event
        service.processAggregateChangedEvent(workflowEvent);

        // Verify notify was called on handler
        verify(workflowHandler).notify(workflowEvent);
    }

    @Test
    void processAggregateChangedEvent_withUserTaskEvent_delegatesToHandler() {
        // Set up event properties
        when(userTaskEvent.getTenantId()).thenReturn("tenant-1");
        when(userTaskEvent.getElementId()).thenReturn("Activity_Review");
        when(userTaskEvent.getBpmnProcessId()).thenReturn("order-process");
        when(userTaskEvent.getProcessDefinitionVersion()).thenReturn(1);
        when(userTaskEvent.getProcessInstanceKey()).thenReturn(12345L);
        when(userTaskEvent.getTaskDefinition()).thenReturn("review-task");

        // Process the event
        service.processAggregateChangedEvent(userTaskEvent);

        // Verify delegation to handler
        verify(userTaskEventHandler).processEvent(
                eq("tenant-1"),
                eq("Activity_Review"),
                eq("order-process"),
                eq(1),
                eq(12345L),
                eq("review-task"),
                any());
    }

    @Test
    void processAggregateChangedEvent_withUserTaskEvent_callsNotifyOnHandler() {
        // Set up event properties
        when(userTaskEvent.getTenantId()).thenReturn("tenant-1");
        when(userTaskEvent.getElementId()).thenReturn("Activity_Review");
        when(userTaskEvent.getBpmnProcessId()).thenReturn("order-process");
        when(userTaskEvent.getProcessDefinitionVersion()).thenReturn(1);
        when(userTaskEvent.getProcessInstanceKey()).thenReturn(12345L);
        when(userTaskEvent.getTaskDefinition()).thenReturn("review-task");

        // Capture the consumer and invoke it
        doAnswer(invocation -> {
            Consumer<Camunda8UserTaskHandler> consumer = invocation.getArgument(6);
            consumer.accept(userTaskHandler);
            return null;
        }).when(userTaskEventHandler).processEvent(anyString(), anyString(), anyString(), anyInt(), anyLong(), anyString(), any());

        // Process the event
        service.processAggregateChangedEvent(userTaskEvent);

        // Verify notify was called on handler
        verify(userTaskHandler).notify(userTaskEvent);
    }

}
