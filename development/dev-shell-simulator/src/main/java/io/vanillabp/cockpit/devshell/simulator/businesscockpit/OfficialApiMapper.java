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
import io.vanillabp.cockpit.gui.api.v1.Group;
import io.vanillabp.cockpit.gui.api.v1.Person;
import io.vanillabp.cockpit.gui.api.v1.UserTask;
import io.vanillabp.cockpit.gui.api.v1.Workflow;
import io.vanillabp.cockpit.gui.api.v1.WorkflowModule;
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
    public abstract WorkflowModule toModel(RegisterWorkflowModuleEvent event, String id);

    @Mapping(target = "id", expression = "java(id)")
    @Mapping(target = "version", ignore = true)
    public abstract WorkflowModule toModel(io.vanillabp.cockpit.bpms.api.v1_1.RegisterWorkflowModuleEvent event, String id);

    @Named("group")
    @Mapping(target = "id", expression = "java(group)")
    @Mapping(target = "display", expression = "java(group)")
    @Mapping(target = "details", ignore = true)
    public abstract Group toGroupApi(String group);

    @Named("groups")
    protected List<Group> toGroupsApi(List<String> groups) {

        if (groups == null) {
            return null;
        }
        return groups
                .stream()
                .map(this::toGroupApi)
                .collect(Collectors.toList());

    }

    @Named("person")
    @Mapping(target = "id", expression = "java(person)")
    @Mapping(target = "display", expression = "java(person)")
    @Mapping(target = "displayShort", expression = "java(person)")
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "avatar", ignore = true)
    @Mapping(target = "details", ignore = true)
    public abstract Person toPersonApi(String person);

    @Named("persons")
    protected List<Person> toPersonsApi(List<String> persons) {

        if (persons == null) {
            return null;
        }
        return persons
                .stream()
                .map(this::toPersonApi)
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
    public abstract Workflow toModel(WorkflowCreatedOrUpdatedEvent event);

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
    public abstract Workflow toModel(WorkflowCreatedEvent event);

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
    public abstract void ontoApi(@MappingTarget Workflow workflow, WorkflowCreatedOrUpdatedEvent event);

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
    public abstract void ontoApi(@MappingTarget Workflow workflow, WorkflowUpdatedEvent event);

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
    public abstract void ontoApi(@MappingTarget Workflow workflow, WorkflowCompletedEvent event);

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
    public abstract void ontoApi(@MappingTarget Workflow workflow, WorkflowCancelledEvent event);

    @Mapping(target = "id", source = "workflowId")
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "endedAt", source = "timestamp")
    @Mapping(target = "initiator", qualifiedByName = "person")
    @Mapping(target = "accessibleToUsers", ignore = true)
    @Mapping(target = "accessibleToGroups", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "uiUri", ignore = true)
    @Mapping(target = "workflowModuleUri", ignore = true)
    public abstract void ontoApi(@MappingTarget Workflow workflow, io.vanillabp.cockpit.bpms.api.v1_1.WorkflowCancelledEvent event);

    @Mapping(target = "id", source = "workflowId")
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "endedAt", source = "timestamp")
    @Mapping(target = "initiator", qualifiedByName = "person")
    @Mapping(target = "accessibleToUsers", ignore = true)
    @Mapping(target = "accessibleToGroups", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "uiUri", ignore = true)
    @Mapping(target = "workflowModuleUri", ignore = true)
    public abstract void ontoApi(@MappingTarget Workflow workflow, io.vanillabp.cockpit.bpms.api.v1_1.WorkflowCompletedEvent event);

    @Mapping(target = "id", source = "userTaskId")
    @Mapping(target = "createdAt", source = "timestamp")
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "assignee", qualifiedByName = "person")
    @Mapping(target = "candidateUsers", qualifiedByName = "persons")
    @Mapping(target = "candidateGroups", qualifiedByName = "groups")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "uiUri", expression = "java(proxiedUiUri(event.getWorkflowModuleId(), event.getUiUriPath()))")
    @Mapping(target = "workflowModuleUri", expression = "java(proxiedWorkflowModuleUri(event.getWorkflowModuleId()))")
    @Mapping(target = "read", ignore = true)
    public abstract UserTask toModel(UserTaskCreatedOrUpdatedEvent event);

    @Mapping(target = "id", source = "event.userTaskId")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", source = "event.timestamp")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "assignee", qualifiedByName = "person")
    @Mapping(target = "candidateUsers", qualifiedByName = "persons")
    @Mapping(target = "candidateGroups", qualifiedByName = "groups")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "uiUri", ignore = true)
    @Mapping(target = "workflowModuleUri", ignore = true)
    @Mapping(target = "read", ignore = true)
    public abstract void ontoApi(@MappingTarget UserTask userTask, UserTaskCreatedOrUpdatedEvent event);

    @Mapping(target = "id", source = "event.userTaskId")
    @Mapping(target = "createdAt", source = "event.timestamp")
    @Mapping(target = "updatedAt", source = "event.timestamp")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "assignee", qualifiedByName = "person")
    @Mapping(target = "candidateUsers", qualifiedByName = "persons")
    @Mapping(target = "candidateGroups", qualifiedByName = "groups")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "uiUri", expression = "java(proxiedUiUri(event.getWorkflowModuleId(), event.getUiUriPath()))")
    @Mapping(target = "workflowModuleUri", expression = "java(proxiedWorkflowModuleUri(event.getWorkflowModuleId()))")
    @Mapping(target = "read", ignore = true)
    public abstract UserTask toModel(UserTaskCreatedEvent event);

    @Mapping(target = "id", source = "event.userTaskId")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", source = "event.timestamp")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "assignee", qualifiedByName = "person")
    @Mapping(target = "candidateUsers", qualifiedByName = "persons")
    @Mapping(target = "candidateGroups", qualifiedByName = "groups")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "uiUri", ignore = true)
    @Mapping(target = "workflowModuleUri", ignore = true)
    @Mapping(target = "read", ignore = true)
    public abstract void ontoApi(@MappingTarget UserTask userTask, UserTaskUpdatedEvent event);

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
    public abstract void ontoApi(@MappingTarget UserTask userTask, io.vanillabp.cockpit.bpms.api.v1_1.UserTaskCompletedEvent event);

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
    public abstract void ontoApi(@MappingTarget UserTask userTask, io.vanillabp.cockpit.bpms.api.v1_1.UserTaskCancelledEvent event);

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
    public abstract void ontoApi(@MappingTarget UserTask userTask, UserTaskCompletedEvent event);

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
    public abstract void ontoApi(@MappingTarget UserTask userTask, UserTaskCancelledEvent event);

}
