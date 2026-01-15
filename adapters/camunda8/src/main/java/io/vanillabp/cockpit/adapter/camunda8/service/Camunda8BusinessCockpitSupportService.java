package io.vanillabp.cockpit.adapter.camunda8.service;

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
