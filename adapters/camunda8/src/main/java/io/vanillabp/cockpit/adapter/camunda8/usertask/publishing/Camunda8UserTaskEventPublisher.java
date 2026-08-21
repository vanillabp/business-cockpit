package io.vanillabp.cockpit.adapter.camunda8.usertask.publishing;

import io.vanillabp.cockpit.adapter.common.usertask.UserTaskPublishing;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

public class Camunda8UserTaskEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(Camunda8UserTaskEventPublisher.class);

    private final static ThreadLocal<List<UserTaskEvent>> events = ThreadLocal.withInitial(LinkedList::new);

    private final UserTaskPublishing userTaskPublishing;

    public Camunda8UserTaskEventPublisher(
            final UserTaskPublishing userTaskPublishing) {

        this.userTaskPublishing = userTaskPublishing;
        
    }

    @EventListener
    public void addEvent(
            UserTaskEvent userTaskEvent) {
        
        events.get().add(userTaskEvent);
        
    }

    @TransactionalEventListener(
            value = ProcessUserTaskAfterTransactionEvent.class,
            fallbackExecution = true,
            phase = TransactionPhase.AFTER_COMMIT)
    public void handle(
            final ProcessUserTaskAfterTransactionEvent triggerEvent) {
        
        try {
            events
                    .get()
                    .stream()
                    .map(UserTaskEvent::getEvent)
                    // a single failing event must not drop the remaining events of this batch
                    .forEach(event -> {
                        try {
                            userTaskPublishing.publish(event);
                        } catch (Exception e) {
                            logger.error("Could not publish user-task event '{}' of user-task '{}'!",
                                    event.getEventId(),
                                    event.getUserTaskId(),
                                    e);
                        }
                    });
        } finally {
            
            events.get().clear();
            
        }
        
    }

    @TransactionalEventListener(
            value = ProcessUserTaskAfterTransactionEvent.class,
            fallbackExecution = false,
            phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleRollback(
            final ProcessUserTaskAfterTransactionEvent triggerEvent) {
        
        events.get().clear();

    }

}
