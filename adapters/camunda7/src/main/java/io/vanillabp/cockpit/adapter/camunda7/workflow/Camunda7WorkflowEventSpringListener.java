package io.vanillabp.cockpit.adapter.camunda7.workflow;

import java.util.HashMap;
import java.util.Map;

import io.vanillabp.cockpit.adapter.common.CockpitProperties;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.impl.history.event.HistoricProcessInstanceEventEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.util.StringUtils;

public class Camunda7WorkflowEventSpringListener {
    private static final Logger logger = LoggerFactory
            .getLogger(Camunda7WorkflowEventSpringListener.class);
    private final CockpitProperties cockpitProperties;
    private final Map<String,Camunda7WorkflowHandler> workflowHandlerMap = new HashMap<>();
    private final RepositoryService repositoryService;
    public Camunda7WorkflowEventSpringListener(
            final CockpitProperties cockpitProperties,
            final RepositoryService repositoryService) {
        this.cockpitProperties = cockpitProperties;
        this.repositoryService = repositoryService;
    }

    public void addWorkflowHandler(String forBpmnProcessId, Camunda7WorkflowHandler camunda7WorkflowHandler) {
        workflowHandlerMap.put(forBpmnProcessId, camunda7WorkflowHandler);
    }

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

        // Only operate on root process instances
        if (processInstanceEvent.getSuperProcessInstanceId() != null) {
            return;
        }

        Camunda7WorkflowHandler workflowHandler = workflowHandlerMap.get(processInstanceEvent.getProcessDefinitionKey());
        if (workflowHandler == null) {
            logger.trace("No workflow handler available for bpmnProcessId '{}'", processInstanceEvent.getProcessDefinitionId());
            return;
        }
        final String bpmnProcessName = processInstanceEvent.getProcessDefinitionKey();
        final var processDefinition = repositoryService.getProcessDefinition(processInstanceEvent.getProcessDefinitionId());
        final String bpmnProcessVersion;
        if (StringUtils.hasText(processDefinition.getVersionTag())) {
            bpmnProcessVersion = processDefinition.getVersionTag()
                    + ":"
                    + Integer.toString(processDefinition.getVersion());
        } else {
            bpmnProcessVersion = Integer.toString(processDefinition.getVersion());
        }

        workflowHandler.listenProcessInstanceEvent(
                processInstanceEvent, bpmnProcessName, bpmnProcessVersion);
    }

}
