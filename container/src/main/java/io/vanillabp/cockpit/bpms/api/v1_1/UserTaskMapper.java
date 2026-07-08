package io.vanillabp.cockpit.bpms.api.v1_1;

import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import java.time.OffsetDateTime;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(implementationName = "UserTaskMapperV1_1Impl")
public abstract class UserTaskMapper {

    private static final String PERSON_MAPPING = "personMapping";
    private static final String GROUP_MAPPING = "groupMapping";
    private static final String FOLLOW_UP_DATE_MAPPING = "followUpDateMapping";

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

    @Named(FOLLOW_UP_DATE_MAPPING)
    public OffsetDateTime toFollowUpDate(
            final OffsetDateTime followUpDate) {
        return followUpDate == null ? null : followUpDate.withSecond(0).withNano(0);
    }

    @Mapping(target = "id", source = "userTaskId")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", source = "timestamp")
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "updatedBy", source = "initiator")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "readBy", ignore = true)
    @Mapping(target = "readAt", ignore = true)
    @Mapping(target = "targetGroups", ignore = true)
    @Mapping(target = "dangling", ignore = true)
    @Mapping(target = "notificationType", ignore = true)
    @Mapping(target = "forced", ignore = true)
    @Mapping(target = "endReason", ignore = true)
    @Mapping(target = "followUpDate", source = "followUpDate", qualifiedByName = FOLLOW_UP_DATE_MAPPING)
    @Mapping(target = "assignee", source = "assignee", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "candidateUsers", source = "candidateUsers", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "candidateGroups", source = "candidateGroups", qualifiedByName = GROUP_MAPPING)
    @Mapping(target = "excludedCandidateUsers", source = "excludedCandidateUsers", qualifiedByName = PERSON_MAPPING)
    public abstract UserTask toNewTask(UserTaskCreatedEvent event);

    @Mapping(target = "id", source = "userTaskId")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", source = "timestamp")
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "updatedBy", source = "initiator")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "readBy", ignore = true)
    @Mapping(target = "readAt", ignore = true)
    @Mapping(target = "targetGroups", ignore = true)
    @Mapping(target = "dangling", ignore = true)
    @Mapping(target = "notificationType", ignore = true)
    @Mapping(target = "forced", ignore = true)
    @Mapping(target = "endReason", ignore = true)
    @Mapping(target = "followUpDate", source = "followUpDate", qualifiedByName = FOLLOW_UP_DATE_MAPPING)
    @Mapping(target = "assignee", source = "assignee", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "candidateUsers", source = "candidateUsers", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "candidateGroups", source = "candidateGroups", qualifiedByName = GROUP_MAPPING)
    @Mapping(target = "excludedCandidateUsers", source = "excludedCandidateUsers", qualifiedByName = PERSON_MAPPING)
    public abstract UserTask toNewTask(UserTaskUpdatedEvent event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "updatedBy", source = "initiator")
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "readBy", ignore = true)
    @Mapping(target = "readAt", ignore = true)
    @Mapping(target = "targetGroups", ignore = true)
    @Mapping(target = "dangling", ignore = true)
    @Mapping(target = "notificationType", ignore = true)
    @Mapping(target = "forced", ignore = true)
    @Mapping(target = "endReason", ignore = true)
    @Mapping(target = "followUpDate", ignore = true)
    @Mapping(target = "assignee", source = "assignee", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "candidateUsers", source = "candidateUsers", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "candidateGroups", source = "candidateGroups", qualifiedByName = GROUP_MAPPING)
    @Mapping(target = "excludedCandidateUsers", source = "excludedCandidateUsers", qualifiedByName = PERSON_MAPPING)
    public abstract UserTask toUpdatedTask(UserTaskUpdatedEvent event, @MappingTarget UserTask result);

}
