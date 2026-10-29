package io.vanillabp.cockpit.bpms.api.v1;

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

/**
 * Maps the user task events of version 1 of the BPMS API onto the user tasks the cockpit stores.
 * Version 1 knows no admitted users, so a report through it leaves them as they are.
 */
@Mapper(implementationName = "UserTaskMapperV1Impl")
public abstract class UserTaskMapper {

    private static final String PERSON_MAPPING = "personMapping";
    private static final String GROUP_MAPPING = "groupMapping";
    private static final String FOLLOW_UP_DATE_MAPPING = "followUpDateMapping";

    @Autowired
    private PersonAndGroupMapper personAndGroupMapper;

    /**
     * Creates the mapper. MapStruct generates the subclass which Spring creates.
     */
    public UserTaskMapper() {
    }

    /**
     * Turns the id of a user, as a workflow module reports it, into a person of the cockpit.
     *
     * @param userId The id of the user, or {@code null} if the report names nobody
     * @return The person, or {@code null} if the report names nobody
     */
    @Named(PERSON_MAPPING)
    public Person toPerson(
            final String userId) {
        // a reporting system which names no user means no person at all. A person built from a
        // null id is hidden by the GUI, but every comparison of ids stumbles over it, a claim for
        // example
        return userId == null ? null : personAndGroupMapper.toModelPerson(userId);
    }

    /**
     * Turns the id of a group, as a workflow module reports it, into a group of the cockpit.
     *
     * @param groupId The id of the group
     * @return The group
     */
    @Named(GROUP_MAPPING)
    public Group toGroup(
            final String groupId) {
        return personAndGroupMapper.toModelGroup(groupId);
    }

    /**
     * Cuts the seconds off a follow-up date, so that the date counts in whole minutes.
     *
     * @param followUpDate The follow-up date as it was reported, or {@code null}
     * @return The date without seconds, or {@code null} if none was reported
     */
    @Named(FOLLOW_UP_DATE_MAPPING)
    public OffsetDateTime toFollowUpDate(
            final OffsetDateTime followUpDate) {
        return followUpDate == null ? null : followUpDate.withSecond(0).withNano(0);
    }

    /**
     * Builds a new user task from a report the cockpit knows nothing about yet.
     *
     * @param event The report of a created or an updated task
     * @return The new task, not stored yet
     */
    @Mapping(target = "id", source = "userTaskId")
    @Mapping(target = "version", ignore = true)
    // maintained by the cockpit itself, never taken from an event:
    @Mapping(target = "reportedAt", ignore = true)
    // the event's timestamp, stamped where reports are weighed against each other:
    @Mapping(target = "latestEventAt", ignore = true)
    @Mapping(target = "candidateUsersSince", ignore = true)
    @Mapping(target = "createdAt", source = "timestamp")
    @Mapping(target = "knownFromItsEndAlone", ignore = true)
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
    @Mapping(target = "notificationDelivery", ignore = true)
    @Mapping(target = "followUpDate", source = "followUpDate", qualifiedByName = FOLLOW_UP_DATE_MAPPING)
    @Mapping(target = "assignee", source = "assignee", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "candidateUsers", source = "candidateUsers", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "candidateGroups", source = "candidateGroups", qualifiedByName = GROUP_MAPPING)
    @Mapping(target = "excludedCandidateUsers", source = "excludedCandidateUsers", qualifiedByName = PERSON_MAPPING)
    // version 1 of the API has no admitted users, so a report through it leaves them as they are
    @Mapping(target = "admittedUsers", ignore = true)
    public abstract UserTask toNewTask(UserTaskCreatedOrUpdatedEvent event);
    
    /**
     * Maps a report onto a user task the cockpit holds already. The id, the creation and the
     * follow-up date stay as they are.
     *
     * @param event The report of an updated task
     * @param result The stored task, changed in place
     * @return The stored task
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    // maintained by the cockpit itself, never taken from an event:
    @Mapping(target = "reportedAt", ignore = true)
    // the event's timestamp, stamped where reports are weighed against each other:
    @Mapping(target = "latestEventAt", ignore = true)
    @Mapping(target = "candidateUsersSince", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "knownFromItsEndAlone", ignore = true)
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
    @Mapping(target = "notificationDelivery", ignore = true)
    @Mapping(target = "followUpDate", ignore = true)
    // who sees the task is mapped like everything else. For a task the cockpit holds already,
    // WhoSeesAUserTask puts the stored values back, because only the report which created the task
    // says who sees it
    @Mapping(target = "assignee", source = "assignee", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "candidateUsers", source = "candidateUsers", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "candidateGroups", source = "candidateGroups", qualifiedByName = GROUP_MAPPING)
    @Mapping(target = "excludedCandidateUsers", source = "excludedCandidateUsers", qualifiedByName = PERSON_MAPPING)
    // version 1 of the API has no admitted users, so a report through it leaves them as they are
    @Mapping(target = "admittedUsers", ignore = true)
    public abstract UserTask toUpdatedTask(UserTaskCreatedOrUpdatedEvent event, @MappingTarget UserTask result);

}
