package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import io.vanillabp.cockpit.bpms.api.v1.RegisterWorkflowModuleEvent;
import io.vanillabp.cockpit.bpms.api.v1.UserTaskCancelledEvent;
import io.vanillabp.cockpit.bpms.api.v1.UserTaskCompletedEvent;
import io.vanillabp.cockpit.bpms.api.v1.UserTaskCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.v1.WorkflowCancelledEvent;
import io.vanillabp.cockpit.bpms.api.v1.WorkflowCompletedEvent;
import io.vanillabp.cockpit.bpms.api.v1.WorkflowCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.commons.mapstruct.NoMappingMethod;
import io.vanillabp.cockpit.gui.api.v1.Group;
import io.vanillabp.cockpit.gui.api.v1.Person;
import io.vanillabp.cockpit.gui.api.v1.UserTask;
import io.vanillabp.cockpit.gui.api.v1.Workflow;
import io.vanillabp.cockpit.gui.api.v1.WorkflowModule;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper
public abstract class OfficialApiMapper {

    @Mapping(target = "id", expression = "java(id)")
    @Mapping(target = "version", ignore = true)
    public abstract WorkflowModule toApi(RegisterWorkflowModuleEvent event, String id);

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
                .toList();

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
                .toList();

    }

    @NoMappingMethod
    protected String proxiedUiUri(
            final WorkflowCreatedOrUpdatedEvent event) {

        if (event.getWorkflowModuleId() == null) {
            return null;
        }
        if (event.getUiUriPath() == null) {
            return null;
        }

        return "/wm/"
                + event.getWorkflowModuleId()
                + (event.getUiUriPath().startsWith("/")
                ? event.getUiUriPath()
                : "/" + event.getUiUriPath());

    }

    @NoMappingMethod
    protected String proxiedWorkflowModuleUri(
            final WorkflowCreatedOrUpdatedEvent event) {

        if (event.getWorkflowModuleId() == null) {
            return null;
        }

        return "/wm/"
                + event.getWorkflowModuleId();

    }

    @Mapping(target = "id", source = "workflowId")
    @Mapping(target = "createdAt", source = "timestamp")
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "initiator", qualifiedByName = "person")
    @Mapping(target = "accessibleToUsers", qualifiedByName = "persons")
    @Mapping(target = "accessibleToGroups", qualifiedByName = "groups")
    @Mapping(target = "version", source = "bpmnProcessVersion")
    @Mapping(target = "uiUri", expression = "java(proxiedUiUri(event))")
    @Mapping(target = "workflowModuleUri", expression = "java(proxiedWorkflowModuleUri(event))")
    public abstract Workflow toApi(WorkflowCreatedOrUpdatedEvent event);

    @Mapping(target = "id", source = "workflowId")
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "initiator", qualifiedByName = "person")
    @Mapping(target = "accessibleToUsers", qualifiedByName = "persons")
    @Mapping(target = "accessibleToGroups", qualifiedByName = "groups")
    @Mapping(target = "version", source = "bpmnProcessVersion")
    @Mapping(target = "uiUri", ignore = true)
    @Mapping(target = "workflowModuleUri", ignore = true)
    public abstract void ontoApi(@MappingTarget Workflow workflow, WorkflowCreatedOrUpdatedEvent event);

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

    @NoMappingMethod
    protected String proxiedUiUri(
            final UserTaskCreatedOrUpdatedEvent event) {

        if (event.getWorkflowModuleId() == null) {
            return null;
        }
        if (event.getUiUriPath() == null) {
            return null;
        }

        return "/wm/"
                + event.getWorkflowModuleId()
                + (event.getUiUriPath().startsWith("/")
                ? event.getUiUriPath()
                : "/" + event.getUiUriPath());

    }

    @NoMappingMethod
    protected String proxiedWorkflowModuleUri(
            final UserTaskCreatedOrUpdatedEvent event) {

        if (event.getWorkflowModuleId() == null) {
            return null;
        }

        return "/wm/"
                + event.getWorkflowModuleId();

    }

    @Mapping(target = "id", source = "userTaskId")
    @Mapping(target = "createdAt", source = "timestamp")
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "assignee", qualifiedByName = "person")
    @Mapping(target = "candidateUsers", qualifiedByName = "persons")
    @Mapping(target = "candidateGroups", qualifiedByName = "groups")
    @Mapping(target = "version", source = "bpmnProcessVersion")
    @Mapping(target = "uiUri", expression = "java(proxiedUiUri(event))")
    @Mapping(target = "workflowModuleUri", expression = "java(proxiedWorkflowModuleUri(event))")
    @Mapping(target = "read", ignore = true)
    public abstract UserTask toApi(UserTaskCreatedOrUpdatedEvent event);

    @Mapping(target = "id", source = "event.userTaskId")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", source = "event.timestamp")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "assignee", qualifiedByName = "person")
    @Mapping(target = "candidateUsers", qualifiedByName = "persons")
    @Mapping(target = "candidateGroups", qualifiedByName = "groups")
    @Mapping(target = "version", source = "event.bpmnProcessVersion")
    @Mapping(target = "uiUri", ignore = true)
    @Mapping(target = "workflowModuleUri", ignore = true)
    @Mapping(target = "read", ignore = true)
    public abstract void ontoApi(@MappingTarget UserTask userTask, UserTaskCreatedOrUpdatedEvent event);

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
