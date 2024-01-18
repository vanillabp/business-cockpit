package io.vanillabp.cockpit.adapter.camunda7.usertask.publishing;

import java.util.LinkedList;
import java.util.List;

import org.springframework.context.event.EventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import io.vanillabp.cockpit.adapter.common.usertask.UserTaskPublishing;

public class Camunda7UserTaskEventPublisher {

    private final static ThreadLocal<List<UserTaskEvent>> events = ThreadLocal.withInitial(() -> new LinkedList<>());

    private final UserTaskPublishing userTaskPublishing;

    public Camunda7UserTaskEventPublisher(
            final UserTaskPublishing userTaskPublishing) {

        this.userTaskPublishing = userTaskPublishing;
        
    }

    @EventListener
    public void addEvent(
            UserTaskEvent userTaskEvent) {
        
        events.get().add(userTaskEvent);
        
    }

    @TransactionalEventListener(
            value = ProcessUserTaskEvent.class,
            fallbackExecution = true,
            phase = TransactionPhase.AFTER_COMMIT)
    public void handle(
            final ProcessUserTaskEvent triggerEvent) {
        
        try {
            events
                    .get()
                    .stream()
                    .map(UserTaskEvent::getEvent)
                    .forEach(userTaskPublishing::publish);

        } finally {
            
            events.get().clear();
            
        }
        
    }

    @TransactionalEventListener(
            value = ProcessUserTaskEvent.class,
            fallbackExecution = false,
            phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleRollback(
            final ProcessUserTaskEvent triggerEvent) {
        
        events.get().clear();

    }

}
