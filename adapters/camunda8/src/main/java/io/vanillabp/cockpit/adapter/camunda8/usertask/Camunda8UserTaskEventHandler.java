package io.vanillabp.cockpit.adapter.camunda8.usertask;

import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8UserTaskCreatedEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8UserTaskLifecycleEvent;
import io.vanillabp.cockpit.adapter.camunda8.wiring.Camunda8UserTaskConnectable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class Camunda8UserTaskEventHandler{

    private static final Logger logger = LoggerFactory
            .getLogger(Camunda8UserTaskEventHandler.class);
    
    private final Map<Camunda8UserTaskConnectable, Camunda8UserTaskHandler> taskHandlers;

    public Camunda8UserTaskEventHandler() {
        this.taskHandlers = new HashMap<>();
    }

    public void addTaskHandler(
            final Camunda8UserTaskConnectable connectable,
            final Camunda8UserTaskHandler taskHandler) {
        
        taskHandlers.put(connectable, taskHandler);
        
    }

    public void notify(
            final Camunda8UserTaskCreatedEvent userTaskCreatedEvent) {
        notify(
                userTaskCreatedEvent.getElementId(),
                userTaskCreatedEvent.getBpmnProcessId(),
                userTaskCreatedEvent.getFormKey(),
                camunda8UserTaskHandler -> camunda8UserTaskHandler.notify(userTaskCreatedEvent)
        );
    }

    public void notify(
            final Camunda8UserTaskLifecycleEvent lifecycleEvent) {
        notify(
                lifecycleEvent.getElementId(),
                lifecycleEvent.getBpmnProcessId(),
                lifecycleEvent.getFormKey(),
                camunda8UserTaskHandler -> camunda8UserTaskHandler.notify(lifecycleEvent)
        );
    }

    
    private void notify(
            String elementId,
            String bpmnProcessId,
            String taskDefinition,
            Consumer<Camunda8UserTaskHandler> camunda8UserTaskHandlerConsumer) {

        taskHandlers
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().getBpmnProcessId().equals(bpmnProcessId))
                .filter(entry -> entry.getKey().getTaskDefinition().equals(taskDefinition))
                .findFirst()
                .map(Map.Entry::getValue)
                .ifPresentOrElse(
                        camunda8UserTaskHandlerConsumer,
                        () -> logger.debug(
                                "Ignoring usertask event '{}' (task-definition: '{}') of foreign workflow: '{}'",
                                elementId,
                                taskDefinition,
                                bpmnProcessId));

    }
}
