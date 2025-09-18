package io.vanillabp.cockpit.adapter.camunda8.workflow;

import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.search.enums.JobKind;
import io.camunda.client.api.worker.JobClient;
import io.camunda.client.api.worker.JobHandler;
import io.vanillabp.cockpit.adapter.camunda8.deployments.Camunda8DeploymentAdapter;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowCreatedEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowLifeCycleEvent;
import io.vanillabp.cockpit.adapter.camunda8.wiring.Camunda8WorkflowConnectable;
import io.vanillabp.spi.cockpit.details.DetailsEvent;
import io.vanillabp.springboot.adapter.ModuleAwareBpmnDeployment;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;

public class Camunda8WorkflowEventHandler implements JobHandler {
    private static final Logger logger = LoggerFactory.getLogger(Camunda8WorkflowEventHandler.class);

    private final Map<Camunda8WorkflowConnectable, Camunda8WorkflowHandler> workflowHandlers = new HashMap<>();

    private final Set<String> knownTenantIds = new HashSet<>();

    private String mapTenantId(String tenantId) {
        return tenantId == null ? "<default>" : tenantId;
    }

    public boolean isTenantKnown(String tenantId) {
        return this.knownTenantIds.contains(mapTenantId(tenantId));
    }

    public void addWorkflowHandler(Camunda8WorkflowConnectable connectable, Camunda8WorkflowHandler workflowHandler) {
        this.knownTenantIds.add(mapTenantId(connectable.getTenantId()));
        this.workflowHandlers.put(connectable, workflowHandler);
    }

    public void processCreatedEvent(Camunda8WorkflowCreatedEvent workflowCreatedEvent) {
        processEvent(
                workflowCreatedEvent.getTenantId(),
                workflowCreatedEvent.getBpmnProcessId(),
                workflowCreatedEvent.getWorkflowDefinitionVersion(),
                workflowCreatedEvent.getProcessInstanceKey(),
                camunda8WorkflowHandlerHandler -> camunda8WorkflowHandlerHandler.notify(workflowCreatedEvent)
        );
    }

    public void processLifecycleEvent(
            final Camunda8WorkflowLifeCycleEvent camunda8WorkflowLifeCycleEvent) {
        processEvent(
                camunda8WorkflowLifeCycleEvent.getTenantId(),
                camunda8WorkflowLifeCycleEvent.getBpmnProcessId(),
                camunda8WorkflowLifeCycleEvent.getWorkflowDefinitionVersion(),
                camunda8WorkflowLifeCycleEvent.getProcessInstanceKey(),
                camunda8WorkflowHandlerHandler -> camunda8WorkflowHandlerHandler.notify(camunda8WorkflowLifeCycleEvent)
        );
    }

    public void processWorkflowUpdateEvent(Camunda8WorkflowCreatedEvent camunda8WorkflowUpdatedEvent) {
        processEvent(
                camunda8WorkflowUpdatedEvent.getTenantId(),
                camunda8WorkflowUpdatedEvent.getBpmnProcessId(),
                camunda8WorkflowUpdatedEvent.getWorkflowDefinitionVersion(),
                camunda8WorkflowUpdatedEvent.getProcessInstanceKey(),
                camunda8WorkflowHandlerHandler -> camunda8WorkflowHandlerHandler.notify(camunda8WorkflowUpdatedEvent)
        );
    }

    private void processEvent(
            String tenantId,
            String bpmnProcessId,
            int bpmnVersion,
            long processInstanceKey,
            Consumer<Camunda8WorkflowHandler> camunda8WorkflowHandlerConsumer) {

        final var mappedTenantId = mapTenantId(tenantId);
        workflowHandlers
                .entrySet()
                .stream()
                .filter(entry -> mapTenantId(entry.getKey().getTenantId()).equals(mappedTenantId))
                .filter(entry -> entry.getKey().getBpmnProcessId().equals(bpmnProcessId))
                .filter(entry -> entry.getKey().getTaskDefinition().equals(bpmnProcessId))
                .findFirst()
                .map(Map.Entry::getValue)
                .ifPresentOrElse(
                        camunda8WorkflowHandlerConsumer,
                        () -> {
                            if (!isTenantKnown(tenantId)) {
                                return;
                            }
                            logger.debug(
                                    "No handler found for workflow event of workflow '{}' (version: '{}', key: '{}') and tenant '{}'",
                                    bpmnProcessId,
                                    bpmnVersion,
                                    processInstanceKey,
                                    tenantId);
                        });
    }

    @Override
    public void handle(
            final JobClient client,
            final ActivatedJob job) throws Exception {

        if (job.getKind() != JobKind.EXECUTION_LISTENER) {
            logger.warn("Received job of type '{}' which is not a execution listener! Will ignore it.", job.getKind());
            client.newCompleteCommand(job).send().join();
            return;
        }

        final var event = new Camunda8WorkflowEvent();
        final var eventType = switch (job.getListenerEventType()) {
            case START -> DetailsEvent.Event.CREATED;
            case CANCELING -> DetailsEvent.Event.CANCELED; // not yet supported by 8.8 - will be available by 8.9
            case END -> DetailsEvent.Event.COMPLETED;
            default -> DetailsEvent.Event.UPDATED;
        };
        event.setEvent(eventType);
        event.setTimestamp(OffsetDateTime.now());
        event.setBpmnProcessId(job.getBpmnProcessId());
        event.setProcessDefinitionKey(job.getProcessDefinitionKey());
        event.setProcessInstanceKey(job.getProcessInstanceKey());
        event.setProcessDefinitionVersion(job.getProcessDefinitionVersion());
        event.setTenantId(job.getTenantId());
        event.setJobKey(job.getKey());
        event.setVariables(job.getVariablesAsMap());

        processEvent(
                job.getTenantId(),
                job.getBpmnProcessId(),
                job.getProcessDefinitionVersion(),
                job.getProcessInstanceKey(),
                camunda8WorkflowHandler -> camunda8WorkflowHandler.notify(event)
        );
    }

    @EventListener
    public void updateVersionInfos(
            final ModuleAwareBpmnDeployment.BpmnModelCacheProcessed event) {

        final var workflowHandlersOfWorkflowModule = workflowHandlers
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().getWorkflowModuleId().equals(event.getWorkflowModuleId()))
                .toList();
        event
                .getProcessedDeployed()
                .stream()
                .flatMap(process -> workflowHandlersOfWorkflowModule
                        .stream()
                        .filter(entry -> entry.getKey().getBpmnProcessId().equals(process.getKey()))
                        .map(entry -> Map.entry(entry, process)))
                .filter(ref -> ref.getKey().getKey().getVersionInfo().equals(
                        Camunda8DeploymentAdapter.VERSIONINFO_CURRENT))
                .forEach(ref -> {
                    final var connectable = ref.getKey().getKey();
                    final var handler = ref.getKey().getValue();
                    connectable.updateVersionInfo(ref.getValue().getValue());
                    handler.updateVersionInfo(ref.getValue().getValue());
                });

    }

}