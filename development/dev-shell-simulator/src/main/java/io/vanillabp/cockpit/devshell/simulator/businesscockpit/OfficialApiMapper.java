package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import io.vanillabp.cockpit.bpms.api.v1.RegisterWorkflowModuleEvent;
import io.vanillabp.cockpit.bpms.api.v1.UserTaskCancelledEvent;
import io.vanillabp.cockpit.bpms.api.v1.UserTaskCompletedEvent;
import io.vanillabp.cockpit.bpms.api.v1.UserTaskCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.v1.WorkflowCancelledEvent;
import io.vanillabp.cockpit.bpms.api.v1.WorkflowCompletedEvent;
import io.vanillabp.cockpit.bpms.api.v1.WorkflowCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.v1_1.GroupHierarchy;
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
import java.util.Map;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

/**
 * Converts between the events of the BPMS API, the simulator's stored model and the official
 * API of the cockpit. The BPMS API comes in version 1 and version 1.1, so many methods exist
 * once for each version.
 */
@Mapper
public abstract class OfficialApiMapper {

    private static final String GROUP_HIERARCHY_MAPPING = "groupHierarchyMapping";

    /**
     * Creates the mapper. MapStruct's generated subclass calls it.
     */
    public OfficialApiMapper() {
    }

    /**
     * Turns a registration of a workflow module, BPMS API version 1, into the stored model.
     * Version 1 knows no group hierarchy.
     *
     * @param event The registration
     * @param id The id of the workflow module
     * @return The workflow module to store
     */
    @Mapping(target = "id", expression = "java(id)")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "groupHierarchy", ignore = true)
    public abstract io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.WorkflowModule toModel(RegisterWorkflowModuleEvent event, String id);

    /**
     * Turns a registration of a workflow module, BPMS API version 1.1, into the stored model,
     * together with its group hierarchy.
     *
     * @param event The registration
     * @param id The id of the workflow module
     * @return The workflow module to store
     */
    @Mapping(target = "id", expression = "java(id)")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "groupHierarchy", qualifiedByName = GROUP_HIERARCHY_MAPPING)
    public abstract io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.WorkflowModule toModel(io.vanillabp.cockpit.bpms.api.v1_1.RegisterWorkflowModuleEvent event, String id);

    /**
     * Turns a group id into a stored group. The id is used as the display name too.
     *
     * @param group The id of the group
     * @return The group
     */
    @Named("group")
    @Mapping(target = "id", expression = "java(group)")
    @Mapping(target = "display", expression = "java(group)")
    @Mapping(target = "details", ignore = true)
    public abstract io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Group toGroupModel(String group);

    /**
     * Turns the group hierarchy of a registration into a map from a group to its target groups.
     *
     * @param groupHierarchy The group hierarchy, may be {@code null}
     * @return The map, empty if there is no hierarchy
     */
    @Named(GROUP_HIERARCHY_MAPPING)
    protected Map<String, List<String>> toModel(List<GroupHierarchy> groupHierarchy) {
        if (groupHierarchy ==  null) {
            return Map.of();
        }
        return groupHierarchy
                .stream()
                .collect(Collectors.toMap(GroupHierarchy::getGroup, GroupHierarchy::getTargets));
    }

    /**
     * Turns the retrieve mode of the official API into the one of the {@link TaskService}.
     *
     * @param retrieveMode The retrieve mode, may be {@code null}
     * @return The retrieve mode of the service, {@code ALL} if none was given
     */
    public TaskService.RetrieveMode toModel(UserTaskRetrieveMode retrieveMode) {
        if (retrieveMode == null) {
            return TaskService.RetrieveMode.ALL;
        }
        return TaskService.RetrieveMode.valueOf(retrieveMode.name());
    }

    /**
     * Turns the retrieve mode of the official API into the one of the {@link WorkflowService}.
     *
     * @param retrieveMode The retrieve mode, may be {@code null}
     * @return The retrieve mode of the service, {@code ALL} if none was given
     */
    public WorkflowService.RetrieveMode toModel(WorkflowRetrieveMode retrieveMode) {
        if (retrieveMode == null) {
            return WorkflowService.RetrieveMode.ALL;
        }
        return WorkflowService.RetrieveMode.valueOf(retrieveMode.name());
    }

    /**
     * Turns a group id into a group of the official API. The id is used as the display name too.
     *
     * @param group The id of the group
     * @return The group
     */
    @Named("apiGroup")
    @Mapping(target = "id", expression = "java(group)")
    @Mapping(target = "display", expression = "java(group)")
    @Mapping(target = "details", ignore = true)
    public abstract Group toGroupApi(String group);

    /**
     * Turns group ids into groups of the official API.
     *
     * @param groups The ids of the groups, may be {@code null}
     * @return The groups, or {@code null} if no ids were given
     */
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

    /**
     * Turns a user id into a person of the official API. The id is used as the display name too.
     *
     * @param person The id of the user
     * @return The person
     */
    @Named("apiPerson")
    @Mapping(target = "id", expression = "java(person)")
    @Mapping(target = "display", expression = "java(person)")
    @Mapping(target = "displayShort", expression = "java(person)")
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "avatar", ignore = true)
    @Mapping(target = "details", ignore = true)
    public abstract Person toPersonApi(String person);

    /**
     * Turns user ids into persons of the official API.
     *
     * @param persons The ids of the users, may be {@code null}
     * @return The persons, or {@code null} if no ids were given
     */
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

    /**
     * Turns group ids into stored groups.
     *
     * @param groups The ids of the groups, may be {@code null}
     * @return The groups, or {@code null} if no ids were given
     */
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

    /**
     * Turns a user id into a stored person. The id is used as the display name too.
     *
     * @param person The id of the user
     * @return The person
     */
    @Named("person")
    @Mapping(target = "id", expression = "java(person)")
    @Mapping(target = "display", expression = "java(person)")
    @Mapping(target = "displayShort", expression = "java(person)")
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "avatar", ignore = true)
    @Mapping(target = "details", ignore = true)
    public abstract io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Person toPersonModel(String person);

    /**
     * Turns user ids into stored persons.
     *
     * @param persons The ids of the users, may be {@code null}
     * @return The persons, or {@code null} if no ids were given
     */
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

    /**
     * The path of a workflow module behind the proxy of the dev shell: {@code /wm/<id>}.
     *
     * @param workflowModuleId The id of the workflow module
     * @return The path, or {@code null} if no id was given
     */
    @NoMappingMethod
    protected String proxiedWorkflowModuleUri(
            final String workflowModuleId) {

        if (workflowModuleId == null) {
            return null;
        }

        return "/wm/" + workflowModuleId;

    }

    /**
     * Turns a person of the official API into a stored person.
     *
     * @param person The person
     * @return The stored person
     */
    public abstract io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Person toModel(Person person);

    /**
     * Turns a group of the official API into a stored group.
     *
     * @param group The group
     * @return The stored group
     */
    public abstract io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Group toGroup(Group group);

    /**
     * Turns a "workflow created" event of BPMS API version 1 into a stored workflow.
     *
     * @param event The event
     * @return The workflow to store
     */
    @Mapping(target = "id", source = "workflowId")
    @Mapping(target = "createdAt", source = "timestamp")
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "initiator", qualifiedByName = "person")
    @Mapping(target = "accessibleToUsers", qualifiedByName = "persons")
    @Mapping(target = "accessibleToGroups", qualifiedByName = "groups")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "uiUri", source = "uiUriPath")
    @Mapping(target = "workflowModuleUri", expression = "java(proxiedWorkflowModuleUri(event.getWorkflowModuleId()))")
    public abstract io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Workflow toModel(WorkflowCreatedOrUpdatedEvent event);

    /**
     * Turns a "workflow created" event of BPMS API version 1.1 into a stored workflow.
     *
     * @param event The event
     * @return The workflow to store
     */
    @Mapping(target = "id", source = "workflowId")
    @Mapping(target = "createdAt", source = "timestamp")
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "initiator", qualifiedByName = "person")
    @Mapping(target = "accessibleToUsers", qualifiedByName = "persons")
    @Mapping(target = "accessibleToGroups", qualifiedByName = "groups")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "uiUri", source = "uiUriPath")
    @Mapping(target = "workflowModuleUri", expression = "java(proxiedWorkflowModuleUri(event.getWorkflowModuleId()))")
    public abstract io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Workflow toModel(WorkflowCreatedEvent event);

    /**
     * Turns a page of stored workflows into the answer of the official API.
     *
     * @param workflowPage The page of workflows
     * @return The answer, with the server's time set to now
     */
    @Mapping(target = "requestId", ignore = true)
    @Mapping(target = "serverTimestamp", expression = "java(java.time.OffsetDateTime.now())")
    @Mapping(target = "workflows", source = "pageObjects")
    @Mapping(target = "page", expression = "java(toApiWorkflowPage(workflowPage))")
    public abstract Workflows toWorkflowsApi(Page<io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Workflow> workflowPage);

    /**
     * The numbers of a page of workflows: page number, page size and totals.
     *
     * @param workflowPage The page of workflows
     * @return The numbers of the page
     */
    public abstract io.vanillabp.cockpit.gui.api.v1.Page toApiWorkflowPage(Page<io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Workflow> workflowPage);

    /**
     * Turns a stored workflow into a workflow of the official API.
     *
     * @param workflow The stored workflow
     * @return The workflow
     */
    public abstract io.vanillabp.cockpit.gui.api.v1.Workflow toApi(io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Workflow workflow);

    /**
     * Copies a "workflow updated" event of BPMS API version 1 onto a stored workflow.
     *
     * @param workflow The stored workflow, changed in place
     * @param event The event
     */
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

    /**
     * Copies a "workflow updated" event of BPMS API version 1.1 onto a stored workflow.
     *
     * @param workflow The stored workflow, changed in place
     * @param event The event
     */
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

    /**
     * Copies a "workflow completed" event of BPMS API version 1 onto a stored workflow.
     *
     * @param workflow The stored workflow, changed in place
     * @param event The event
     */
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

    /**
     * Copies a "workflow cancelled" event of BPMS API version 1 onto a stored workflow.
     *
     * @param workflow The stored workflow, changed in place
     * @param event The event
     */
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

    /**
     * Copies a "workflow cancelled" event of BPMS API version 1.1 onto a stored workflow.
     *
     * @param workflow The stored workflow, changed in place
     * @param event The event
     */
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

    /**
     * Copies a "workflow completed" event of BPMS API version 1.1 onto a stored workflow.
     *
     * @param workflow The stored workflow, changed in place
     * @param event The event
     */
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

    /**
     * Turns a "user task created" event of BPMS API version 1 into a stored user task.
     *
     * @param event The event
     * @return The user task to store
     */
    @Mapping(target = "id", source = "userTaskId")
    @Mapping(target = "createdAt", source = "timestamp")
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "assignee", qualifiedByName = "apiPerson")
    @Mapping(target = "candidateUsers", qualifiedByName = "apiPersons")
    @Mapping(target = "candidateGroups", qualifiedByName = "apiGroups")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "uiUri", source = "uiUriPath")
    @Mapping(target = "workflowModuleUri", expression = "java(proxiedWorkflowModuleUri(event.getWorkflowModuleId()))")
    @Mapping(target = "read", ignore = true)
    public abstract io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTask toModel(UserTaskCreatedOrUpdatedEvent event);

    /**
     * Copies a "user task updated" event of BPMS API version 1 onto a stored user task.
     *
     * @param userTask The stored user task, changed in place
     * @param event The event
     */
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

    /**
     * Turns a "user task created" event of BPMS API version 1.1 into a stored user task.
     *
     * @param event The event
     * @return The user task to store
     */
    @Mapping(target = "id", source = "event.userTaskId")
    @Mapping(target = "createdAt", source = "event.timestamp")
    @Mapping(target = "updatedAt", source = "event.timestamp")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "assignee", qualifiedByName = "apiPerson")
    @Mapping(target = "candidateUsers", qualifiedByName = "apiPersons")
    @Mapping(target = "candidateGroups", qualifiedByName = "apiGroups")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "uiUri", source = "uiUriPath")
    @Mapping(target = "workflowModuleUri", expression = "java(proxiedWorkflowModuleUri(event.getWorkflowModuleId()))")
    @Mapping(target = "read", ignore = true)
    public abstract io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTask toModel(UserTaskCreatedEvent event);

    /**
     * Copies a "user task updated" event of BPMS API version 1.1 onto a stored user task.
     *
     * @param userTask The stored user task, changed in place
     * @param event The event
     */
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

    /**
     * Copies a "user task completed" event of BPMS API version 1.1 onto a stored user task.
     *
     * @param userTask The stored user task, changed in place
     * @param event The event
     */
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

    /**
     * Copies a "user task cancelled" event of BPMS API version 1.1 onto a stored user task.
     *
     * @param userTask The stored user task, changed in place
     * @param event The event
     */
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

    /**
     * Copies a "user task completed" event of BPMS API version 1 onto a stored user task.
     *
     * @param userTask The stored user task, changed in place
     * @param event The event
     */
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

    /**
     * Copies a "user task cancelled" event of BPMS API version 1 onto a stored user task.
     *
     * @param userTask The stored user task, changed in place
     * @param event The event
     */
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

    /**
     * Turns a page of stored user tasks into the answer of the official API.
     *
     * @param userTaskPage The page of user tasks
     * @return The answer, with the server's time set to now
     */
    @Mapping(target = "serverTimestamp", expression = "java(java.time.OffsetDateTime.now())")
    @Mapping(target = "userTasks", source = "pageObjects")
    @Mapping(target = "page", expression = "java(toApiUserTaskPage(userTaskPage))")
    public abstract UserTasks toUserTasksApi(Page<io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTask> userTaskPage);

    /**
     * The numbers of a page of user tasks: page number, page size and totals.
     *
     * @param userTaskPage The page of user tasks
     * @return The numbers of the page
     */
    public abstract io.vanillabp.cockpit.gui.api.v1.Page toApiUserTaskPage(Page<io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTask> userTaskPage);

    /**
     * Turns a stored user task into a user task of the official API.
     *
     * @param userTask The stored user task
     * @return The user task
     */
    public abstract io.vanillabp.cockpit.gui.api.v1.UserTask toApi(io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTask userTask);

}
