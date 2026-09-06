package io.vanillabp.cockpit.adapter.camunda8.service;

import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8AggregateChangedEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8UserTaskEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowEvent;
import io.vanillabp.cockpit.adapter.camunda8.usertask.Camunda8UserTaskEventHandler;
import io.vanillabp.cockpit.adapter.camunda8.workflow.Camunda8WorkflowEventHandler;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

public class Camunda8BusinessCockpitSupportService {

    private final Camunda8WorkflowEventHandler workflowEventHandler;

    private final Camunda8UserTaskEventHandler userTaskEventHandler;

    public Camunda8BusinessCockpitSupportService(Camunda8WorkflowEventHandler workflowEventHandler, Camunda8UserTaskEventHandler userTaskEventHandler) {
        this.workflowEventHandler = workflowEventHandler;
        this.userTaskEventHandler = userTaskEventHandler;
    }

    /**
     * Asks Camunda 8 which workflows a changed aggregate belongs to, once the transaction which
     * changed it is through. Deliberately without a transaction of its own: the lookup may have to
     * wait for the cluster to export a workflow started moments ago, and a transaction held open for
     * that long would be paid for by the application's database. The workflow events this produces
     * bring their own transaction, one per event, through the listener below.
     */
    @TransactionalEventListener(
            value = Camunda8AggregateChangedEvent.class,
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    public void processAggregateChangedEvent(
            final Camunda8AggregateChangedEvent event) {

        event.findWorkflowsAndNotifyTheCockpit();

    }

    @TransactionalEventListener(
            value = Camunda8WorkflowEvent.class,
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processAggregateChangedEvent(
            final Camunda8WorkflowEvent event) {

        workflowEventHandler.processEvent(
                event.getTenantId(),
                event.getBpmnProcessId(),
                event.getProcessDefinitionVersion(),
                event.getProcessInstanceKey(),
                camunda8WorkflowHandler -> camunda8WorkflowHandler.notify(event));

    }

    @TransactionalEventListener(
            value = Camunda8UserTaskEvent.class,
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processAggregateChangedEvent(
            final Camunda8UserTaskEvent event) {

        userTaskEventHandler.processEvent(
                event.getTenantId(),
                event.getElementId(),
                event.getBpmnProcessId(),
                event.getProcessDefinitionVersion(),
                event.getProcessInstanceKey(),
                event.getTaskDefinition(),
                camunda8UserTaskHandler -> camunda8UserTaskHandler.notify(event));

    }

}
