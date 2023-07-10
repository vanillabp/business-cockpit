package io.vanillabp.cockpit.adapter.camunda7.workflow.publishing;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import io.vanillabp.cockpit.adapter.common.workflow.WorkflowPublishing;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

public class Camunda7WorkflowEventPublisher {

    private final static ThreadLocal<List<WorkflowEvent>> events = ThreadLocal.withInitial(() -> new LinkedList<>());

    private final WorkflowPublishing workflowPublishing;

    public Camunda7WorkflowEventPublisher(
            final WorkflowPublishing workflowPublishing) {

        this.workflowPublishing = workflowPublishing;

    }

    @EventListener
    public void addEvent(
            WorkflowEvent workflowEvent) {

        events.get().add(workflowEvent);

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
                    .collect(Collectors.groupingBy(
                            WorkflowEvent::getApiVersion,
                            Collectors.mapping(
                                    WorkflowEvent::getEvent, Collectors.toList()
                            )))
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
