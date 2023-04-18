package io.vanillabp.cockpit.tasklist.api.v1;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import io.vanillabp.cockpit.commons.mapstruct.NoMappingMethod;
import io.vanillabp.cockpit.gui.api.v1.UserTask;
import io.vanillabp.cockpit.util.microserviceproxy.MicroserviceProxyRegistry;

@Mapper
public abstract class GuiApiMapper {

    @Mapping(target = "url", expression = "java(urlToApiUsingMicroserviceProxy(userTask.getUrl()))")
	public abstract UserTask toApi(
			io.vanillabp.cockpit.tasklist.model.UserTask userTask);

    public abstract List<UserTask> toApi(
			List<io.vanillabp.cockpit.tasklist.model.UserTask> userTasks);
	
    @NoMappingMethod
	protected String urlToApiUsingMicroserviceProxy(
	        final String url) {
	    
	    if (url == null) {
	        return null;
	    }
	    
	    var indexOfPath = -1;
	    if (url.startsWith("http")) {
	        indexOfPath = url.indexOf("//");
	        if (indexOfPath != -1) {
	            indexOfPath += 2;
	        }
	        indexOfPath = url.indexOf('/', indexOfPath);
            if (indexOfPath != -1) {
                indexOfPath += 1;
            }
	    } else {
            indexOfPath = url.indexOf('/');
            if (indexOfPath != -1) {
                indexOfPath += 1;
            }
	    }

	    if (indexOfPath == -1) {
	        return MicroserviceProxyRegistry.WORKFLOW_MODULES_PATH_PREFIX;
	    }
	    
	    return MicroserviceProxyRegistry.WORKFLOW_MODULES_PATH_PREFIX
	            + url.substring(indexOfPath);
	    
	}
	
}
