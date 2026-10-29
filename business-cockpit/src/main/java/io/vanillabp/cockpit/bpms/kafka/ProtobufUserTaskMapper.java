package io.vanillabp.cockpit.bpms.kafka;

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

/**
 * Maps the protobuf user task events which arrive by Kafka onto the user tasks the cockpit stores.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.DEFAULT)
public abstract class ProtobufUserTaskMapper {

  private static final String DETAILS_MAPPING = "detailsMapping";
  private static final String PERSON_MAPPING = "personMapping";
  private static final String GROUP_MAPPING = "groupMapping";

  @Autowired
  private PersonAndGroupMapper personAndGroupMapper;

  /**
   * Creates the mapper. MapStruct generates the subclass, and the Kafka configuration creates it
   * as a Spring bean.
   */
  public ProtobufUserTaskMapper() {
  }

  /**
   * Turns the id of a user, as a workflow module reports it, into a person of the cockpit.
   *
   * @param userId The id of the user
   * @return The person
   */
  @Named(PERSON_MAPPING)
  public Person toPerson(
      final String userId) {
    return personAndGroupMapper.toModelPerson(userId);
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

  // proto3 enums carry an extra UNRECOGNIZED constant; map it (and the default USER_CONFIG) to
  // null, which the notification logic interprets as USER_CONFIG.
  @ValueMapping(source = "UNRECOGNIZED", target = MappingConstants.NULL)
  @ValueMapping(source = "USER_CONFIG", target = MappingConstants.NULL)
  abstract io.vanillabp.spi.cockpit.usertask.NotificationDelivery mapNotificationDelivery(
      io.vanillabp.cockpit.bpms.api.protobuf.v1.NotificationDelivery delivery);

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
  public abstract UserTask toNewTask(
      UserTaskCreatedOrUpdatedEvent event);

  /**
   * Maps an update event onto the stored user task.
   * <p>
   * Who sees the task is mapped like everything else. For a task the cockpit holds already,
   * {@link io.vanillabp.cockpit.bpms.WhoSeesAUserTask} puts the stored assignee and candidates
   * back, because only the report which created the task says who sees it. The admitted users
   * are the exception: every report may set them.
   *
   * @param event The report of the updated task
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
  @Mapping(target = "dangling", ignore = true)
  @Mapping(target = "notificationType", ignore = true)
  @Mapping(target = "forced", ignore = true)
  @Mapping(target = "endReason", ignore = true)
  @Mapping(target = "targetGroups", ignore = true)
  @Mapping(target = "followUpDate", ignore = true)
  @Mapping(target = "assignee", source = "assignee", qualifiedByName = PERSON_MAPPING)
  @Mapping(target = "candidateUsers", source = "candidateUsersList", qualifiedByName = PERSON_MAPPING)
  @Mapping(target = "candidateGroups", source = "candidateGroupsList", qualifiedByName = GROUP_MAPPING)
  @Mapping(target = "excludedCandidateUsers", source = "excludedCandidateUsersList", qualifiedByName = PERSON_MAPPING)
  @Mapping(target = "admittedUsers", source = "admittedUsersList", qualifiedByName = PERSON_MAPPING)
  @Mapping(target = "title", source = "titleMap")
  @Mapping(target = "workflowTitle", source = "workflowTitleMap")
  @Mapping(target = "taskDefinitionTitle", source = "taskDefinitionTitleMap")
  @Mapping(target = "details", source = "details", qualifiedByName = DETAILS_MAPPING)
  public abstract UserTask toUpdatedTask(
      UserTaskCreatedOrUpdatedEvent event,
      @MappingTarget UserTask result);

  /**
   * Maps a completed or cancelled event onto the stored user task.
   * <p>
   * Who sees the task is mapped, so that an end which creates the task says who sees it. For a
   * task the cockpit holds already, {@link io.vanillabp.cockpit.bpms.WhoSeesAUserTask} keeps the
   * stored assignee and candidates. The notification poller needs the former assignee to tell
   * them that somebody else completed the task or that the process cancelled it.
   * <p>
   * This is the mapping MapStruct generates. Call
   * {@link #toEndedTask(UserTaskCreatedOrUpdatedEvent, UserTask)} instead, which also keeps what
   * the end does not report.
   *
   * @param event The end as it was reported
   * @param result The stored task, changed in place
   * @return The stored task
   */
  // an end overwrites what it reports and leaves the rest of the stored task as it is. See
  // decision 19 in the repository's DECISIONS.md. A field the sender left out is declared
  // optional in the protobuf schema and arrives as null. This strategy handles that. The titles
  // and the business data arrive empty instead, and toEndedTask handles those.
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
  @Mapping(target = "knownFromItsEndAlone", ignore = true)
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
  // who sees the task is mapped, because an end which arrives before the creation creates the
  // task and has to say who sees it. For a task the cockpit holds already, WhoSeesAUserTask puts
  // the stored values back. The admitted users may change with every report. Like any list of
  // an end, an empty one counts as not reported, and toEndedTask says so
  @Mapping(target = "assignee", source = "assignee", qualifiedByName = PERSON_MAPPING)
  @Mapping(target = "candidateUsers", source = "candidateUsersList", qualifiedByName = PERSON_MAPPING)
  @Mapping(target = "candidateGroups", source = "candidateGroupsList", qualifiedByName = GROUP_MAPPING)
  @Mapping(target = "excludedCandidateUsers", source = "excludedCandidateUsersList", qualifiedByName = PERSON_MAPPING)
  @Mapping(target = "admittedUsers", source = "admittedUsersList", qualifiedByName = PERSON_MAPPING)
  @Mapping(target = "title", source = "titleMap")
  @Mapping(target = "workflowTitle", source = "workflowTitleMap")
  @Mapping(target = "taskDefinitionTitle", source = "taskDefinitionTitleMap")
  @Mapping(target = "details", source = "details", qualifiedByName = DETAILS_MAPPING)
  protected abstract UserTask mapEndedTask(
      UserTaskCreatedOrUpdatedEvent event,
      @MappingTarget UserTask result);

  /**
   * Maps a completed or cancelled event onto the stored user task, keeping the business data and
   * the titles the cockpit already has where the end reports none.
   * <p>
   * The sender always fills the details field, even with nothing in it. A protobuf map cannot
   * tell "left out" from "empty". So if the BPMS can no longer describe the task, its end arrives
   * here with an empty map. The three titles arrive the same way. Mapping them would leave a
   * finished task without a name in the list ({@link WhatAnEndReports}, and see decision 19 in
   * the repository's DECISIONS.md).
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
    final var storedAdmittedUsers = WhatAnEndReports.before(result.getAdmittedUsers());
    final var task = mapEndedTask(event, result);
    task.setAdmittedUsers(WhatAnEndReports.whatToStore(task.getAdmittedUsers(), storedAdmittedUsers));
    task.setDetails(WhatAnEndReports.whatToStore(task.getDetails(), storedDetails));
    task.setDetailsFulltextSearch(
        WhatAnEndReports.whatToStore(task.getDetailsFulltextSearch(), storedFulltextSearch));
    task.setTitle(WhatAnEndReports.whatToStore(task.getTitle(), storedTitle));
    task.setWorkflowTitle(WhatAnEndReports.whatToStore(task.getWorkflowTitle(), storedWorkflowTitle));
    task.setTaskDefinitionTitle(
        WhatAnEndReports.whatToStore(task.getTaskDefinitionTitle(), storedTaskDefinitionTitle));
    return task;

  }

  /**
   * Turns a protobuf timestamp into a date and time in the time zone of the cockpit.
   *
   * @param value The timestamp as it was reported
   * @return The same moment
   */
  public OffsetDateTime map(
      Timestamp value) {
    return ProtobufHelper.map(value);
  }

  /**
   * Copies a repeated protobuf string field into a plain list.
   *
   * @param stringList The strings as they were reported
   * @return The same strings
   */
  public List<String> map(
      ProtocolStringList stringList) {
    return stringList.stream().toList();
  }

  /**
   * Turns the business data of a report into the map the cockpit stores.
   *
   * @param detailsMap The business data as it was reported
   * @return The business data as plain Java values
   */
  @Named(DETAILS_MAPPING)
  protected Map<String, Object> map(
      final DetailsMap detailsMap) {
    return DetailsMapper.mapMapValue(detailsMap);
  }

}
