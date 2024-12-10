package io.vanillabp.cockpit.adapter.camunda8.workflow;

import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowCreatedEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowLifeCycleEvent;
import io.vanillabp.cockpit.adapter.camunda8.workflow.publishing.ProcessWorkflowEvent;
import io.vanillabp.cockpit.adapter.camunda8.workflow.publishing.WorkflowEvent;
import io.vanillabp.cockpit.adapter.common.workflow.WorkflowPublishing;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Transactional
public class Camunda8WorkflowEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(Camunda8WorkflowEventHandler.class);

    private final static ThreadLocal<List<WorkflowEvent>> events = ThreadLocal.withInitial(LinkedList::new);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final Set<String> knownTenantIds = new HashSet<>();
    private final Map<String, Camunda8WorkflowHandler> camunda8WorkflowHandlerMap = new HashMap<>();
    private final WorkflowPublishing workflowPublishing;

    public Camunda8WorkflowEventHandler(ApplicationEventPublisher applicationEventPublisher,
                                        WorkflowPublishing workflowPublishing) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.workflowPublishing = workflowPublishing;
    }

    private String getHandlerMapKey(String tenantId, String bpmnProcessId) {
        return mapTenantId(tenantId) + "#" + bpmnProcessId;
    }

    private String getHandlerMapKey(Camunda8WorkflowCreatedEvent event) {
        return getHandlerMapKey(event.getTenantId(), event.getBpmnProcessId());
    }

    private String getHandlerMapKey(Camunda8WorkflowLifeCycleEvent event) {
        return getHandlerMapKey(event.getTenantId(), event.getBpmnProcessId());
    }

    private String mapTenantId(String tenantId) {
        return tenantId == null ? "default" : tenantId;
    }

    private boolean isTenantKnown(String tenantId) {
        return this.knownTenantIds.contains(mapTenantId(tenantId));
    }

    public void addWorkflowHandler(String tenantId, String bpmnProcessId, Camunda8WorkflowHandler workflowHandler) {
        this.knownTenantIds.add(mapTenantId(tenantId));
        this.camunda8WorkflowHandlerMap.put(getHandlerMapKey(tenantId, bpmnProcessId), workflowHandler);
    }

    public void processWorkflowCreatedEvent(Camunda8WorkflowCreatedEvent workflowCreatedEvent) {
        Camunda8WorkflowHandler camunda8WorkflowHandler =
                camunda8WorkflowHandlerMap.get(getHandlerMapKey(workflowCreatedEvent));
        if (camunda8WorkflowHandler == null) {
            if (isTenantKnown(workflowCreatedEvent.getTenantId())) {
                logger.debug("Ignoring workflow created event of foreign workflow: '{}' (version: '{}', key: '{}')",
                        workflowCreatedEvent.getBpmnProcessId(),
                        workflowCreatedEvent.getVersion(),
                        workflowCreatedEvent.getProcessInstanceKey());
            }
            return;
        }

        io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowEvent workflowEvent =
                camunda8WorkflowHandler.processCreatedEvent(workflowCreatedEvent);

        sendWorkflowEvent(workflowEvent);
    }


    public void processWorkflowLifecycleEvent(Camunda8WorkflowLifeCycleEvent camunda8WorkflowLifeCycleEvent) {
        Camunda8WorkflowHandler camunda8WorkflowHandler =
                camunda8WorkflowHandlerMap.get(getHandlerMapKey(camunda8WorkflowLifeCycleEvent));
        if (camunda8WorkflowHandler == null) {
            if (isTenantKnown(camunda8WorkflowLifeCycleEvent.getTenantId())) {
                logger.debug("No handler found for workflow lifecycle event '{}' of workflow '{}' (version: '{}', key: '{}') and tenant '{}'!",
                        camunda8WorkflowLifeCycleEvent.getIntent().name(),
                        camunda8WorkflowLifeCycleEvent.getBpmnProcessId(),
                        camunda8WorkflowLifeCycleEvent.getBpmnProcessVersion(),
                        camunda8WorkflowLifeCycleEvent.getProcessInstanceKey(),
                        camunda8WorkflowLifeCycleEvent.getTenantId());
            }
            return;
        }

        io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowEvent workflowEvent =
                camunda8WorkflowHandler.processLifeCycleEvent(camunda8WorkflowLifeCycleEvent);

        sendWorkflowEvent(workflowEvent);
    }

    public void processWorkflowUpdateEvent(Camunda8WorkflowCreatedEvent camunda8WorkflowCreatedEvent) {
        Camunda8WorkflowHandler camunda8WorkflowHandler =
                camunda8WorkflowHandlerMap.get(getHandlerMapKey(camunda8WorkflowCreatedEvent));
        if ((camunda8WorkflowHandler == null) && isTenantKnown(camunda8WorkflowCreatedEvent.getTenantId())) {
            logger.debug("No handler found for workflow update event of workflow: '{}' (version: '{}', key: '{}') and tenant '{}'!",
                    camunda8WorkflowCreatedEvent.getBpmnProcessId(),
                    camunda8WorkflowCreatedEvent.getVersion(),
                    camunda8WorkflowCreatedEvent.getProcessInstanceKey(),
                    camunda8WorkflowCreatedEvent.getTenantId());
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