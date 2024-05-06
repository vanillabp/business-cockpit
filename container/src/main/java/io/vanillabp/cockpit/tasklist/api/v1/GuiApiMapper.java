package io.vanillabp.cockpit.tasklist.api.v1;

import io.vanillabp.cockpit.commons.mapstruct.NoMappingMethod;
import io.vanillabp.cockpit.gui.api.v1.UserTask;
import io.vanillabp.cockpit.gui.api.v1.UserTasks;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.users.model.PersonAndGroupApiMapper;
import io.vanillabp.cockpit.util.microserviceproxy.MicroserviceProxyRegistry;
import java.time.OffsetDateTime;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

@Mapper(implementationName = "TasklistGuiApiMapperImpl")
public abstract class GuiApiMapper {

    private static final String PERSON_MAPPING = "personMapping";
    private static final String GROUP_MAPPING = "groupMapping";

    @Autowired
    private PersonAndGroupApiMapper personAndGroupMapper;

    @Named(PERSON_MAPPING)
    public io.vanillabp.cockpit.gui.api.v1.Person toPerson(
            final Person user) {
        return personAndGroupMapper.personToApiPerson(user);
    }

    @Named(GROUP_MAPPING)
    public io.vanillabp.cockpit.gui.api.v1.Group toGroup(
            final Group group) {
        return personAndGroupMapper.groupToApiGroup(group);
    }

    @Mapping(target = "uiUri", expression = "java(proxiedUiUri(userTask))")
    @Mapping(target = "dueDate", expression = "java(mapDateTimeMaxToNull(userTask))")
    @Mapping(target = "workflowModuleUri", expression = "java(proxiedWorkflowModuleUri(userTask))")
    @Mapping(target = "read", expression = "java(userTask.getReadAt(userId))")
    @Mapping(target = "assignee", source = "userTask.assignee", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "candidateUsers", source = "userTask.candidateUsers", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "candidateGroups", source = "userTask.candidateGroups", qualifiedByName = GROUP_MAPPING)
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

    @NoMappingMethod
    protected String proxiedUiUri(
            final io.vanillabp.cockpit.tasklist.model.UserTask userTask) {
        
        if (userTask.getWorkflowModuleId() == null) {
            return null;
        }
        if (userTask.getUiUriPath() == null) {
            return null;
        }
        
        return MicroserviceProxyRegistry.WORKFLOW_MODULES_PATH_PREFIX
                + userTask.getWorkflowModuleId()
                + (userTask.getUiUriPath().startsWith("/")
                        ? userTask.getUiUriPath()
                        : "/" + userTask.getUiUriPath());
        
    }

    @NoMappingMethod
    protected String proxiedWorkflowModuleUri(
            final io.vanillabp.cockpit.tasklist.model.UserTask userTask) {
        
        if (userTask.getWorkflowModuleId() == null) {
            return null;
        }
        
        return MicroserviceProxyRegistry.WORKFLOW_MODULES_PATH_PREFIX
                + userTask.getWorkflowModuleId();
        
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
