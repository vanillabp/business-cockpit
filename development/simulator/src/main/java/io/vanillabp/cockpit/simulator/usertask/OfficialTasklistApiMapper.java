package io.vanillabp.cockpit.simulator.usertask;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import io.vanillabp.cockpit.bpms.api.v1.UserTaskCreatedEvent;
import io.vanillabp.cockpit.commons.mapstruct.NoMappingMethod;
import io.vanillabp.cockpit.gui.api.v1.UserTask;

@Mapper
public abstract class OfficialTasklistApiMapper {

    @Mapping(target = "createdAt", expression = "java(java.time.OffsetDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.OffsetDateTime.now())")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "version", constant = "1")
    @Mapping(target = "uiUri", expression = "java(proxiedUiUri(event))")
    @Mapping(target = "taskProviderUri", expression = "java(proxiedTaskProviderUri(event))")
    public abstract UserTask toApi(UserTaskCreatedEvent event);
    
    @NoMappingMethod
    protected String proxiedUiUri(
            final UserTaskCreatedEvent userTask) {
        
        if (userTask.getWorkflowModuleUri() == null) {
            return null;
        }
        if (userTask.getUiUriPath() == null) {
            return null;
        }
        
        return userTask.getWorkflowModuleUri()
                + (userTask.getUiUriPath().startsWith("/")
                        ? userTask.getUiUriPath()
                        : "/" + userTask.getUiUriPath());
        
    }

    @NoMappingMethod
    protected String proxiedTaskProviderUri(
            final UserTaskCreatedEvent userTask) {
        
        if (userTask.getWorkflowModuleUri() == null) {
            return null;
        }
        
        return userTask.getWorkflowModuleUri()
                + "/";
        
    }
    
}
