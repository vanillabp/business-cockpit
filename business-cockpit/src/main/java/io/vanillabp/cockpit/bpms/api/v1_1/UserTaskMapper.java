package io.vanillabp.cockpit.bpms.api.v1_1;

import io.vanillabp.cockpit.bpms.WhatAnEndReports;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import java.time.OffsetDateTime;
import java.util.function.Supplier;
import org.mapstruct.BeanMapping;
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
    // the event's timestamp, stamped where reports are weighed against each other:
    @Mapping(target = "latestEventAt", ignore = true)
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
    // the event's timestamp, stamped where reports are weighed against each other:
    @Mapping(target = "latestEventAt", ignore = true)
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
    // the event's timestamp, stamped where reports are weighed against each other:
    @Mapping(target = "latestEventAt", ignore = true)
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

    // an end overwrites what it reports and leaves the rest of the stored task as it is - see
    // decision 19 in the repository's DECISIONS.md
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    // the record is the one the service looked up, and MongoDB counts its saves:
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    // maintained by the cockpit itself, never taken from an event:
    @Mapping(target = "reportedAt", ignore = true)
    @Mapping(target = "readBy", ignore = true)
    @Mapping(target = "readAt", ignore = true)
    @Mapping(target = "candidateUsersSince", ignore = true)
    // the event's timestamp, stamped where reports are weighed against each other:
    @Mapping(target = "latestEventAt", ignore = true)
    // an end does not say when the task began, and guessing it would make the same task look
    // different depending on which of the two reports arrived first:
    @Mapping(target = "createdAt", ignore = true)
    // audit information, replaced by the cockpit's own clock and user whenever the record is saved:
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "updatedBy", source = "initiator")
    // the service and the controller set these three, because an end means more to them than the
    // event says:
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "endReason", ignore = true)
    @Mapping(target = "initiator", ignore = true)
    // read from the assignee and the candidates whenever they are asked for:
    @Mapping(target = "targetGroups", ignore = true)
    @Mapping(target = "dangling", ignore = true)
    // what the notification poller hands on to the notification service, never stored:
    @Mapping(target = "notificationType", ignore = true)
    @Mapping(target = "forced", ignore = true)
    // the follow-up date comes with the creation and belongs to the cockpit afterwards: a user sets
    // it there and no workflow system hears about it
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

    // an end overwrites what it reports and leaves the rest of the stored task as it is - see
    // decision 19 in the repository's DECISIONS.md
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    // the record is the one the service looked up, and MongoDB counts its saves:
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    // maintained by the cockpit itself, never taken from an event:
    @Mapping(target = "reportedAt", ignore = true)
    @Mapping(target = "readBy", ignore = true)
    @Mapping(target = "readAt", ignore = true)
    @Mapping(target = "candidateUsersSince", ignore = true)
    // the event's timestamp, stamped where reports are weighed against each other:
    @Mapping(target = "latestEventAt", ignore = true)
    // an end does not say when the task began, and guessing it would make the same task look
    // different depending on which of the two reports arrived first:
    @Mapping(target = "createdAt", ignore = true)
    // audit information, replaced by the cockpit's own clock and user whenever the record is saved:
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "updatedBy", source = "initiator")
    // the service and the controller set these three, because an end means more to them than the
    // event says:
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "endReason", ignore = true)
    @Mapping(target = "initiator", ignore = true)
    // read from the assignee and the candidates whenever they are asked for:
    @Mapping(target = "targetGroups", ignore = true)
    @Mapping(target = "dangling", ignore = true)
    // what the notification poller hands on to the notification service, never stored:
    @Mapping(target = "notificationType", ignore = true)
    @Mapping(target = "forced", ignore = true)
    // the follow-up date comes with the creation and belongs to the cockpit afterwards: a user sets
    // it there and no workflow system hears about it
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
     * Whatever the end does not report leaves what is stored alone (see decision 19 in the
     * repository's DECISIONS.md), and {@link WhatAnEndReports} holds the part of that rule a mapping
     * annotation cannot express. Field by field the mapping reads like this.
     * <p>
     * An end may replace what it reports, because that is the youngest answer there is about the
     * task: where it sits in the model (the source, the workflow module, the BPMN process and its
     * version, the workflow and the sub workflow it belongs to, the business id, the BPMN task and
     * the task definition), what the cockpit shows of it (the three titles, the address of the user
     * interface, the comment, the business data and the words the fulltext search reads), how the
     * workflow module wants notifications delivered, and the due date.
     * <p>
     * An end may not replace what it cannot know. The assignee and the candidates are cockpit-owned
     * state: a task is taken over in the cockpit and the notification poller needs the former
     * assignee. A user sets the follow-up date in the cockpit. Who read the task, when the cockpit
     * stored it, since when somebody is a candidate and how often the record was saved are the
     * cockpit's own record-keeping. When the task began is what an end does not say. That it has
     * ended, why, and who ended it are set by the service and the controller rather than mapped,
     * because an end means more to them than the event says. The target groups and the dangling
     * flag are read from the assignee and the candidates, and the kind of notification and the
     * forced flag never leave the notification poller.
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

        final var storedDetails = WhatAnEndReports.before(stored.getDetails());
        final var storedFulltextSearch = stored.getDetailsFulltextSearch();
        final var storedTitle = WhatAnEndReports.before(stored.getTitle());
        final var storedWorkflowTitle = WhatAnEndReports.before(stored.getWorkflowTitle());
        final var storedTaskDefinitionTitle = WhatAnEndReports.before(stored.getTaskDefinitionTitle());
        final var task = mapping.get();
        task.setDetails(WhatAnEndReports.whatToStore(task.getDetails(), storedDetails));
        task.setDetailsFulltextSearch(
                WhatAnEndReports.whatToStore(task.getDetailsFulltextSearch(), storedFulltextSearch));
        task.setTitle(WhatAnEndReports.whatToStore(task.getTitle(), storedTitle));
        task.setWorkflowTitle(WhatAnEndReports.whatToStore(task.getWorkflowTitle(), storedWorkflowTitle));
        task.setTaskDefinitionTitle(
                WhatAnEndReports.whatToStore(task.getTaskDefinitionTitle(), storedTaskDefinitionTitle));
        return task;

    }

}
