package io.vanillabp.cockpit.adapter.camunda8.workflow;

import io.vanillabp.cockpit.adapter.camunda8.workflow.publishing.ProcessWorkflowEvent;
import io.vanillabp.cockpit.adapter.camunda8.workflow.publishing.WorkflowEvent;
import io.vanillabp.cockpit.adapter.common.workflow.WorkflowPublishing;
import io.zeebe.exporter.proto.Schema;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Camunda8WorkflowEventHandler {

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

    public void notify(Schema.ProcessInstanceCreationRecord processInstanceCreationRecord) {
        Camunda8WorkflowHandler camunda8WorkflowHandler =
                camunda8WorkflowHandlerMap.get(processInstanceCreationRecord.getBpmnProcessId());

        CreatedEventInformation createdEventInformation =
                Camunda8WorkflowEventMapper.map(processInstanceCreationRecord);

        io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowEvent workflowEvent =
                camunda8WorkflowHandler.processCreatedEvent(createdEventInformation);

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