package io.vanillabp.cockpit.adapter.camunda7.workflow;

import io.vanillabp.cockpit.adapter.camunda7.workflow.publishing.ProcessWorkflowAfterTransactionEvent;
import io.vanillabp.cockpit.adapter.camunda7.workflow.publishing.ProcessWorkflowEvent;
import io.vanillabp.cockpit.adapter.camunda7.workflow.publishing.WorkflowAfterTransactionEvent;
import io.vanillabp.cockpit.adapter.camunda7.workflow.publishing.WorkflowEvent;
import io.vanillabp.cockpit.adapter.common.properties.CockpitProperties;
import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.cockpit.adapter.common.workflow.WorkflowPublishing;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCreatedEvent;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.history.HistoricProcessInstanceQuery;
import org.camunda.bpm.engine.impl.history.event.HistoricProcessInstanceEventEntity;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link Camunda7WorkflowEventHandler}.
 */
@ExtendWith(MockitoExtension.class)
class Camunda7WorkflowEventHandlerTest {

    @Mock
    private VanillaBpCockpitProperties properties;

    @Mock
    private CockpitProperties cockpitProperties;

    @Mock
    private HistoryService historyService;

    @Mock
    private RepositoryService repositoryService;

    @Mock
    private WorkflowPublishing workflowPublishing;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private Camunda7WorkflowHandler workflowHandler;

    private Camunda7WorkflowEventHandler handler;

    @BeforeEach
    void setUp() {
        handler = new Camunda7WorkflowEventHandler(
                properties,
                historyService,
                repositoryService,
                workflowPublishing,
                applicationEventPublisher
        );
    }

    @Test
    void addWorkflowHandler_registersHandler() {
        // Act
        handler.addWorkflowHandler("test-process", workflowHandler);

        // Assert - no exception means success (handler is added to internal map)
        verifyNoInteractions(workflowHandler);
    }

    @Test
    void triggerEventAfterTransaction_publishesWorkflowAfterTransactionEvent() {
        // Arrange
        ArgumentCaptor<WorkflowAfterTransactionEvent> captor = ArgumentCaptor.forClass(WorkflowAfterTransactionEvent.class);

        // Act
        handler.triggerEventAfterTransaction("PI-123");

        // Assert
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getProcessInstanceId()).isEqualTo("PI-123");
    }

    @Test
    void listenProcessInstanceEvents_skipsWhenWorkflowListDisabled() {
        // Arrange
        when(properties.getCockpit()).thenReturn(cockpitProperties);
        when(cockpitProperties.isWorkflowListEnabled()).thenReturn(false);
        HistoricProcessInstanceEventEntity event = mock(HistoricProcessInstanceEventEntity.class);

        // Act
        handler.listenProcessInstanceEvents(event);

        // Assert
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    void listenProcessInstanceEvents_skipsSubProcessInstances() {
        // Arrange
        when(properties.getCockpit()).thenReturn(cockpitProperties);
        when(cockpitProperties.isWorkflowListEnabled()).thenReturn(true);
        HistoricProcessInstanceEventEntity event = mock(HistoricProcessInstanceEventEntity.class);
        when(event.getSuperProcessInstanceId()).thenReturn("parent-process-id");

        // Act
        handler.listenProcessInstanceEvents(event);

        // Assert - no workflow event published for sub-process
        verify(applicationEventPublisher, never()).publishEvent(any(WorkflowEvent.class));
    }

    @Test
    void addEvent_collectsAndPublishesProcessWorkflowEvent() {
        // Arrange
        WorkflowCreatedEvent mockEvent = mock(WorkflowCreatedEvent.class);
        WorkflowEvent workflowEvent = new WorkflowEvent(this, mockEvent);

        // Act
        handler.addEvent(workflowEvent);

        // Assert
        verify(applicationEventPublisher).publishEvent(any(ProcessWorkflowEvent.class));
    }

    @Test
    void addWorkflowToBeProcessedAfterTransaction_collectsWorkflowId() {
        // Arrange
        WorkflowAfterTransactionEvent event = new WorkflowAfterTransactionEvent(this, "PI-456");

        // Act
        handler.addWorkflowToBeProcessedAfterTransaction(event);

        // Assert
        verify(applicationEventPublisher).publishEvent(any(ProcessWorkflowAfterTransactionEvent.class));
    }

    @Test
    void handle_publishesCollectedEventsAfterCommit() {
        // Arrange
        WorkflowCreatedEvent mockEvent = mock(WorkflowCreatedEvent.class);
        WorkflowEvent workflowEvent = new WorkflowEvent(this, mockEvent);
        ProcessWorkflowEvent triggerEvent = new ProcessWorkflowEvent(this);

        handler.addEvent(workflowEvent);

        // Act
        handler.handle(triggerEvent);

        // Assert
        verify(workflowPublishing).publish(mockEvent);
    }

    @Test
    void handle_clearsEventsAfterPublishing() {
        // Arrange
        WorkflowCreatedEvent mockEvent = mock(WorkflowCreatedEvent.class);
        WorkflowEvent workflowEvent = new WorkflowEvent(this, mockEvent);
        ProcessWorkflowEvent triggerEvent = new ProcessWorkflowEvent(this);

        handler.addEvent(workflowEvent);
        handler.handle(triggerEvent);

        // Act - handle again without adding new events
        handler.handle(triggerEvent);

        // Assert - should only have published once
        verify(workflowPublishing, times(1)).publish(mockEvent);
    }

    @Test
    void handleRollback_clearsEventsWithoutPublishing() {
        // Arrange
        WorkflowCreatedEvent mockEvent = mock(WorkflowCreatedEvent.class);
        WorkflowEvent workflowEvent = new WorkflowEvent(this, mockEvent);
        ProcessWorkflowEvent triggerEvent = new ProcessWorkflowEvent(this);

        handler.addEvent(workflowEvent);

        // Act
        handler.handleRollback(triggerEvent);

        // Assert - nothing should be published
        verify(workflowPublishing, never()).publish(any());
    }

    @Test
    void processEventsAfterTransaction_queriesHistoryAndPublishes() {
        // Arrange
        WorkflowAfterTransactionEvent workflowEvent = new WorkflowAfterTransactionEvent(this, "PI-789");
        ProcessWorkflowAfterTransactionEvent triggerEvent = new ProcessWorkflowAfterTransactionEvent(this);

        HistoricProcessInstanceQuery query = mock(HistoricProcessInstanceQuery.class);
        when(historyService.createHistoricProcessInstanceQuery()).thenReturn(query);
        when(query.processInstanceIds(any(Set.class))).thenReturn(query);
        when(query.list()).thenReturn(List.of());

        handler.addWorkflowToBeProcessedAfterTransaction(workflowEvent);

        // Act
        handler.processEventsAfterTransaction(triggerEvent);

        // Assert
        verify(historyService).createHistoricProcessInstanceQuery();
    }

    @Test
    void processEventsAfterTransaction_skipsWhenNoEvents() {
        // Arrange
        ProcessWorkflowAfterTransactionEvent triggerEvent = new ProcessWorkflowAfterTransactionEvent(this);

        // Act
        handler.processEventsAfterTransaction(triggerEvent);

        // Assert
        verifyNoInteractions(historyService);
    }

    @Test
    void handleRollbackForEventsAfterTransaction_clearsWorkflows() {
        // Arrange
        WorkflowAfterTransactionEvent workflowEvent = new WorkflowAfterTransactionEvent(this, "PI-999");
        ProcessWorkflowAfterTransactionEvent triggerEvent = new ProcessWorkflowAfterTransactionEvent(this);

        handler.addWorkflowToBeProcessedAfterTransaction(workflowEvent);

        // Act
        handler.handleRollbackForEventsAfterTransaction(triggerEvent);

        // Assert - try to process, should do nothing since cleared
        handler.processEventsAfterTransaction(triggerEvent);
        verifyNoInteractions(historyService);
    }

    @Test
    void listenProcessInstanceEvents_withNoRegisteredHandler_skipsEvent() {
        // Arrange
        when(properties.getCockpit()).thenReturn(cockpitProperties);
        when(cockpitProperties.isWorkflowListEnabled()).thenReturn(true);

        HistoricProcessInstanceEventEntity event = mock(HistoricProcessInstanceEventEntity.class);
        when(event.getSuperProcessInstanceId()).thenReturn(null);
        when(event.getProcessDefinitionKey()).thenReturn("unregistered-process");

        // Act
        handler.listenProcessInstanceEvents(event);

        // Assert - no workflow event published when handler not found
        verify(applicationEventPublisher, never()).publishEvent(any(WorkflowEvent.class));
    }

    @Test
    void listenProcessInstanceEvents_withRegisteredHandler_publishesEvent() {
        // Arrange
        when(properties.getCockpit()).thenReturn(cockpitProperties);
        when(cockpitProperties.isWorkflowListEnabled()).thenReturn(true);

        ProcessDefinition processDefinition = mock(ProcessDefinition.class);
        when(processDefinition.getVersionTag()).thenReturn("1.0");
        when(processDefinition.getVersion()).thenReturn(1);
        when(repositoryService.getProcessDefinition("process-def-id")).thenReturn(processDefinition);

        WorkflowCreatedEvent mockWorkflowEvent = mock(WorkflowCreatedEvent.class);
        when(workflowHandler.wrapProcessInstanceEvent(any(), anyString(), anyString()))
                .thenReturn(mockWorkflowEvent);

        // Register handler first
        handler.addWorkflowHandler("test-process", workflowHandler);

        HistoricProcessInstanceEventEntity event = mock(HistoricProcessInstanceEventEntity.class);
        when(event.getSuperProcessInstanceId()).thenReturn(null);
        when(event.getProcessDefinitionKey()).thenReturn("test-process");
        when(event.getProcessDefinitionId()).thenReturn("process-def-id");

        // Act
        handler.listenProcessInstanceEvents(event);

        // Assert - workflow event should be published
        verify(applicationEventPublisher).publishEvent(any(WorkflowEvent.class));
    }

    @Test
    void listenProcessInstanceEvents_withVersionTagNull_usesVersionNumber() {
        // Arrange
        when(properties.getCockpit()).thenReturn(cockpitProperties);
        when(cockpitProperties.isWorkflowListEnabled()).thenReturn(true);

        ProcessDefinition processDefinition = mock(ProcessDefinition.class);
        when(processDefinition.getVersionTag()).thenReturn(null);
        when(processDefinition.getVersion()).thenReturn(5);
        when(repositoryService.getProcessDefinition("process-def-id")).thenReturn(processDefinition);

        WorkflowCreatedEvent mockWorkflowEvent = mock(WorkflowCreatedEvent.class);
        when(workflowHandler.wrapProcessInstanceEvent(any(), anyString(), eq("5")))
                .thenReturn(mockWorkflowEvent);

        handler.addWorkflowHandler("test-process", workflowHandler);

        HistoricProcessInstanceEventEntity event = mock(HistoricProcessInstanceEventEntity.class);
        when(event.getSuperProcessInstanceId()).thenReturn(null);
        when(event.getProcessDefinitionKey()).thenReturn("test-process");
        when(event.getProcessDefinitionId()).thenReturn("process-def-id");

        // Act
        handler.listenProcessInstanceEvents(event);

        // Assert - version should be just the number
        verify(workflowHandler).wrapProcessInstanceEvent(event, "test-process", "5");
    }

    @Test
    void listenProcessInstanceEvents_withVersionTag_usesTagAndNumber() {
        // Arrange
        when(properties.getCockpit()).thenReturn(cockpitProperties);
        when(cockpitProperties.isWorkflowListEnabled()).thenReturn(true);

        ProcessDefinition processDefinition = mock(ProcessDefinition.class);
        when(processDefinition.getVersionTag()).thenReturn("2.1.0");
        when(processDefinition.getVersion()).thenReturn(3);
        when(repositoryService.getProcessDefinition("process-def-id")).thenReturn(processDefinition);

        WorkflowCreatedEvent mockWorkflowEvent = mock(WorkflowCreatedEvent.class);
        when(workflowHandler.wrapProcessInstanceEvent(any(), anyString(), eq("2.1.0:3")))
                .thenReturn(mockWorkflowEvent);

        handler.addWorkflowHandler("test-process", workflowHandler);

        HistoricProcessInstanceEventEntity event = mock(HistoricProcessInstanceEventEntity.class);
        when(event.getSuperProcessInstanceId()).thenReturn(null);
        when(event.getProcessDefinitionKey()).thenReturn("test-process");
        when(event.getProcessDefinitionId()).thenReturn("process-def-id");

        // Act
        handler.listenProcessInstanceEvents(event);

        // Assert - version should be tag:number
        verify(workflowHandler).wrapProcessInstanceEvent(event, "test-process", "2.1.0:3");
    }

    @Test
    void handle_withMultipleEvents_publishesAll() {
        // Arrange
        WorkflowCreatedEvent mockEvent1 = mock(WorkflowCreatedEvent.class);
        WorkflowCreatedEvent mockEvent2 = mock(WorkflowCreatedEvent.class);
        WorkflowEvent workflowEvent1 = new WorkflowEvent(this, mockEvent1);
        WorkflowEvent workflowEvent2 = new WorkflowEvent(this, mockEvent2);
        ProcessWorkflowEvent triggerEvent = new ProcessWorkflowEvent(this);

        handler.addEvent(workflowEvent1);
        handler.addEvent(workflowEvent2);

        // Act
        handler.handle(triggerEvent);

        // Assert
        verify(workflowPublishing).publish(mockEvent1);
        verify(workflowPublishing).publish(mockEvent2);
    }
}
