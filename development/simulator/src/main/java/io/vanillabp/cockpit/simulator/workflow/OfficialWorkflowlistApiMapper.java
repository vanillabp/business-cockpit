package io.vanillabp.cockpit.simulator.workflow;

import io.vanillabp.cockpit.bpms.api.v1_1.WorkflowCreatedEvent;
import io.vanillabp.cockpit.commons.mapstruct.NoMappingMethod;
import io.vanillabp.cockpit.gui.api.v1.Workflow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public abstract class OfficialWorkflowlistApiMapper {

    @Mapping(target = "createdAt", expression = "java(java.time.OffsetDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.OffsetDateTime.now())")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "version", constant = "1")
    @Mapping(target = "uiUri", expression = "java(proxiedUiUri(event))")
    @Mapping(target = "workflowModuleUri", expression = "java(proxiedWorkflowModuleUri(event))")
    @Mapping(target = "accessibleToUsers", ignore = true)  // TODO
    @Mapping(target = "accessibleToGroups", ignore = true) // TODO
    @Mapping(target = "initiator", ignore = true)          // TODO
    public abstract Workflow toApi(WorkflowCreatedEvent event);
    
    @NoMappingMethod
    protected String proxiedUiUri(
            final WorkflowCreatedEvent workflow) {
        
        if (workflow.getWorkflowModuleId() == null) {
            return null;
        }
        if (workflow.getUiUriPath() == null) {
            return null;
        }
        
        return workflow.getUiUriPath().startsWith("/")
                ? workflow.getUiUriPath()
                : "/" + workflow.getUiUriPath();
        
    }
    
    @NoMappingMethod
    protected String proxiedWorkflowModuleUri(
            final WorkflowCreatedEvent workflow) {
        
        if (workflow.getWorkflowModuleId() == null) {
            return null;
        }
        
        return "/" + workflow.getWorkflowModuleId() + "/";
        
    }
    
}
