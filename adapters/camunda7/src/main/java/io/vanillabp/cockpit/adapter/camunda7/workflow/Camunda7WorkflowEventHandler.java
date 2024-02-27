package io.vanillabp.cockpit.adapter.camunda7.workflow;

import io.vanillabp.cockpit.adapter.camunda7.workflow.publishing.ProcessWorkflowAfterTransactionEvent;
import io.vanillabp.cockpit.adapter.camunda7.workflow.publishing.ProcessWorkflowEvent;
import io.vanillabp.cockpit.adapter.camunda7.workflow.publishing.WorkflowAfterTransactionEvent;
import io.vanillabp.cockpit.adapter.camunda7.workflow.publishing.WorkflowEvent;
import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.cockpit.adapter.common.workflow.WorkflowPublishing;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.impl.history.event.HistoricProcessInstanceEventEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Camunda7WorkflowEventHandler {
    private static final Logger logger = LoggerFactory
            .getLogger(Camunda7WorkflowEventHandler.class);
    private static final Map<String,Camunda7WorkflowHandler> workflowHandlerMap = new HashMap<>();
    private final static ThreadLocal<List<WorkflowEvent>> events = ThreadLocal.withInitial(() -> new LinkedList<>());
    private final static ThreadLocal<List<String>> workflowsAfterTransaction = ThreadLocal.withInitial(() -> new LinkedList<>());
    
    private final WorkflowPublishing workflowPublishing;
    private final VanillaBpCockpitProperties properties;
    private final RepositoryService repositoryService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final HistoryService historyService;

    public Camunda7WorkflowEventHandler(
            final VanillaBpCockpitProperties properties,
            final HistoryService historyService,
            final RepositoryService repositoryService,
            final WorkflowPublishing workflowPublishing,
            final ApplicationEventPublisher applicationEventPublisher) {
        this.properties = properties;
        this.repositoryService = repositoryService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.workflowPublishing = workflowPublishing;
        this.historyService = historyService;
    }

    public void addWorkflowHandler(String forBpmnProcessId, Camunda7WorkflowHandler camunda7WorkflowHandler) {
        workflowHandlerMap.put(forBpmnProcessId, camunda7WorkflowHandler);
    }

    @EventListener
    public void listenProcessInstanceEvents(HistoricProcessInstanceEventEntity processInstanceEvent) {
        
        if (!properties.getCockpit().isWorkflowListEnabled()) {
            return;
        }

        processEventInTransaction(processInstanceEvent);
        
    }
    
    public void triggerEventAfterTransaction(
            final String workflowId) {
        
        applicationEventPublisher.publishEvent(
                new WorkflowAfterTransactionEvent(
                        Camunda7WorkflowEventHandler.class,
                        workflowId));
        
    }
    
    private void processEventInTransaction(
            final HistoricProcessInstanceEventEntity processInstanceEvent) {
        
        final var eventWrapper = processProcessInstanceHistoryEvent(processInstanceEvent);

        if (eventWrapper == null) {
            return;
        }
        
        applicationEventPublisher.publishEvent(
                new WorkflowEvent(
                        Camunda7WorkflowEventHandler.class,
                        eventWrapper));
        
    }

    private io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowEvent processProcessInstanceHistoryEvent(HistoricProcessInstance processInstance) {

        // Only operate on root process instances
        if (processInstance.getSuperProcessInstanceId() != null) {
            return null;
        }

        Camunda7WorkflowHandler workflowHandler = workflowHandlerMap.get(processInstance.getProcessDefinitionKey());
        if (workflowHandler == null) {
            logger.trace("No workflow handler available for bpmnProcessId '{}'", processInstance.getProcessDefinitionId());
            return null;
        }
        final String bpmnProcessName = processInstance.getProcessDefinitionKey();
        final var processDefinition = repositoryService.getProcessDefinition(processInstance.getProcessDefinitionId());
        final String bpmnProcessVersion;
        if (StringUtils.hasText(processDefinition.getVersionTag())) {
            bpmnProcessVersion = processDefinition.getVersionTag()
                    + ":"
                    + Integer.toString(processDefinition.getVersion());
        } else {
            bpmnProcessVersion = Integer.toString(processDefinition.getVersion());
        }

        return workflowHandler.wrapProcessInstance(
                processInstance, bpmnProcessName, bpmnProcessVersion);
        
    }

    private io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowEvent processProcessInstanceHistoryEvent(HistoricProcessInstanceEventEntity processInstanceEvent) {

        // Only operate on root process instances
        if (processInstanceEvent.getSuperProcessInstanceId() != null) {
            return null;
        }

        Camunda7WorkflowHandler workflowHandler = workflowHandlerMap.get(processInstanceEvent.getProcessDefinitionKey());
        if (workflowHandler == null) {
            logger.trace("No workflow handler available for bpmnProcessId '{}'", processInstanceEvent.getProcessDefinitionId());
            return null;
        }
        final String bpmnProcessName = processInstanceEvent.getProcessDefinitionKey();
        final var processDefinition = repositoryService.getProcessDefinition(processInstanceEvent.getProcessDefinitionId());
        final String bpmnProcessVersion;
        if (StringUtils.hasText(processDefinition.getVersionTag())) {
            bpmnProcessVersion = processDefinition.getVersionTag()
                    + ":"
                    + Integer.toString(processDefinition.getVersion());
        } else {
            bpmnProcessVersion = Integer.toString(processDefinition.getVersion());
        }

        return workflowHandler.wrapProcessInstanceEvent(
                processInstanceEvent, bpmnProcessName, bpmnProcessVersion);
        
    }

    @EventListener
    public void addEvent(
            WorkflowEvent workflowEvent) {

        events.get().add(workflowEvent);
        applicationEventPublisher.publishEvent(
                new ProcessWorkflowEvent(
                        Camunda7WorkflowEventHandler.class));

    }
    
    @EventListener
    public void addWorkflowToBeProcessedAfterTransaction(
            final WorkflowAfterTransactionEvent workflowEvent) {
        
        workflowsAfterTransaction.get().add(workflowEvent.getProcessInstanceId());
        applicationEventPublisher.publishEvent(
                new ProcessWorkflowAfterTransactionEvent(
                        Camunda7WorkflowEventHandler.class));
        
    }

    @TransactionalEventListener(
            value = ProcessWorkflowEvent.class,
            fallbackExecution = true,
            phase = TransactionPhase.AFTER_COMMIT)
    public void handle(final ProcessWorkflowEvent triggerEvent) {

        try {
            events
                    .get()
                    .stream()
                    .map(WorkflowEvent::getEvent)
                    .forEach(workflowPublishing::publish);

        } finally {

            events.get().clear();

        }
    }

    @TransactionalEventListener(
            value = ProcessWorkflowEvent.class,
            fallbackExecution = false,
            phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleRollback(
            final ProcessWorkflowEvent triggerEvent) {

        events.get().clear();

    }

    @TransactionalEventListener(
            value = ProcessWorkflowAfterTransactionEvent.class,
            fallbackExecution = true,
            phase = TransactionPhase.AFTER_COMMIT)
    public void processEventsAfterTransaction(
            final ProcessWorkflowAfterTransactionEvent event) {

        /* events are collected in a thread-local property and are
         * processed once the first after-commit event fires.
         * the reason for this is to remove duplicates. Since the
         * first event publishes all, all the other events have to be
         * ignored.
         */
        if (workflowsAfterTransaction.get().isEmpty()) {
            return;
        }
        try {

            historyService
                    .createHistoricProcessInstanceQuery()
                    .processInstanceIds(new HashSet<String>(workflowsAfterTransaction.get().stream().distinct().toList()))
                    .list()
                    .stream()
                    .map(this::processProcessInstanceHistoryEvent)
                    .filter(Objects::nonNull)
                    .forEach(workflowPublishing::publish);

        } finally {
            
            workflowsAfterTransaction.get().clear();
            
        }
        
    }

    @TransactionalEventListener(
            value = ProcessWorkflowAfterTransactionEvent.class,
            fallbackExecution = false,
            phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleRollbackForEventsAfterTransaction(
            final ProcessWorkflowAfterTransactionEvent event) {

        workflowsAfterTransaction.get().clear();
        
    }

}
