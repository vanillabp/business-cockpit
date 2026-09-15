package io.vanillabp.cockpit.bpms.kafka;

import com.google.protobuf.ProtocolStringList;
import com.google.protobuf.Timestamp;
import io.vanillabp.cockpit.bpms.WhatAnEndReports;
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
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ValueMapping;
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

    // proto3 enums carry an extra UNRECOGNIZED constant; map it (and the default USER_CONFIG) to
    // null, which the notification logic interprets as USER_CONFIG.
    @ValueMapping(source = "UNRECOGNIZED", target = MappingConstants.NULL)
    @ValueMapping(source = "USER_CONFIG", target = MappingConstants.NULL)
    abstract io.vanillabp.spi.cockpit.usertask.NotificationDelivery mapNotificationDelivery(
            io.vanillabp.cockpit.bpms.api.protobuf.v1.NotificationDelivery delivery);

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
    @Mapping(target = "dangling", ignore = true)
    @Mapping(target = "notificationType", ignore = true)
    @Mapping(target = "forced", ignore = true)
    @Mapping(target = "endReason", ignore = true)
    @Mapping(target = "targetGroups", ignore = true)
    @Mapping(target = "assignee", source = "assignee", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "candidateUsers", source = "candidateUsersList", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "candidateGroups", source = "candidateGroupsList", qualifiedByName = GROUP_MAPPING)
    @Mapping(target = "excludedCandidateUsers", source = "excludedCandidateUsersList", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "admittedUsers", source = "admittedUsersList", qualifiedByName = PERSON_MAPPING)
    // protoc deprecates the plain getter of a map field and points at the ...Map() one. It names
    // a repeated field ...List in the same way. Naming the sources here keeps the deprecated
    // getters out of the generated mapper. Both getters return the same map.
    @Mapping(target = "title", source = "titleMap")
    @Mapping(target = "workflowTitle", source = "workflowTitleMap")
    @Mapping(target = "taskDefinitionTitle", source = "taskDefinitionTitleMap")
    @Mapping(target = "details", source = "details", qualifiedByName = DETAILS_MAPPING)
    public abstract UserTask toNewTask(UserTaskCreatedOrUpdatedEvent event);

    /**
     * Maps an update event onto the stored user task.
     * <p>
     * The assignee is kept if the event does not report one. A task is taken over in the cockpit,
     * so the assignment belongs to the cockpit and no workflow system knows about it. Without
     * {@link NullValuePropertyMappingStrategy#IGNORE}, an event which leaves the assignee out would
     * drop the takeover without a word, and with it the recipient of the completion notification.
     * <p>
     * The candidate users are kept for the same reason. Assigning a task in the cockpit adds a
     * personal candidate which the workflow system does not know. A repeated protobuf field says
     * nothing about presence, so an empty list cannot be told apart from a field which was left
     * out. Events do report the candidates the engine knows, and mapping them would drop the
     * candidate the cockpit added. This has a price we accept: candidate users are taken from the
     * create event only, so later changes by the process do not reach the cockpit. Candidate
     * groups, excluded candidates and admitted users stay mapped, because the cockpit never writes
     * any of them.
     */
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
    @Mapping(target = "dangling", ignore = true)
    @Mapping(target = "notificationType", ignore = true)
    @Mapping(target = "forced", ignore = true)
    @Mapping(target = "endReason", ignore = true)
    @Mapping(target = "targetGroups", ignore = true)
    @Mapping(target = "followUpDate", ignore = true)
    @Mapping(target = "assignee", source = "assignee", qualifiedByName = PERSON_MAPPING,
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "candidateUsers", ignore = true)
    @Mapping(target = "candidateGroups", source = "candidateGroupsList", qualifiedByName = GROUP_MAPPING)
    @Mapping(target = "excludedCandidateUsers", source = "excludedCandidateUsersList", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "admittedUsers", source = "admittedUsersList", qualifiedByName = PERSON_MAPPING)
    @Mapping(target = "title", source = "titleMap")
    @Mapping(target = "workflowTitle", source = "workflowTitleMap")
    @Mapping(target = "taskDefinitionTitle", source = "taskDefinitionTitleMap")
    @Mapping(target = "details", source = "details", qualifiedByName = DETAILS_MAPPING)
    public abstract UserTask toUpdatedTask(UserTaskCreatedOrUpdatedEvent event, @MappingTarget UserTask result);

    /**
     * Maps a completed or cancelled event onto the stored user task.
     * <p>
     * The assignee and the candidates are left alone. The event of an ended task does not carry
     * them, because the Camunda adapters do not fill them. Mapping the missing values would wipe
     * just what the notification poller needs to tell the former assignee that somebody else
     * completed the task or that the process cancelled it. A repeated protobuf field says nothing
     * about presence, so an empty list cannot be told apart from a field which was left out. That
     * is why these fields are ignored here instead of being mapped with a null-value strategy.
     */
    // an end overwrites what it reports and leaves the rest of the stored task as it is. See
    // decision 19 in the repository's DECISIONS.md. A field the sender left out is declared
    // optional in the protobuf schema and arrives as null, and that is what this strategy answers.
    // The titles and the business data arrive empty instead, and toEndedTask answers that.
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
    // an end does not say when the task began. Guessing it would make the same task look
    // different, depending on which of the two reports arrived first:
    @Mapping(target = "createdAt", ignore = true)
    // audit information, replaced by the cockpit's own clock and user whenever the record is saved:
    @Mapping(target = "updatedAt", source = "timestamp")
    @Mapping(target = "updatedBy", source = "initiator")
    // who ended the task. Nobody reported means the process ended it. This is the one field an
    // end does clear, see decision 19 in the repository's DECISIONS.md
    @Mapping(target = "initiator", source = "initiator",
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    // the service sets these two, because an end means more to it than the event says:
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "endReason", ignore = true)
    // read from the assignee and the candidates whenever they are asked for:
    @Mapping(target = "dangling", ignore = true)
    @Mapping(target = "targetGroups", ignore = true)
    // what the notification poller hands on to the notification service, never stored:
    @Mapping(target = "notificationType", ignore = true)
    @Mapping(target = "forced", ignore = true)
    // the follow-up date arrives with the creation and belongs to the cockpit afterwards. A user
    // sets it there and no workflow system hears about it
    @Mapping(target = "followUpDate", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "candidateUsers", ignore = true)
    @Mapping(target = "candidateGroups", ignore = true)
    @Mapping(target = "excludedCandidateUsers", ignore = true)
    @Mapping(target = "admittedUsers", ignore = true)
    @Mapping(target = "title", source = "titleMap")
    @Mapping(target = "workflowTitle", source = "workflowTitleMap")
    @Mapping(target = "taskDefinitionTitle", source = "taskDefinitionTitleMap")
    @Mapping(target = "details", source = "details", qualifiedByName = DETAILS_MAPPING)
    protected abstract UserTask mapEndedTask(UserTaskCreatedOrUpdatedEvent event, @MappingTarget UserTask result);

    /**
     * Maps a completed or cancelled event onto the stored user task, keeping the business data and
     * the titles the cockpit already has where the end reports none.
     * <p>
     * The sender fills the details field whether it has anything to put in it or not, and a
     * protobuf map says nothing about presence either way. So an empty map is how an end of a task
     * the BPMS can no longer describe looks here. The three titles arrive the same way, and mapping
     * them would leave a finished task without a name in the list ({@link WhatAnEndReports}, and
     * see decision 19 in the repository's DECISIONS.md).
     *
     * @param event The end as it was reported
     * @param result The stored task, changed in place
     * @return The stored task
     */
    public UserTask toEndedTask(
            final UserTaskCreatedOrUpdatedEvent event,
            @MappingTarget final UserTask result) {

        final var storedDetails = WhatAnEndReports.before(result.getDetails());
        final var storedFulltextSearch = result.getDetailsFulltextSearch();
        final var storedTitle = WhatAnEndReports.before(result.getTitle());
        final var storedWorkflowTitle = WhatAnEndReports.before(result.getWorkflowTitle());
        final var storedTaskDefinitionTitle = WhatAnEndReports.before(result.getTaskDefinitionTitle());
        final var task = mapEndedTask(event, result);
        task.setDetails(WhatAnEndReports.whatToStore(task.getDetails(), storedDetails));
        task.setDetailsFulltextSearch(
                WhatAnEndReports.whatToStore(task.getDetailsFulltextSearch(), storedFulltextSearch));
        task.setTitle(WhatAnEndReports.whatToStore(task.getTitle(), storedTitle));
        task.setWorkflowTitle(WhatAnEndReports.whatToStore(task.getWorkflowTitle(), storedWorkflowTitle));
        task.setTaskDefinitionTitle(
                WhatAnEndReports.whatToStore(task.getTaskDefinitionTitle(), storedTaskDefinitionTitle));
        return task;

    }

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
