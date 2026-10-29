package io.vanillabp.cockpit.bpms.kafka;

import java.time.OffsetDateTime;
import java.util.Map;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.protobuf.Timestamp;

import io.vanillabp.cockpit.bpms.WhatAnEndReports;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.DetailsMap;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import io.vanillabp.cockpit.util.protobuf.ProtobufHelper;
import io.vanillabp.cockpit.workflowlist.model.Workflow;

/**
 * Maps the protobuf workflow events which arrive by Kafka onto the workflows, the cases, the
 * cockpit stores.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.DEFAULT)
public abstract class ProtobufWorkflowMapper {

  private static final String DETAILS_MAPPING = "detailsMapping";
  private static final String PERSON_MAPPING = "personMapping";
  private static final String GROUP_MAPPING = "groupMapping";

  @Autowired
  private PersonAndGroupMapper personAndGroupMapper;

  /**
   * Creates the mapper. MapStruct generates the subclass, and the Kafka configuration creates it
   * as a Spring bean.
   */
  public ProtobufWorkflowMapper() {
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

  /**
   * Builds a new workflow from a report the cockpit knows nothing about yet.
   *
   * @param event The report of a created or an updated workflow
   * @return The new workflow, not stored yet
   */
  @Mapping(target = "id", source = "workflowId")
  @Mapping(target = "version", ignore = true)
  // maintained by the cockpit itself, never taken from an event:
  @Mapping(target = "reportedAt", ignore = true)
  // the event's timestamp, stamped where reports are weighed against each other:
  @Mapping(target = "latestEventAt", ignore = true)
  @Mapping(target = "createdAt", source = "timestamp")
  @Mapping(target = "knownFromItsEndAlone", ignore = true)
  @Mapping(target = "updatedAt", source = "timestamp")
  @Mapping(target = "updatedBy", source = "initiator")
  @Mapping(target = "endedAt", ignore = true)
  @Mapping(target = "dangling", ignore = true)
  @Mapping(target = "targetGroups", ignore = true)
  @Mapping(target = "initiator", source = "initiator", qualifiedByName = PERSON_MAPPING)
  @Mapping(target = "accessibleToUsers", source = "accessibleToUsersList", qualifiedByName = PERSON_MAPPING)
  @Mapping(target = "accessibleToGroups", source = "accessibleToGroupsList", qualifiedByName = GROUP_MAPPING)
  // protoc deprecates the plain getter of a map field and points at the ...Map() one. It names
  // a repeated field ...List in the same way. Naming the source here keeps the deprecated getter
  // out of the generated mapper. Both getters return the same map.
  @Mapping(target = "title", source = "titleMap")
  @Mapping(target = "details", source = "details", qualifiedByName = DETAILS_MAPPING)
  public abstract Workflow toNewWorkflow(
      WorkflowCreatedOrUpdatedEvent event);

  /**
   * Maps an update onto a workflow the cockpit holds already. The id and the creation stay as they
   * are.
   *
   * @param event The report of the updated workflow
   * @param result The stored workflow, changed in place
   * @return The stored workflow
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "version", ignore = true)
  // maintained by the cockpit itself, never taken from an event:
  @Mapping(target = "reportedAt", ignore = true)
  // the event's timestamp, stamped where reports are weighed against each other:
  @Mapping(target = "latestEventAt", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "knownFromItsEndAlone", ignore = true)
  @Mapping(target = "updatedAt", source = "timestamp")
  @Mapping(target = "updatedBy", source = "initiator")
  @Mapping(target = "endedAt", ignore = true)
  @Mapping(target = "dangling", ignore = true)
  @Mapping(target = "targetGroups", ignore = true)
  @Mapping(target = "initiator", source = "initiator", qualifiedByName = PERSON_MAPPING)
  @Mapping(target = "accessibleToUsers", source = "accessibleToUsersList", qualifiedByName = PERSON_MAPPING)
  @Mapping(target = "accessibleToGroups", source = "accessibleToGroupsList", qualifiedByName = GROUP_MAPPING)
  @Mapping(target = "title", source = "titleMap")
  @Mapping(target = "details", source = "details", qualifiedByName = DETAILS_MAPPING)
  public abstract Workflow toUpdatedWorkflow(
      WorkflowCreatedOrUpdatedEvent event,
      @MappingTarget Workflow result);

  /**
   * The mapping of an end which MapStruct generates. Call
   * {@link #toEndedWorkflow(WorkflowCreatedOrUpdatedEvent, Workflow)} instead, which also keeps what
   * the end does not report.
   *
   * @param event The end as it was reported
   * @param result The stored workflow, changed in place
   * @return The stored workflow
   */
  // an end overwrites what it reports and leaves the rest of the stored case as it is. See
  // decision 19 in the repository's DECISIONS.md. Who started the case is declared optional in
  // the protobuf schema and arrives as null. This strategy handles that. The title, the business
  // data and the permissions to open the case arrive empty instead, and toEndedWorkflow handles
  // those.
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  // the record is the one the service looked up, and MongoDB counts its saves:
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "version", ignore = true)
  // maintained by the cockpit itself, never taken from an event:
  @Mapping(target = "reportedAt", ignore = true)
  // the event's timestamp, stamped where reports are weighed against each other:
  @Mapping(target = "latestEventAt", ignore = true)
  // an end does not say when the case began. Guessing it would make the same case look
  // different, depending on which of the two reports arrived first:
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "knownFromItsEndAlone", ignore = true)
  // audit information, replaced by the cockpit's own clock and user whenever the record is saved:
  @Mapping(target = "updatedAt", source = "timestamp")
  @Mapping(target = "updatedBy", source = "initiator")
  // the service sets this one, because an end means more to it than the event says
  @Mapping(target = "endedAt", ignore = true)
  // read from the users and groups the case is accessible to whenever they are asked for:
  @Mapping(target = "dangling", ignore = true)
  @Mapping(target = "targetGroups", ignore = true)
  @Mapping(target = "initiator", source = "initiator", qualifiedByName = PERSON_MAPPING)
  @Mapping(target = "accessibleToUsers", source = "accessibleToUsersList", qualifiedByName = PERSON_MAPPING)
  @Mapping(target = "accessibleToGroups", source = "accessibleToGroupsList", qualifiedByName = GROUP_MAPPING)
  @Mapping(target = "title", source = "titleMap")
  @Mapping(target = "details", source = "details", qualifiedByName = DETAILS_MAPPING)
  protected abstract Workflow mapEndedWorkflow(
      WorkflowCreatedOrUpdatedEvent event,
      @MappingTarget Workflow result);

  /**
   * Maps a completed or cancelled event onto the stored workflow, keeping the business data and
   * the title the cockpit already has where the end reports none.
   * <p>
   * The sender always fills the details field, even with nothing in it. A protobuf map cannot
   * tell "left out" from "empty". So if the BPMS can no longer describe the case, its end arrives
   * here with an empty map. The title arrives the same way. So do the users and groups who may
   * open the case, because a repeated protobuf field cannot tell the two apart either. Mapping
   * those would take away everybody's permission to open a finished case
   * ({@link WhatAnEndReports}, and see decision 19 in the repository's DECISIONS.md).
   *
   * @param event The end as it was reported
   * @param result The stored workflow, changed in place
   * @return The stored workflow
   */
  public Workflow toEndedWorkflow(
      final WorkflowCreatedOrUpdatedEvent event,
      @MappingTarget final Workflow result) {

    final var storedDetails = WhatAnEndReports.before(result.getDetails());
    final var storedFulltextSearch = result.getDetailsFulltextSearch();
    final var storedTitle = WhatAnEndReports.before(result.getTitle());
    final var storedAccessibleToUsers = WhatAnEndReports.before(result.getAccessibleToUsers());
    final var storedAccessibleToGroups = WhatAnEndReports.before(result.getAccessibleToGroups());
    final var workflow = mapEndedWorkflow(event, result);
    workflow.setDetails(WhatAnEndReports.whatToStore(workflow.getDetails(), storedDetails));
    workflow.setDetailsFulltextSearch(
        WhatAnEndReports.whatToStore(workflow.getDetailsFulltextSearch(), storedFulltextSearch));
    workflow.setTitle(WhatAnEndReports.whatToStore(workflow.getTitle(), storedTitle));
    workflow.setAccessibleToUsers(
        WhatAnEndReports.whatToStore(workflow.getAccessibleToUsers(), storedAccessibleToUsers));
    workflow.setAccessibleToGroups(
        WhatAnEndReports.whatToStore(workflow.getAccessibleToGroups(), storedAccessibleToGroups));
    return workflow;

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