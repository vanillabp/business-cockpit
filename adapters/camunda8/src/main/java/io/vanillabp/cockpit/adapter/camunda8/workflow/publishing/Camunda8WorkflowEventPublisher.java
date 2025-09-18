package io.vanillabp.cockpit.adapter.camunda8.workflow.publishing;

import io.vanillabp.cockpit.adapter.common.workflow.WorkflowPublishing;
import java.util.LinkedList;
import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

public class Camunda8WorkflowEventPublisher {

    private final static ThreadLocal<List<WorkflowEvent>> events = ThreadLocal.withInitial(LinkedList::new);

    private final WorkflowPublishing workflowPublishing;

    public Camunda8WorkflowEventPublisher(
            final WorkflowPublishing workflowPublishing) {

        this.workflowPublishing = workflowPublishing;

    }

    @EventListener
    public void addEvent(
            WorkflowEvent workflowEvent) {

        events.get().add(workflowEvent);

    }

    @TransactionalEventListener(
            value = ProcessWorkflowAfterTransactionEvent.class,
            fallbackExecution = true,
            phase = TransactionPhase.AFTER_COMMIT)
    public void handle(
            final ProcessWorkflowAfterTransactionEvent triggerEvent) {

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
            value = ProcessWorkflowAfterTransactionEvent.class,
            fallbackExecution = false,
            phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleRollback(
            final ProcessWorkflowAfterTransactionEvent triggerEvent) {

        events.get().clear();

    }

}
