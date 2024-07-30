package io.vanillabp.cockpit.bpms.kafka;

import com.google.protobuf.ProtocolStringList;
import com.google.protobuf.Timestamp;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.DetailsMap;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import io.vanillabp.cockpit.util.protobuf.ProtobufHelper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = MappingConstants.ComponentModel.DEFAULT)
public abstract class ProtobufUserTaskMapper {

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

    @Mapping(target = "id", source = "userTaskId")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", source = "timestamp")
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "updatedBy", source = "initiator")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "readBy", ignore = true)
    @Mapping(target = "readAt", ignore = true)
    @Mapping(target = "dangling", ignore = true)
    @Mapping(target = "targetGroups", ignore = true)
    @Mapping(target = "assignee", source = "assignee", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "candidateUsers", source = "candidateUsersList", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "candidateGroups", source = "candidateGroupsList", qualifiedByName = GROUP_MAPPING)
    @Mapping(target = "details", source = "details", qualifiedByName = DETAILS_MAPPING)
    public abstract UserTask toNewTask(UserTaskCreatedOrUpdatedEvent event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "updatedBy", source = "initiator")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "readBy", ignore = true)
    @Mapping(target = "readAt", ignore = true)
    @Mapping(target = "dangling", ignore = true)
    @Mapping(target = "targetGroups", ignore = true)
    @Mapping(target = "assignee", source = "assignee", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "candidateUsers", source = "candidateUsersList", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "candidateGroups", source = "candidateGroupsList", qualifiedByName = GROUP_MAPPING)
    @Mapping(target = "details", source = "details", qualifiedByName = DETAILS_MAPPING)
    public abstract UserTask toUpdatedTask(UserTaskCreatedOrUpdatedEvent event, @MappingTarget UserTask result);

    public OffsetDateTime map(Timestamp value) {
        return ProtobufHelper.map(value);
    }

    public List<String> map(ProtocolStringList stringList){
        return stringList.stream().toList();
    }

    @Named(DETAILS_MAPPING)
    protected Map<String, Object> map(
            final DetailsMap detailsMap) {
        return DetailsMapper.mapMapValue(detailsMap);
    }

}
