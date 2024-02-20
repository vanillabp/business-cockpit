package io.vanillabp.cockpit.tasklist.api.v1;

import io.vanillabp.cockpit.commons.mapstruct.NoMappingMethod;
import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.gui.api.v1.Sex;
import io.vanillabp.cockpit.gui.api.v1.User;
import io.vanillabp.cockpit.gui.api.v1.UserStatus;
import io.vanillabp.cockpit.gui.api.v1.UserTask;
import io.vanillabp.cockpit.gui.api.v1.UserTasks;
import io.vanillabp.cockpit.util.microserviceproxy.MicroserviceProxyRegistry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

import java.time.OffsetDateTime;
import java.util.List;

@Mapper(implementationName = "TasklistGuiApiMapperImpl")
public abstract class GuiApiMapper {

    @Mapping(target = "uiUri", expression = "java(proxiedUiUri(userTask))")
    @Mapping(target = "dueDate", expression = "java(mapDateTimeMaxToNull(userTask))")
    @Mapping(target = "workflowModuleUri", expression = "java(proxiedWorkflowModuleUri(userTask))")
    @Mapping(target = "read", expression = "java(userTask.getReadAt(userId))")
	public abstract UserTask toApi(
			io.vanillabp.cockpit.tasklist.model.UserTask userTask,
            String userId);

    public List<UserTask> toApi(
			List<io.vanillabp.cockpit.tasklist.model.UserTask> userTasks,
            String userId) {
        if (userTasks == null) {
            return null;
        }
        return userTasks
                .stream()
                .map(userTask -> toApi(userTask, userId))
                .toList();
    }

    @Mapping(target = "page.number", source = "data.number")
    @Mapping(target = "page.size", source = "data.size")
    @Mapping(target = "page.totalPages", source = "data.totalPages")
    @Mapping(target = "page.totalElements", source = "data.totalElements")
    @Mapping(target = "userTasks", expression = "java(toApi(data.getContent(), userId))")
    @Mapping(target = "serverTimestamp", source = "timestamp")
    public abstract UserTasks toApi(Page<io.vanillabp.cockpit.tasklist.model.UserTask> data,
                                    OffsetDateTime timestamp,
                                    String userId);

    @Mapping(target = "avatar", ignore = true)
    @Mapping(target = "sex", expression = "java(sexToApi(user))")
    @Mapping(target = "status", expression = "java(statusToApi(user))")
    @Mapping(target = "roles", source = "authorities")
    public abstract User toApi(UserDetails user);

    public UserStatus statusToApi(UserDetails userDetails) {

        if (userDetails == null) {
            return null;
        }
        if (userDetails.isActive()) {
            return UserStatus.ACTIVE;
        }
        return UserStatus.INACTIVE;

    }

    public Sex sexToApi(UserDetails userDetails) {

        if (userDetails == null) {
            return null;
        }
        if (userDetails.isFemale() == null) {
            return Sex.OTHER;
        } else if (userDetails.isFemale()) {
            return Sex.FEMALE;
        }
        return Sex.MALE;

    }

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
    protected String proxiedWorkflowModuleUri(
            final io.vanillabp.cockpit.tasklist.model.UserTask userTask) {
        
        if (userTask.getWorkflowModuleUri() == null) {
            return null;
        }
        
        return MicroserviceProxyRegistry.WORKFLOW_MODULES_PATH_PREFIX
                + userTask.getWorkflowModule();
        
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
