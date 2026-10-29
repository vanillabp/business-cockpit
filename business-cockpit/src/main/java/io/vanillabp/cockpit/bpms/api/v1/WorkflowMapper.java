package io.vanillabp.cockpit.bpms.api.v1;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import io.vanillabp.cockpit.workflowlist.model.Workflow;

/**
 * Maps the workflow events of version 1 of the BPMS API onto the workflows, the cases, the
 * cockpit stores.
 */
@Mapper(implementationName = "WorkflowMapperV1Impl")
public abstract class WorkflowMapper {

  private static final String PERSON_MAPPING = "personMapping";
  private static final String GROUP_MAPPING = "groupMapping";

  @Autowired
  private PersonAndGroupMapper personAndGroupMapper;

  /**
   * Creates the mapper. MapStruct generates the subclass which Spring creates.
   */
  public WorkflowMapper() {
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
    // null id is hidden by the GUI, but every comparison of ids stumbles over it
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
  @Mapping(target = "targetGroups", ignore = true)
  @Mapping(target = "dangling", ignore = true)
  @Mapping(target = "initiator", source = "initiator", qualifiedByName = PERSON_MAPPING)
  @Mapping(target = "accessibleToUsers", source = "accessibleToUsers", qualifiedByName = PERSON_MAPPING)
  @Mapping(target = "accessibleToGroups", source = "accessibleToGroups", qualifiedByName = GROUP_MAPPING)
  public abstract Workflow toNewWorkflow(
      WorkflowCreatedOrUpdatedEvent event);

  /**
   * Maps a report onto a workflow the cockpit holds already. The id and the creation stay as they
   * are.
   *
   * @param event The report of an updated workflow
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
  @Mapping(target = "targetGroups", ignore = true)
  @Mapping(target = "dangling", ignore = true)
  @Mapping(target = "initiator", source = "initiator", qualifiedByName = PERSON_MAPPING)
  @Mapping(target = "accessibleToUsers", source = "accessibleToUsers", qualifiedByName = PERSON_MAPPING)
  @Mapping(target = "accessibleToGroups", source = "accessibleToGroups", qualifiedByName = GROUP_MAPPING)
  public abstract Workflow toUpdatedWorkflow(
      WorkflowCreatedOrUpdatedEvent event,
      @MappingTarget Workflow result);

}
