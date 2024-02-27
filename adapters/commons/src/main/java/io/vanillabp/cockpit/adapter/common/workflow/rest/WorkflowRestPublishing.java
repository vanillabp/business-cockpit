package io.vanillabp.cockpit.adapter.common.workflow.rest;

import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.cockpit.adapter.common.workflow.WorkflowPublishing;
import io.vanillabp.cockpit.adapter.common.workflow.WorkflowPublishingBase;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCreatedEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.v1.BpmsApi;
import io.vanillabp.springboot.adapter.VanillaBpProperties;
import jakarta.annotation.PostConstruct;

import java.util.Optional;

public class WorkflowRestPublishing extends WorkflowPublishingBase implements WorkflowPublishing {

    private final Optional<BpmsApi> bpmsApi;

    private final WorkflowRestMapper workflowMapper;

    public WorkflowRestPublishing(
            final String workerId,
            final Optional<BpmsApi> bpmsApi,
            final VanillaBpCockpitProperties properties,
            final WorkflowRestMapper workflowMapper) {
        super(workerId, properties);
        this.bpmsApi = bpmsApi;
        this.workflowMapper = workflowMapper;
    }

    @PostConstruct
    @javax.annotation.PostConstruct
    public void validateAutowiring() {
        if (bpmsApi.isPresent()) {
            return;
        }
        
        throw new RuntimeException("You have to configure either '"
                + VanillaBpProperties.PREFIX
                + ".cockpit.rest' or '"
                + VanillaBpProperties.PREFIX
                + ".cockpit.kafka' to define were to send user task events to!");
    }

    @Override
    public void publish(
            final WorkflowEvent eventObject) {
        if(eventObject instanceof WorkflowUpdatedEvent workflowUpdatedEvent){

            editWorkflowCreatedOrUpdatedEvent(workflowUpdatedEvent);
            final var event = workflowMapper.map(workflowUpdatedEvent);
            bpmsApi.get().workflowUpdatedEvent(event.getWorkflowId(), event);

        } else if(eventObject instanceof WorkflowCreatedEvent workflowCreatedEvent){

            editWorkflowCreatedOrUpdatedEvent(workflowCreatedEvent);
            final var event = workflowMapper.map(workflowCreatedEvent);
            bpmsApi.get().workflowCreatedEvent(event);

        } else if(eventObject instanceof io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCancelledEvent workflowCancelledEvent){

            final var event = workflowMapper.map(workflowCancelledEvent);
            bpmsApi.get().workflowCancelledEvent(event.getWorkflowId(), event);

        } else if(eventObject instanceof io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCompletedEvent workflowCompletedEvent) {

            final var event = workflowMapper.map(workflowCompletedEvent);
            bpmsApi.get().workflowCompletedEvent(event.getWorkflowId(), event);
        }
        // else if suspended
        // else if activated
        else {
            throw new RuntimeException(
                    "Unsupported event type '"
                    + eventObject.getClass().getName()
                    + "'!");
        }
    }

}
