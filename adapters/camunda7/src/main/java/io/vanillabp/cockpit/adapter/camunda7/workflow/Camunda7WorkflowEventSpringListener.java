package io.vanillabp.cockpit.adapter.camunda7.workflow;

import java.util.HashMap;
import java.util.Map;

import io.vanillabp.cockpit.adapter.camunda7.usertask.Camunda7UserTaskHandler;
import io.vanillabp.cockpit.adapter.camunda7.workflow.events.WorkflowCompleted;
import io.vanillabp.cockpit.adapter.camunda7.workflow.events.WorkflowCreated;
import io.vanillabp.cockpit.adapter.camunda7.workflow.events.WorkflowUpdated;
import io.vanillabp.cockpit.adapter.camunda7.workflow.publishing.ProcessWorkflowEvent;
import io.vanillabp.cockpit.adapter.camunda7.workflow.publishing.WorkflowEvent;
import io.vanillabp.cockpit.adapter.common.CockpitProperties;
import io.vanillabp.cockpit.adapter.common.workflow.EventWrapper;
import io.vanillabp.cockpit.bpms.api.v1.WorkflowCompletedEvent;
import io.vanillabp.cockpit.bpms.api.v1.WorkflowCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.commons.rest.adapter.versioning.ApiVersionAware;
import io.vanillabp.cockpit.commons.utils.DateTimeUtil;
import io.vanillabp.spi.cockpit.workflow.WorkflowDetails;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.impl.history.event.HistoricProcessInstanceEventEntity;
import org.camunda.bpm.engine.impl.history.event.HistoryEventTypes;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.util.StringUtils;

public class Camunda7WorkflowEventSpringListener {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final CockpitProperties cockpitProperties;
    private final ApiVersionAware bpmsApiVersionAware;
    private final RepositoryService repositoryService;

    public Camunda7WorkflowEventSpringListener(
            ApplicationEventPublisher applicationEventPublisher,
            CockpitProperties cockpitProperties,
            ApiVersionAware bpmsApiVersionAware,
            RepositoryService repositoryService) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.cockpitProperties = cockpitProperties;
        this.bpmsApiVersionAware = bpmsApiVersionAware;
        this.repositoryService = repositoryService;
    }

    private final Map<String, String> bpmnProcessIdToWorkflowModuleId = new HashMap<>();

    @EventListener(condition="#processInstanceEvent.eventType=='start'")
    public void listenProcessInstanceStartEvent(HistoricProcessInstanceEventEntity processInstanceEvent) {
        if (!cockpitProperties.isWorkflowListEnabled()) {
            return;
        }

        listenProcessInstanceEvent(processInstanceEvent);
    }

    @EventListener(condition="#processInstanceEvent.eventType=='end'")
    public void listenProcessInstanceEndEvent(HistoricProcessInstanceEventEntity processInstanceEvent) {
        if (!cockpitProperties.isWorkflowListEnabled()) {
            return;
        }

        listenProcessInstanceEvent(processInstanceEvent);
    }

    private void listenProcessInstanceEvent(HistoricProcessInstanceEventEntity processInstanceEvent) {

        final String workflowModuleId = processInstanceEvent.getTenantId();

        EventWrapper eventWrapper = null;
        if (processInstanceEvent.isEventOfType(HistoryEventTypes.PROCESS_INSTANCE_START)) {
            eventWrapper = new WorkflowCreated(
                    new WorkflowCreatedOrUpdatedEvent(),
                    workflowModuleId,
                    cockpitProperties.getI18nLanguages()
            );
        }

        if (processInstanceEvent.isEventOfType(HistoryEventTypes.PROCESS_INSTANCE_UPDATE)) {
            eventWrapper = new WorkflowUpdated(
                    new WorkflowCreatedOrUpdatedEvent(),
                    workflowModuleId,
                    cockpitProperties.getI18nLanguages()
            );
        }

        // migrate

        if (processInstanceEvent.isEventOfType(HistoryEventTypes.PROCESS_INSTANCE_END)) {
            eventWrapper = new WorkflowCompleted(
                    new WorkflowCompletedEvent()
            );
        }
        if (eventWrapper == null) {
            throw new RuntimeException(
                    "Unsupported process instance event type '"
                            + processInstanceEvent.getEventType()
                            + "'!");
        }

        if (eventWrapper instanceof WorkflowCreated) {

            final var workflowCreatedEvent = (WorkflowCreated) eventWrapper;
            final ProcessDefinition processDefinition = repositoryService.getProcessDefinition(
                    processInstanceEvent.getProcessDefinitionId());
            final var bpmnProcessId = processInstanceEvent.getProcessDefinitionKey();

            final String bpmnProcessVersionTag = processDefinition.getVersionTag();
            final String bpmnProcessVersion = Integer.toString(processDefinition.getVersion());
            final var bpmnProcessName = StringUtils.hasText(processDefinition.getName())
                    ? processDefinition.getName()
                    : processDefinition.getKey();

            final var prefilledWorkflowDetails = prefillWorkflowDetails(
                    bpmnProcessId,
                    bpmnProcessVersionTag,
                    bpmnProcessVersion,
                    bpmnProcessName,
                    processInstanceEvent,
                    workflowCreatedEvent);

            // callWorkflowDetailsProviderMethod

            fillWorkflowDetailsByCustomDetails(workflowCreatedEvent, prefilledWorkflowDetails);

        } else {

            fillLifecycleEvent(processInstanceEvent, eventWrapper);

        }

        applicationEventPublisher.publishEvent(
                new WorkflowEvent(
                        Camunda7UserTaskHandler.class,
                        eventWrapper.getEvent(),
                        bpmsApiVersionAware.getApiVersion()));
        applicationEventPublisher.publishEvent(
                new ProcessWorkflowEvent(
                        Camunda7WorkflowEventSpringListener.class));

    }

    private void fillWorkflowDetailsByCustomDetails(
            WorkflowCreated event,
            WorkflowDetails details
    ) {
        event.setWorkflowTitle(details.getWorkflowTitle());
        event.setTitle(details.getTitle());
    }


    private WorkflowCreated prefillWorkflowDetails(
            final String bpmnProcessId,
            final String bpmnProcessVersionTag,
            final String bpmnProcessVersion,
            final String bpmnProcessName,
            final HistoricProcessInstanceEventEntity processInstanceEvent,
            final WorkflowCreated event) {

        final var prefilledWorkflowDetails = event;

        final var language = "de";

        prefilledWorkflowDetails.setId(
                System.nanoTime()
                        + "@"
                        + processInstanceEvent.getProcessInstanceId()
                        + "#"
                        + processInstanceEvent.getId());
        prefilledWorkflowDetails.setTimestamp(
                DateTimeUtil.fromDate(processInstanceEvent.getStartTime()));
        prefilledWorkflowDetails.setBpmnProcessId(bpmnProcessId);
        prefilledWorkflowDetails.setBpmnProcessVersionTag(bpmnProcessVersionTag);
        prefilledWorkflowDetails.setBpmnProcessVersion(bpmnProcessVersion);
        prefilledWorkflowDetails.setWorkflowId(processInstanceEvent.getProcessInstanceId());
        prefilledWorkflowDetails.setWorkflowAggregateId(processInstanceEvent.getBusinessKey());
        prefilledWorkflowDetails.setTitle(Map.of(language, bpmnProcessName));
        prefilledWorkflowDetails.setWorkflowTitle(Map.of(language, bpmnProcessName));

        return prefilledWorkflowDetails;

    }

    private void fillLifecycleEvent(
            final HistoricProcessInstanceEventEntity processInstanceEvent,
            final EventWrapper event) {

        event.setId(
                System.nanoTime()
                        + "@"
                        + processInstanceEvent.getProcessInstanceId()
                        + "#"
                        + processInstanceEvent.getId());
        event.setComment(
                processInstanceEvent.getDeleteReason());
        event.setTimestamp(
                // TODO GWI
                DateTimeUtil.fromDate(processInstanceEvent.getStartTime()));

    }

}
