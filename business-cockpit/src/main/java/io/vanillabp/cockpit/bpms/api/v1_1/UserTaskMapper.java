package io.vanillabp.cockpit.bpms.api.v1_1;

import io.vanillabp.cockpit.bpms.DetailsOfAnEnd;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import java.time.OffsetDateTime;
import java.util.function.Supplier;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
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
        // a reporting system which names no user means no person at all: building one from a null
        // id yields a person the GUI hides but every id comparison stumbles over, e.g. a claim
        return userId == null ? null : personAndGroupMapper.toModelPerson(userId);
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
    // maintained by the cockpit itself, never taken from an event:
    @Mapping(target = "reportedAt", ignore = true)
    @Mapping(target = "candidateUsersSince", ignore = true)
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
    @Mapping(target = "admittedUsers", source = "admittedUsers", qualifiedByName = PERSON_MAPPING)
    public abstract UserTask toNewTask(UserTaskCreatedEvent event);

    @Mapping(target = "id", source = "userTaskId")
    @Mapping(target = "version", ignore = true)
    // maintained by the cockpit itself, never taken from an event:
    @Mapping(target = "reportedAt", ignore = true)
    @Mapping(target = "candidateUsersSince", ignore = true)
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
    @Mapping(target = "admittedUsers", source = "admittedUsers", qualifiedByName = PERSON_MAPPING)
    public abstract UserTask toNewTask(UserTaskUpdatedEvent event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    // maintained by the cockpit itself, never taken from an event:
    @Mapping(target = "reportedAt", ignore = true)
    @Mapping(target = "candidateUsersSince", ignore = true)
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
    // the assignee is kept if the event provides none: a task is taken over in the
    // cockpit, so no workflow system can report that assignment
    @Mapping(target = "assignee", source = "assignee", qualifiedByName = PERSON_MAPPING,
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    // candidate users are cockpit-owned too: assigning a task in the cockpit adds a personal
    // candidate no event reports back, so an update must not replace the stored list. They are
    // therefore taken from the create event only. Groups, exclusions and admitted users stay
    // mapped.
    @Mapping(target = "candidateUsers", ignore = true)
    @Mapping(target = "candidateGroups", source = "candidateGroups", qualifiedByName = GROUP_MAPPING)
    @Mapping(target = "excludedCandidateUsers", source = "excludedCandidateUsers", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "admittedUsers", source = "admittedUsers", qualifiedByName = PERSON_MAPPING)
    public abstract UserTask toUpdatedTask(UserTaskUpdatedEvent event, @MappingTarget UserTask result);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    // maintained by the cockpit itself, never taken from an event:
    @Mapping(target = "reportedAt", ignore = true)
    @Mapping(target = "candidateUsersSince", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "updatedBy", source = "initiator")
    // the controller sets these two, because an end means more to them than the event says
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "initiator", ignore = true)
    @Mapping(target = "readBy", ignore = true)
    @Mapping(target = "readAt", ignore = true)
    @Mapping(target = "targetGroups", ignore = true)
    @Mapping(target = "dangling", ignore = true)
    @Mapping(target = "notificationType", ignore = true)
    @Mapping(target = "forced", ignore = true)
    @Mapping(target = "endReason", ignore = true)
    @Mapping(target = "followUpDate", ignore = true)
    // who the task belonged to is cockpit-owned state: a task is taken over in the cockpit, and
    // the notification poller needs the former assignee to tell them that somebody else finished
    // it. An end does not report any of this, so none of it is mapped.
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "candidateUsers", ignore = true)
    @Mapping(target = "candidateGroups", ignore = true)
    @Mapping(target = "excludedCandidateUsers", ignore = true)
    @Mapping(target = "admittedUsers", ignore = true)
    protected abstract UserTask mapEndedTask(UserTaskCompletedEvent event, @MappingTarget UserTask result);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    // maintained by the cockpit itself, never taken from an event:
    @Mapping(target = "reportedAt", ignore = true)
    @Mapping(target = "candidateUsersSince", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "updatedBy", source = "initiator")
    // the controller sets these two, because an end means more to them than the event says
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "initiator", ignore = true)
    @Mapping(target = "readBy", ignore = true)
    @Mapping(target = "readAt", ignore = true)
    @Mapping(target = "targetGroups", ignore = true)
    @Mapping(target = "dangling", ignore = true)
    @Mapping(target = "notificationType", ignore = true)
    @Mapping(target = "forced", ignore = true)
    @Mapping(target = "endReason", ignore = true)
    @Mapping(target = "followUpDate", ignore = true)
    // who the task belonged to is cockpit-owned state: a task is taken over in the cockpit, and
    // the notification poller needs the former assignee to tell them that somebody else finished
    // it. An end does not report any of this, so none of it is mapped.
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "candidateUsers", ignore = true)
    @Mapping(target = "candidateGroups", ignore = true)
    @Mapping(target = "excludedCandidateUsers", ignore = true)
    @Mapping(target = "admittedUsers", ignore = true)
    protected abstract UserTask mapEndedTask(UserTaskCancelledEvent event, @MappingTarget UserTask result);

    /**
     * Maps a completed or cancelled event onto the stored user task, so that the list of finished
     * work shows what the task was finished with.
     * <p>
     * Business data the end does not report leaves what is stored alone. {@link DetailsOfAnEnd}
     * says why a BPMS may have nothing left to report about a task which has just ended.
     *
     * @param event The end as it was reported
     * @param result The stored task, changed in place
     * @return The stored task
     */
    public UserTask toEndedTask(
            final UserTaskCompletedEvent event,
            @MappingTarget final UserTask result) {

        return keepingWhatTheEndDoesNotReport(result, () -> mapEndedTask(event, result));

    }

    /**
     * The same for a cancellation. The API declares a schema per kind of event and the generator
     * made a class of each, so there is no one type both of them are.
     *
     * @param event The end as it was reported
     * @param result The stored task, changed in place
     * @return The stored task
     */
    public UserTask toEndedTask(
            final UserTaskCancelledEvent event,
            @MappingTarget final UserTask result) {

        return keepingWhatTheEndDoesNotReport(result, () -> mapEndedTask(event, result));

    }

    private static UserTask keepingWhatTheEndDoesNotReport(
            final UserTask stored,
            final Supplier<UserTask> mapping) {

        final var storedDetails = DetailsOfAnEnd.before(stored.getDetails());
        final var storedFulltextSearch = stored.getDetailsFulltextSearch();
        final var task = mapping.get();
        task.setDetails(DetailsOfAnEnd.whatToStore(task.getDetails(), storedDetails));
        task.setDetailsFulltextSearch(
                DetailsOfAnEnd.whatToStore(task.getDetailsFulltextSearch(), storedFulltextSearch));
        return task;

    }

}
