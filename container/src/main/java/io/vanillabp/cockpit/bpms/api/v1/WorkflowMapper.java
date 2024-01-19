package io.vanillabp.cockpit.bpms.api.v1;

import io.vanillabp.cockpit.workflowlist.model.Workflow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public abstract class WorkflowMapper {

    @Mapping(target = "id", source = "workflowId")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", source = "timestamp")
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "updatedBy", source = "initiator")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "targetRoles", ignore = true)
    @Mapping(target = "dangling", ignore = true)
    public abstract Workflow toNewWorkflow(WorkflowCreatedOrUpdatedEvent event);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "updatedBy", source = "initiator")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "targetRoles", ignore = true)
    @Mapping(target = "dangling", ignore = true)
    public abstract Workflow toUpdatedWorkflow(WorkflowCreatedOrUpdatedEvent event, @MappingTarget Workflow result);
    
}
