package io.vanillabp.cockpit.adapter.common.workflowmodule.rest;

import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.cockpit.adapter.common.workflowmodule.WorkflowModulePublishing;
import io.vanillabp.cockpit.adapter.common.workflowmodule.WorkflowModulePublishingBase;
import io.vanillabp.cockpit.adapter.common.workflowmodule.events.RegisterWorkflowModuleEvent;
import io.vanillabp.cockpit.adapter.common.workflowmodule.events.WorkflowModuleEvent;
import io.vanillabp.cockpit.bpms.api.v1.BpmsApi;
import io.vanillabp.spi.cockpit.workflowmodules.WorkflowModuleDetailsProvider;
import io.vanillabp.springboot.adapter.VanillaBpProperties;
import jakarta.annotation.PostConstruct;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.lang.Nullable;

public class WorkflowModuleRestPublishing extends WorkflowModulePublishingBase implements WorkflowModulePublishing {

    private final Optional<BpmsApi> bpmsApi;

    private final WorkflowModuleRestMapper mapper;

    public WorkflowModuleRestPublishing(
            final String workerId,
            final Optional<BpmsApi> bpmsApi,
            final VanillaBpCockpitProperties properties,
            final ObjectProvider<List<WorkflowModuleDetailsProvider>> workflowModuleDetailsProviders,
            final WorkflowModuleRestMapper mapper) {

        super(workerId, properties, workflowModuleDetailsProviders);
        this.bpmsApi = bpmsApi;
        this.mapper = mapper;

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
            final WorkflowModuleEvent eventObject) {

        if (eventObject instanceof RegisterWorkflowModuleEvent registerWorkflowModuleEvent) {

            enrichRegisterWorkflowModuleEvent(registerWorkflowModuleEvent);
            final var event = mapper.map(registerWorkflowModuleEvent);
            bpmsApi.get().registerWorkflowModule(eventObject.getId(), event);

        } else {

            throw new RuntimeException(
                    "Unsupported event type '"
                    + eventObject.getClass().getName()
                    + "'!");

        }

    }

}
