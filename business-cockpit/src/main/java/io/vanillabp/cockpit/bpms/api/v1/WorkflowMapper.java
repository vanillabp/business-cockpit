package io.vanillabp.cockpit.bpms.api.v1;

import io.vanillabp.cockpit.workflowlist.model.Workflow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface WorkflowMapper {

    @Mapping(target = "id", source = "workflowId")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", source = "timestamp")
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "updatedBy", source = "initiator")
    @Mapping(target = "endedAt", ignore = true)
    Workflow toNewWorkflow(WorkflowCreatedOrUpdatedEvent event);

}
