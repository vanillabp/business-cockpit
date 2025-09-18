package io.vanillabp.cockpit.adapter.camunda8.usertask;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.search.enums.JobKind;
import io.camunda.client.api.worker.JobClient;
import io.camunda.client.api.worker.JobHandler;
import io.vanillabp.cockpit.adapter.camunda8.deployments.Camunda8DeploymentAdapter;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8UserTaskCreatedEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8UserTaskEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8UserTaskLifecycleEvent;
import io.vanillabp.cockpit.adapter.camunda8.wiring.Camunda8UserTaskConnectable;
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
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class Camunda8UserTaskEventHandler implements JobHandler {

    private static final Logger logger = LoggerFactory
            .getLogger(Camunda8UserTaskEventHandler.class);
    
    private final Map<Camunda8UserTaskConnectable, Camunda8UserTaskHandler> taskHandlers;

    private final Set<String> knownTenantIds = new HashSet<>();

    private CamundaClient client;

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

    @EventListener
    public void updateVersionInfos(
            final ModuleAwareBpmnDeployment.BpmnModelCacheProcessed event) {

        final var taskHandlersOfWorkflowModule = taskHandlers
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().getWorkflowModuleId().equals(event.getWorkflowModuleId()))
                .toList();
        event
                .getProcessedDeployed()
                .stream()
                .flatMap(process -> taskHandlersOfWorkflowModule
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

    @Override
    public void handle(
            final JobClient client,
            final ActivatedJob job) throws Exception {

        if (job.getKind() != JobKind.TASK_LISTENER) {
            logger.warn("Received job of type '{}' which is not a task listener! Will ignore it.", job.getKind());
            client.newCompleteCommand(job).send().join();
            return;
        }
        if (job.getUserTask() == null) {
            logger.warn("Received job of type '{}' which is not a task task listener! Will ignore it.", job.getKind());
            client.newCompleteCommand(job).send().join();
            return;
        }

        final var event = new Camunda8UserTaskEvent();
        final var eventType = switch (job.getListenerEventType()) {
            case CREATING -> DetailsEvent.Event.CREATED;
            case CANCELING -> DetailsEvent.Event.CANCELED;
            case COMPLETING -> DetailsEvent.Event.COMPLETED;
            default -> DetailsEvent.Event.UPDATED;
        };
        event.setEvent(eventType);
        event.setTimestamp(OffsetDateTime.now());
        event.setBpmnProcessId(job.getBpmnProcessId());
        event.setElementId(job.getElementId());
        event.setProcessDefinitionKey(job.getProcessDefinitionKey());
        event.setProcessInstanceKey(job.getProcessInstanceKey());
        event.setProcessDefinitionVersion(job.getProcessDefinitionVersion());
        event.setTenantId(job.getTenantId());
        event.setJobKey(job.getKey());
        event.setUserTaskKey(job.getUserTask().getUserTaskKey());
        // TODO
        //event.setFollowUpDate(job.getUserTask().getFollowUpDate());
        event.setAssignee(job.getUserTask().getAssignee());
        event.setCandidateUsers(job.getUserTask().getCandidateUsers());
        event.setCandidateGroups(job.getUserTask().getCandidateGroups());
        // TODO
        //event.setDueDate(job.getUserTask().getDueDate());
        event.setVariables(job.getVariablesAsMap());

        final var taskDefinition = job.getType().startsWith(Camunda8UserTaskWiring.TASKDEFINITION_USERTASK_DETAILSPROVIDER)
                ? job.getType().substring(Camunda8UserTaskWiring.TASKDEFINITION_USERTASK_DETAILSPROVIDER.length())
                : job.getType();
        event.setTaskDefinition(taskDefinition);

        notify(
                job.getTenantId(),
                job.getElementId(),
                job.getBpmnProcessId(),
                job.getProcessDefinitionVersion(),
                job.getProcessInstanceKey(),
                taskDefinition,
                camunda8UserTaskHandler -> camunda8UserTaskHandler.notify(event)
        );
    }

    private String mapTenantId(String tenantId) {
        return tenantId == null ? "<default>" : tenantId;
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
                .filter(entry -> mapTenantId(entry.getKey().getTenantId()).equals(mappedTenantId))
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
