package io.vanillabp.cockpit.adapter.camunda8.usertask;

import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8UserTaskCreatedEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8UserTaskLifecycleEvent;
import io.vanillabp.cockpit.adapter.camunda8.wiring.Camunda8UserTaskConnectable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class Camunda8UserTaskEventHandler{

    private static final Logger logger = LoggerFactory
            .getLogger(Camunda8UserTaskEventHandler.class);
    
    private final Map<Camunda8UserTaskConnectable, Camunda8UserTaskHandler> taskHandlers;

    private final Set<String> knownTenantIds = new HashSet<>();

    public Camunda8UserTaskEventHandler() {
        this.taskHandlers = new HashMap<>();
    }

    public void addTaskHandler(
            final Camunda8UserTaskConnectable connectable,
            final Camunda8UserTaskHandler taskHandler) {
        this.knownTenantIds.add(mapTenantId(connectable.getTenantId()));
        taskHandlers.put(connectable, taskHandler);
        
    }

    public void notify(
            final Camunda8UserTaskCreatedEvent userTaskCreatedEvent) {
        notify(
                userTaskCreatedEvent.getTenantId(),
                userTaskCreatedEvent.getElementId(),
                userTaskCreatedEvent.getBpmnProcessId(),
                userTaskCreatedEvent.getWorkflowDefinitionVersion(),
                userTaskCreatedEvent.getProcessInstanceKey(),
                userTaskCreatedEvent.getFormKey(),
                camunda8UserTaskHandler -> camunda8UserTaskHandler.notify(userTaskCreatedEvent)
        );
    }

    public void notify(
            final Camunda8UserTaskLifecycleEvent lifecycleEvent) {
        notify(
                lifecycleEvent.getTenantId(),
                lifecycleEvent.getElementId(),
                lifecycleEvent.getBpmnProcessId(),
                lifecycleEvent.getWorkflowDefinitionVersion(),
                lifecycleEvent.getProcessInstanceKey(),
                lifecycleEvent.getFormKey(),
                camunda8UserTaskHandler -> camunda8UserTaskHandler.notify(lifecycleEvent)
        );
    }

    private String mapTenantId(String tenantId) {
        return tenantId == null ? "default" : tenantId;
    }

    private boolean isTenantKnown(String tenantId) {
        return this.knownTenantIds.contains(mapTenantId(tenantId));
    }

    private void notify(
            String tenantId,
            String elementId,
            String bpmnProcessId,
            int bpmnVersion,
            long processInstanceKey,
            String taskDefinition,
            Consumer<Camunda8UserTaskHandler> camunda8UserTaskHandlerConsumer) {

        final var mappedTenantId = mapTenantId(tenantId);
        taskHandlers
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().getTenantId().equals(mappedTenantId))
                .filter(entry -> entry.getKey().getBpmnProcessId().equals(bpmnProcessId))
                .filter(entry -> entry.getKey().getTaskDefinition().equals(taskDefinition))
                .findFirst()
                .map(Map.Entry::getValue)
                .ifPresentOrElse(
                        camunda8UserTaskHandlerConsumer,
                        () -> {
                            if (!isTenantKnown(tenantId)) {
                                return;
                            }
                            logger.debug(
                                    "No handler found for user-task event '{}' (task-definition: '{}') of workflow '{}' (version: '{}', key: '{}') and tenant '{}'",
                                    elementId,
                                    taskDefinition,
                                    bpmnProcessId,
                                    bpmnVersion,
                                    processInstanceKey,
                                    tenantId);
                        });

    }

}
