package io.vanillabp.cockpit.tasklist.api.v1;

import java.time.OffsetDateTime;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import io.vanillabp.cockpit.commons.mapstruct.NoMappingMethod;
import io.vanillabp.cockpit.gui.api.v1.UserTask;
import io.vanillabp.cockpit.util.microserviceproxy.MicroserviceProxyRegistry;

@Mapper
public abstract class GuiApiMapper {

    @Mapping(target = "uiUri", expression = "java(proxiedUiUri(userTask))")
    @Mapping(target = "dueDate", expression = "java(mapDateTimeMaxToNull(userTask))")
    @Mapping(target = "taskProviderUri", expression = "java(proxiedTaskProviderUri(userTask))")
	public abstract UserTask toApi(
			io.vanillabp.cockpit.tasklist.model.UserTask userTask);

    public abstract List<UserTask> toApi(
			List<io.vanillabp.cockpit.tasklist.model.UserTask> userTasks);
	
    @NoMappingMethod
	protected String proxiedUiUri(
	        final io.vanillabp.cockpit.tasklist.model.UserTask userTask) {
	    
        if (userTask.getWorkflowModuleUri() == null) {
            return null;
        }
	    if (userTask.getUiUriPath() == null) {
	        return null;
	    }
	    
	    return MicroserviceProxyRegistry.WORKFLOW_MODULES_PATH_PREFIX
	            + userTask.getWorkflowModule()
	            + (userTask.getUiUriPath().startsWith("/")
	                    ? userTask.getUiUriPath()
                        : "/" + userTask.getUiUriPath());
	    
	}

    @NoMappingMethod
    protected String proxiedTaskProviderUri(
            final io.vanillabp.cockpit.tasklist.model.UserTask userTask) {
        
        if (userTask.getWorkflowModuleUri() == null) {
            return null;
        }
        
        return MicroserviceProxyRegistry.WORKFLOW_MODULES_PATH_PREFIX
                + userTask.getWorkflowModule()
                + "/";
        
    }

    @NoMappingMethod
    protected OffsetDateTime mapDateTimeMaxToNull(
            final io.vanillabp.cockpit.tasklist.model.UserTask userTask) {
        if ((userTask.getDueDate() != null)
                && userTask.getDueDate().equals(OffsetDateTime.MAX)) {
            return null;
        }

        return userTask.getDueDate();
    }

}
