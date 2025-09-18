package io.vanillabp.cockpit.bpms.api.v1_1;

import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper
public abstract class WorkflowMapper {

    private static final String PERSON_MAPPING = "personMapping";
    private static final String GROUP_MAPPING = "groupMapping";

    @Autowired
    private PersonAndGroupMapper personAndGroupMapper;

    @Named(PERSON_MAPPING)
    public Person toPerson(
            final String userId) {
        return personAndGroupMapper.toModelPerson(userId);
    }

    @Named(GROUP_MAPPING)
    public Group toGroup(
            final String groupId) {
        return personAndGroupMapper.toModelGroup(groupId);
    }

    @Mapping(target = "id", source = "workflowId")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", source = "timestamp")
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "updatedBy", source = "initiator")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "targetGroups", ignore = true)
    @Mapping(target = "dangling", ignore = true)
    @Mapping(target = "initiator", source = "initiator", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "accessibleToUsers", source = "accessibleToUsers", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "accessibleToGroups", source = "accessibleToGroups", qualifiedByName = GROUP_MAPPING)
    public abstract Workflow toNewWorkflow(WorkflowCreatedEvent event);

    @Mapping(target = "id", source = "workflowId")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", source = "timestamp")
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "updatedBy", source = "initiator")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "targetGroups", ignore = true)
    @Mapping(target = "dangling", ignore = true)
    @Mapping(target = "initiator", source = "initiator", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "accessibleToUsers", source = "accessibleToUsers", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "accessibleToGroups", source = "accessibleToGroups", qualifiedByName = GROUP_MAPPING)
    public abstract Workflow toNewWorkflow(WorkflowUpdatedEvent event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "updatedBy", source = "initiator")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "targetGroups", ignore = true)
    @Mapping(target = "dangling", ignore = true)
    @Mapping(target = "initiator", source = "initiator", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "accessibleToUsers", source = "accessibleToUsers", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "accessibleToGroups", source = "accessibleToGroups", qualifiedByName = GROUP_MAPPING)
    public abstract Workflow toUpdatedWorkflow(WorkflowUpdatedEvent event, @MappingTarget Workflow result);
    
}
