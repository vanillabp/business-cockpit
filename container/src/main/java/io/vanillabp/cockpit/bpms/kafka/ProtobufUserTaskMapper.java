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
    @Mapping(target = "details", source = "details", qualifiedByName = DETAILS_MAPPING)
    public abstract UserTask toNewTask(UserTaskCreatedOrUpdatedEvent event);

    /**
     * Maps an update event onto the stored user task.
     * <p>
     * The assignee is kept if the event does not provide one: a task is taken over in the cockpit,
     * so the assignment is cockpit-owned state no workflow system knows about. Without
     * {@link NullValuePropertyMappingStrategy#IGNORE} an event omitting the assignee would silently
     * drop the takeover (and with it the recipient of the completion notification).
     * <p>
     * The candidate users are kept for the same reason: assigning a task in the cockpit adds a
     * personal candidate the workflow system does not know, and a repeated protobuf field carries
     * no presence information - an empty list cannot be told apart from "not provided", and events
     * do report the candidates known to the engine, which would drop the cockpit-side assignment.
     * Deliberate consequence: candidate users are taken from the create event only; later
     * changes made by the process do not reach the cockpit. Candidate groups and excluded
     * candidates stay mapped - they are never written on the cockpit side.
     */
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
    @Mapping(target = "details", source = "details", qualifiedByName = DETAILS_MAPPING)
    public abstract UserTask toUpdatedTask(UserTaskCreatedOrUpdatedEvent event, @MappingTarget UserTask result);

    /**
     * Maps a completed or cancelled event onto the stored user task.
     * <p>
     * Assignee and candidates are left untouched: the lifecycle event of an ended task does not
     * carry them (the Camunda adapters do not fill them), and mapping the absent values would wipe
     * exactly the information the notification poller needs to tell the former assignee that
     * somebody else completed or the process cancelled the task. Repeated fields have no presence
     * information in protobuf, so an empty list cannot be told apart from "not provided" -
     * therefore they are ignored here rather than mapped with a null-value strategy.
     */
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
    @Mapping(target = "dangling", ignore = true)
    @Mapping(target = "notificationType", ignore = true)
    @Mapping(target = "forced", ignore = true)
    @Mapping(target = "endReason", ignore = true)
    @Mapping(target = "targetGroups", ignore = true)
    @Mapping(target = "followUpDate", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "candidateUsers", ignore = true)
    @Mapping(target = "candidateGroups", ignore = true)
    @Mapping(target = "excludedCandidateUsers", ignore = true)
    @Mapping(target = "details", source = "details", qualifiedByName = DETAILS_MAPPING)
    public abstract UserTask toEndedTask(UserTaskCreatedOrUpdatedEvent event, @MappingTarget UserTask result);

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
