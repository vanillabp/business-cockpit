package io.vanillabp.cockpit.adapter.camunda8.workflow;

import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowCreatedEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowLifeCycleEvent;
import io.vanillabp.cockpit.adapter.camunda8.workflow.publishing.ProcessWorkflowEvent;
import io.vanillabp.cockpit.adapter.camunda8.workflow.publishing.WorkflowEvent;
import io.vanillabp.cockpit.adapter.common.workflow.WorkflowPublishing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Transactional
public class Camunda8WorkflowEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(Camunda8WorkflowEventHandler.class);

    private final static ThreadLocal<List<WorkflowEvent>> events = ThreadLocal.withInitial(LinkedList::new);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final Map<String, Camunda8WorkflowHandler> camunda8WorkflowHandlerMap = new HashMap<>();
    private final WorkflowPublishing workflowPublishing;

    public Camunda8WorkflowEventHandler(ApplicationEventPublisher applicationEventPublisher,
                                        WorkflowPublishing workflowPublishing) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.workflowPublishing = workflowPublishing;
    }


    public void addWorkflowHandler(String bpmnProcessId, Camunda8WorkflowHandler workflowHandler) {
        this.camunda8WorkflowHandlerMap.put(bpmnProcessId, workflowHandler);
    }

    public void processWorkflowCreatedEvent(Camunda8WorkflowCreatedEvent workflowCreatedEvent) {
        Camunda8WorkflowHandler camunda8WorkflowHandler =
                camunda8WorkflowHandlerMap.get(workflowCreatedEvent.getBpmnProcessId());

        io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowEvent workflowEvent =
                camunda8WorkflowHandler.processCreatedEvent(workflowCreatedEvent);

        sendWorkflowEvent(workflowEvent);
    }


    public void processWorkflowLifecycleEvent(Camunda8WorkflowLifeCycleEvent camunda8WorkflowLifeCycleEvent) {
        Camunda8WorkflowHandler camunda8WorkflowHandler =
                camunda8WorkflowHandlerMap.get(camunda8WorkflowLifeCycleEvent.getBpmnProcessId());

        io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowEvent workflowEvent =
                camunda8WorkflowHandler.processLifeCycleEvent(camunda8WorkflowLifeCycleEvent);

        sendWorkflowEvent(workflowEvent);
    }

    public void processWorkflowUpdateEvent(Camunda8WorkflowCreatedEvent camunda8WorkflowCreatedEvent) {
        Camunda8WorkflowHandler camunda8WorkflowHandler =
                camunda8WorkflowHandlerMap.get(camunda8WorkflowCreatedEvent.getBpmnProcessId());

        if(camunda8WorkflowHandler == null){
            logger.warn(
                    "Received update event, but no workflow handler is registered for bpmn process id {}.",
                    camunda8WorkflowCreatedEvent.getBpmnProcessId());
            return;
        }

        io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowEvent workflowEvent =
                camunda8WorkflowHandler.processUpdatedEvent(camunda8WorkflowCreatedEvent);

        sendWorkflowEvent(workflowEvent);
    }

    private void sendWorkflowEvent(io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowEvent workflowEvent) {
        applicationEventPublisher.publishEvent(
                new WorkflowEvent(
                        Camunda8WorkflowEventHandler.class,
                        workflowEvent));
    }

    @EventListener
    public void addEvent(
            WorkflowEvent workflowEvent) {

        events.get().add(workflowEvent);
        applicationEventPublisher.publishEvent(
                new ProcessWorkflowEvent(
                        Camunda8WorkflowEventHandler.class));

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

}