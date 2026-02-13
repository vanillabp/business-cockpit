package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import io.vanillabp.cockpit.bpms.api.v1.RegisterWorkflowModuleEvent;
import io.vanillabp.cockpit.bpms.api.v1.UserTaskCancelledEvent;
import io.vanillabp.cockpit.bpms.api.v1.UserTaskCompletedEvent;
import io.vanillabp.cockpit.bpms.api.v1.UserTaskCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.v1.WorkflowCancelledEvent;
import io.vanillabp.cockpit.bpms.api.v1.WorkflowCompletedEvent;
import io.vanillabp.cockpit.bpms.api.v1.WorkflowCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.v1_1.UserTaskCreatedEvent;
import io.vanillabp.cockpit.bpms.api.v1_1.UserTaskUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.v1_1.WorkflowCreatedEvent;
import io.vanillabp.cockpit.bpms.api.v1_1.WorkflowUpdatedEvent;
import io.vanillabp.cockpit.commons.mapstruct.NoMappingMethod;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Page;
import io.vanillabp.cockpit.gui.api.v1.Group;
import io.vanillabp.cockpit.gui.api.v1.Person;
import io.vanillabp.cockpit.gui.api.v1.UserTaskRetrieveMode;
import io.vanillabp.cockpit.gui.api.v1.UserTasks;
import io.vanillabp.cockpit.gui.api.v1.WorkflowRetrieveMode;
import io.vanillabp.cockpit.gui.api.v1.Workflows;
import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper
public abstract class OfficialApiMapper {

    @Mapping(target = "id", expression = "java(id)")
    @Mapping(target = "version", ignore = true)
    public abstract io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.WorkflowModule toModel(RegisterWorkflowModuleEvent event, String id);

    @Mapping(target = "id", expression = "java(id)")
    @Mapping(target = "version", ignore = true)
    public abstract io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.WorkflowModule toModel(io.vanillabp.cockpit.bpms.api.v1_1.RegisterWorkflowModuleEvent event, String id);

    @Named("group")
    @Mapping(target = "id", expression = "java(group)")
    @Mapping(target = "display", expression = "java(group)")
    @Mapping(target = "details", ignore = true)
    public abstract io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Group toGroupModel(String group);

    public TaskService.RetrieveMode toModel(UserTaskRetrieveMode retrieveMode) {
        if (retrieveMode == null) {
            return TaskService.RetrieveMode.ALL;
        }
        return TaskService.RetrieveMode.valueOf(retrieveMode.name());
    }

    public WorkflowService.RetrieveMode toModel(WorkflowRetrieveMode retrieveMode) {
        if (retrieveMode == null) {
            return WorkflowService.RetrieveMode.ALL;
        }
        return WorkflowService.RetrieveMode.valueOf(retrieveMode.name());
    }

    @Named("apiGroup")
    @Mapping(target = "id", expression = "java(group)")
    @Mapping(target = "display", expression = "java(group)")
    @Mapping(target = "details", ignore = true)
    public abstract Group toGroupApi(String group);

    @Named("apiGroups")
    protected List<Group> toGroupsApi(List<String> groups) {

        if (groups == null) {
            return null;
        }
        return groups
                .stream()
                .map(this::toGroupApi)
                .collect(Collectors.toList());

    }

    @Named("apiPerson")
    @Mapping(target = "id", expression = "java(person)")
    @Mapping(target = "display", expression = "java(person)")
    @Mapping(target = "displayShort", expression = "java(person)")
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "avatar", ignore = true)
    @Mapping(target = "details", ignore = true)
    public abstract Person toPersonApi(String person);

    @Named("apiPersons")
    protected List<Person> toPersonsApi(List<String> persons) {

        if (persons == null) {
            return null;
        }
        return persons
                .stream()
                .map(this::toPersonApi)
                .collect(Collectors.toList());

    }

    @Named("groups")
    protected List<io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Group> toGroupsModel(List<String> groups) {

        if (groups == null) {
            return null;
        }
        return groups
                .stream()
                .map(this::toGroupModel)
                .collect(Collectors.toList());

    }

    @Named("person")
    @Mapping(target = "id", expression = "java(person)")
    @Mapping(target = "display", expression = "java(person)")
    @Mapping(target = "displayShort", expression = "java(person)")
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "avatar", ignore = true)
    @Mapping(target = "details", ignore = true)
    public abstract io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Person toPersonModel(String person);

    @Named("persons")
    protected List<io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Person> toPersonModel(List<String> persons) {

        if (persons == null) {
            return null;
        }
        return persons
                .stream()
                .map(this::toPersonModel)
                .collect(Collectors.toList());

    }

    @NoMappingMethod
    protected String proxiedUiUri(
            final String workflowModuleId,
            final String uiUriPath) {

        if (workflowModuleId == null) {
            return null;
        }
        if (uiUriPath == null) {
            return null;
        }

        return "/wm/"
                + workflowModuleId
                + (uiUriPath.startsWith("/") ? uiUriPath : "/" + uiUriPath);

    }

    @NoMappingMethod
    protected String proxiedWorkflowModuleUri(
            final String workflowModuleId) {

        if (workflowModuleId == null) {
            return null;
        }

        return "/wm/" + workflowModuleId;

    }

    public abstract io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Person toModel(Person person);

    public abstract io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Group toGroup(Group group);

    @Mapping(target = "id", source = "workflowId")
    @Mapping(target = "createdAt", source = "timestamp")
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "initiator", qualifiedByName = "person")
    @Mapping(target = "accessibleToUsers", qualifiedByName = "persons")
    @Mapping(target = "accessibleToGroups", qualifiedByName = "groups")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "uiUri", expression = "java(proxiedUiUri(event.getWorkflowModuleId(), event.getUiUriPath()))")
    @Mapping(target = "workflowModuleUri", expression = "java(proxiedWorkflowModuleUri(event.getWorkflowModuleId()))")
    public abstract io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Workflow toModel(WorkflowCreatedOrUpdatedEvent event);

    @Mapping(target = "id", source = "workflowId")
    @Mapping(target = "createdAt", source = "timestamp")
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "initiator", qualifiedByName = "person")
    @Mapping(target = "accessibleToUsers", qualifiedByName = "persons")
    @Mapping(target = "accessibleToGroups", qualifiedByName = "groups")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "uiUri", expression = "java(proxiedUiUri(event.getWorkflowModuleId(), event.getUiUriPath()))")
    @Mapping(target = "workflowModuleUri", expression = "java(proxiedWorkflowModuleUri(event.getWorkflowModuleId()))")
    public abstract io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Workflow toModel(WorkflowCreatedEvent event);

    @Mapping(target = "requestId", ignore = true)
    @Mapping(target = "serverTimestamp", expression = "java(java.time.OffsetDateTime.now())")
    @Mapping(target = "workflows", source = "pageObjects")
    @Mapping(target = "page", expression = "java(toApiWorkflowPage(workflowPage))")
    public abstract Workflows toWorkflowsApi(Page<io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Workflow> workflowPage);

    public abstract io.vanillabp.cockpit.gui.api.v1.Page toApiWorkflowPage(Page<io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Workflow> workflowPage);

    public abstract io.vanillabp.cockpit.gui.api.v1.Workflow toApi(io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Workflow workflow);

    @Mapping(target = "id", source = "workflowId")
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "initiator", qualifiedByName = "person")
    @Mapping(target = "accessibleToUsers", qualifiedByName = "persons")
    @Mapping(target = "accessibleToGroups", qualifiedByName = "groups")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "uiUri", ignore = true)
    @Mapping(target = "workflowModuleUri", ignore = true)
    public abstract void ontoModel(@MappingTarget io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Workflow workflow, WorkflowCreatedOrUpdatedEvent event);

    @Mapping(target = "id", source = "workflowId")
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "initiator", qualifiedByName = "person")
    @Mapping(target = "accessibleToUsers", qualifiedByName = "persons")
    @Mapping(target = "accessibleToGroups", qualifiedByName = "groups")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "uiUri", ignore = true)
    @Mapping(target = "workflowModuleUri", ignore = true)
    public abstract void ontoModel(@MappingTarget io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Workflow workflow, WorkflowUpdatedEvent event);

    @Mapping(target = "id", source = "workflowId")
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "endedAt", source = "timestamp")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "initiator", qualifiedByName = "person")
    @Mapping(target = "uiUri", ignore = true)
    @Mapping(target = "workflowModuleUri", ignore = true)
    @Mapping(target = "accessibleToUsers", ignore = true)
    @Mapping(target = "accessibleToGroups", ignore = true)
    @Mapping(target = "details", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "workflowModuleId", ignore = true)
    @Mapping(target = "businessId", ignore = true)
    @Mapping(target = "title", ignore = true)
    @Mapping(target = "uiUriType", ignore = true)
    @Mapping(target = "detailsFulltextSearch", ignore = true)
    public abstract void ontoModel(@MappingTarget io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Workflow workflow, WorkflowCompletedEvent event);

    @Mapping(target = "id", source = "workflowId")
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "endedAt", source = "timestamp")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "initiator", qualifiedByName = "apiPerson")
    @Mapping(target = "uiUri", ignore = true)
    @Mapping(target = "workflowModuleUri", ignore = true)
    @Mapping(target = "accessibleToUsers", ignore = true)
    @Mapping(target = "accessibleToGroups", ignore = true)
    @Mapping(target = "details", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "workflowModuleId", ignore = true)
    @Mapping(target = "businessId", ignore = true)
    @Mapping(target = "title", ignore = true)
    @Mapping(target = "uiUriType", ignore = true)
    @Mapping(target = "detailsFulltextSearch", ignore = true)
    public abstract void ontoModel(@MappingTarget io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Workflow workflow, WorkflowCancelledEvent event);

    @Mapping(target = "id", source = "workflowId")
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "endedAt", source = "timestamp")
    @Mapping(target = "initiator", qualifiedByName = "apiPerson")
    @Mapping(target = "accessibleToUsers", ignore = true)
    @Mapping(target = "accessibleToGroups", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "uiUri", ignore = true)
    @Mapping(target = "workflowModuleUri", ignore = true)
    public abstract void ontoModel(@MappingTarget io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Workflow workflow, io.vanillabp.cockpit.bpms.api.v1_1.WorkflowCancelledEvent event);

    @Mapping(target = "id", source = "workflowId")
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "endedAt", source = "timestamp")
    @Mapping(target = "initiator", qualifiedByName = "apiPerson")
    @Mapping(target = "accessibleToUsers", ignore = true)
    @Mapping(target = "accessibleToGroups", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "uiUri", ignore = true)
    @Mapping(target = "workflowModuleUri", ignore = true)
    public abstract void ontoModel(@MappingTarget io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Workflow workflow, io.vanillabp.cockpit.bpms.api.v1_1.WorkflowCompletedEvent event);

    @Mapping(target = "id", source = "userTaskId")
    @Mapping(target = "createdAt", source = "timestamp")
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "assignee", qualifiedByName = "apiPerson")
    @Mapping(target = "candidateUsers", qualifiedByName = "apiPersons")
    @Mapping(target = "candidateGroups", qualifiedByName = "apiGroups")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "uiUri", expression = "java(proxiedUiUri(event.getWorkflowModuleId(), event.getUiUriPath()))")
    @Mapping(target = "workflowModuleUri", expression = "java(proxiedWorkflowModuleUri(event.getWorkflowModuleId()))")
    @Mapping(target = "read", ignore = true)
    public abstract io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTask toModel(UserTaskCreatedOrUpdatedEvent event);

    @Mapping(target = "id", source = "event.userTaskId")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", source = "event.timestamp")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "assignee", qualifiedByName = "apiPerson")
    @Mapping(target = "candidateUsers", qualifiedByName = "apiPersons")
    @Mapping(target = "candidateGroups", qualifiedByName = "apiGroups")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "uiUri", ignore = true)
    @Mapping(target = "workflowModuleUri", ignore = true)
    @Mapping(target = "read", ignore = true)
    public abstract void ontoModel(@MappingTarget io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTask userTask, UserTaskCreatedOrUpdatedEvent event);

    @Mapping(target = "id", source = "event.userTaskId")
    @Mapping(target = "createdAt", source = "event.timestamp")
    @Mapping(target = "updatedAt", source = "event.timestamp")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "assignee", qualifiedByName = "apiPerson")
    @Mapping(target = "candidateUsers", qualifiedByName = "apiPersons")
    @Mapping(target = "candidateGroups", qualifiedByName = "apiGroups")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "uiUri", expression = "java(proxiedUiUri(event.getWorkflowModuleId(), event.getUiUriPath()))")
    @Mapping(target = "workflowModuleUri", expression = "java(proxiedWorkflowModuleUri(event.getWorkflowModuleId()))")
    @Mapping(target = "read", ignore = true)
    public abstract io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTask toModel(UserTaskCreatedEvent event);

    @Mapping(target = "id", source = "event.userTaskId")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", source = "event.timestamp")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "assignee", qualifiedByName = "apiPerson")
    @Mapping(target = "candidateUsers", qualifiedByName = "apiPersons")
    @Mapping(target = "candidateGroups", qualifiedByName = "apiGroups")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "uiUri", ignore = true)
    @Mapping(target = "workflowModuleUri", ignore = true)
    @Mapping(target = "read", ignore = true)
    public abstract void ontoModel(@MappingTarget io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTask userTask, UserTaskUpdatedEvent event);

    @Mapping(target = "id", source = "event.userTaskId")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", source = "event.timestamp")
    @Mapping(target = "endedAt", source = "event.timestamp")
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "candidateUsers", ignore = true)
    @Mapping(target = "candidateGroups", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "uiUri", ignore = true)
    @Mapping(target = "workflowModuleUri", ignore = true)
    @Mapping(target = "read", ignore = true)
    public abstract void ontoModel(@MappingTarget io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTask userTask, io.vanillabp.cockpit.bpms.api.v1_1.UserTaskCompletedEvent event);

    @Mapping(target = "id", source = "event.userTaskId")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", source = "event.timestamp")
    @Mapping(target = "endedAt", source = "event.timestamp")
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "candidateUsers", ignore = true)
    @Mapping(target = "candidateGroups", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "uiUri", ignore = true)
    @Mapping(target = "workflowModuleUri", ignore = true)
    @Mapping(target = "read", ignore = true)
    public abstract void ontoModel(@MappingTarget io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTask userTask, io.vanillabp.cockpit.bpms.api.v1_1.UserTaskCancelledEvent event);

    @Mapping(target = "id", source = "userTaskId")
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "endedAt", source = "timestamp")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "uiUri", ignore = true)
    @Mapping(target = "workflowModuleUri", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "candidateUsers", ignore = true)
    @Mapping(target = "candidateGroups", ignore = true)
    @Mapping(target = "details", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "workflowModuleId", ignore = true)
    @Mapping(target = "businessId", ignore = true)
    @Mapping(target = "bpmnProcessId", ignore = true)
    @Mapping(target = "bpmnProcessVersion", ignore = true)
    @Mapping(target = "workflowTitle", ignore = true)
    @Mapping(target = "workflowId", ignore = true)
    @Mapping(target = "bpmnTaskId", ignore = true)
    @Mapping(target = "taskDefinition", ignore = true)
    @Mapping(target = "taskDefinitionTitle", ignore = true)
    @Mapping(target = "dueDate", ignore = true)
    @Mapping(target = "followUpDate", ignore = true)
    @Mapping(target = "read", ignore = true)
    @Mapping(target = "title", ignore = true)
    @Mapping(target = "uiUriType", ignore = true)
    @Mapping(target = "detailsFulltextSearch", ignore = true)
    public abstract void ontoModel(@MappingTarget io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTask userTask, UserTaskCompletedEvent event);

    @Mapping(target = "id", source = "userTaskId")
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "endedAt", source = "timestamp")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "uiUri", ignore = true)
    @Mapping(target = "workflowModuleUri", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "candidateUsers", ignore = true)
    @Mapping(target = "candidateGroups", ignore = true)
    @Mapping(target = "details", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "workflowModuleId", ignore = true)
    @Mapping(target = "businessId", ignore = true)
    @Mapping(target = "bpmnProcessId", ignore = true)
    @Mapping(target = "bpmnProcessVersion", ignore = true)
    @Mapping(target = "workflowTitle", ignore = true)
    @Mapping(target = "workflowId", ignore = true)
    @Mapping(target = "bpmnTaskId", ignore = true)
    @Mapping(target = "taskDefinition", ignore = true)
    @Mapping(target = "taskDefinitionTitle", ignore = true)
    @Mapping(target = "dueDate", ignore = true)
    @Mapping(target = "followUpDate", ignore = true)
    @Mapping(target = "read", ignore = true)
    @Mapping(target = "title", ignore = true)
    @Mapping(target = "uiUriType", ignore = true)
    @Mapping(target = "detailsFulltextSearch", ignore = true)
    public abstract void ontoModel(@MappingTarget io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTask userTask, UserTaskCancelledEvent event);

    @Mapping(target = "serverTimestamp", expression = "java(java.time.OffsetDateTime.now())")
    @Mapping(target = "userTasks", source = "pageObjects")
    @Mapping(target = "page", expression = "java(toApiUserTaskPage(userTaskPage))")
    public abstract UserTasks toUserTasksApi(Page<io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTask> userTaskPage);

    public abstract io.vanillabp.cockpit.gui.api.v1.Page toApiUserTaskPage(Page<io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTask> userTaskPage);

    public abstract io.vanillabp.cockpit.gui.api.v1.UserTask toApi(io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTask userTask);

}
