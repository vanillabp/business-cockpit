package io.vanillabp.cockpit.bpms.kafka;

import com.google.protobuf.Timestamp;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.DetailsMap;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import io.vanillabp.cockpit.util.protobuf.ProtobufHelper;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import java.time.OffsetDateTime;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = MappingConstants.ComponentModel.DEFAULT)
public abstract class ProtobufWorkflowMapper {

    private static final String DETAILS_MAPPING = "detailsMapping";
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
    @Mapping(target = "dangling", ignore = true)
    @Mapping(target = "targetGroups", ignore = true)
    @Mapping(target = "initiator", source = "initiator", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "accessibleToUsers", source = "accessibleToUsersList", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "accessibleToGroups", source = "accessibleToGroupsList", qualifiedByName = GROUP_MAPPING)
    @Mapping(target = "details", source = "details", qualifiedByName = DETAILS_MAPPING)
    public abstract Workflow toNewWorkflow(WorkflowCreatedOrUpdatedEvent event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "updatedBy", source = "initiator")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "dangling", ignore = true)
    @Mapping(target = "targetGroups", ignore = true)
    @Mapping(target = "initiator", source = "initiator", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "accessibleToUsers", source = "accessibleToUsersList", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "accessibleToGroups", source = "accessibleToGroupsList", qualifiedByName = GROUP_MAPPING)
    @Mapping(target = "details", source = "details", qualifiedByName = DETAILS_MAPPING)
    public abstract Workflow toUpdatedWorkflow(WorkflowCreatedOrUpdatedEvent event, @MappingTarget Workflow result);

    public OffsetDateTime map(Timestamp value) {
        return ProtobufHelper.map(value);
    }

    @Named(DETAILS_MAPPING)
    protected Map<String, Object> map(
            final DetailsMap detailsMap) {
        return DetailsMapper.mapMapValue(detailsMap);
    }

}