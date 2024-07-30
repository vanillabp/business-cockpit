package io.vanillabp.cockpit.simulator.usertask;

import io.vanillabp.cockpit.bpms.api.v1.UserTaskCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.commons.mapstruct.NoMappingMethod;
import io.vanillabp.cockpit.gui.api.v1.UserTask;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public abstract class OfficialTasklistApiMapper {

    @Mapping(target = "createdAt", expression = "java(java.time.OffsetDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.OffsetDateTime.now())")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "version", constant = "1")
    @Mapping(target = "uiUri", expression = "java(proxiedUiUri(event))")
    @Mapping(target = "workflowModuleUri", expression = "java(proxiedWorkflowModuleUri(event))")
    @Mapping(target = "read", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "candidateUsers", ignore = true)
    @Mapping(target = "candidateGroups", ignore = true)
    public abstract UserTask toApi(UserTaskCreatedOrUpdatedEvent event);
    
    @NoMappingMethod
    protected String proxiedUiUri(
            final UserTaskCreatedOrUpdatedEvent userTask) {
        
        if (userTask.getWorkflowModuleId() == null) {
            return null;
        }
        if (userTask.getUiUriPath() == null) {
            return null;
        }
        
        return userTask.getUiUriPath().startsWith("/")
                    ? userTask.getUiUriPath()
                    : "/" + userTask.getUiUriPath();
        
    }

    @NoMappingMethod
    protected String proxiedWorkflowModuleUri(
            final UserTaskCreatedOrUpdatedEvent userTask) {
        
        if (userTask.getWorkflowModuleId() == null) {
            return null;
        }
        
        return "/" + userTask.getWorkflowModuleId() + "/";
        
    }
    
}
