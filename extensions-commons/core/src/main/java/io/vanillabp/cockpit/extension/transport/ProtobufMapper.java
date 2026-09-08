package io.vanillabp.cockpit.extension.transport;

import java.time.OffsetDateTime;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Timestamp;

import io.vanillabp.cockpit.bpms.api.protobuf.v1.BcEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.GroupHierarchy;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.NotificationDelivery;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.extension.event.RegisterWorkflowModuleEvent;
import io.vanillabp.cockpit.extension.event.UserTaskEvent;
import io.vanillabp.cockpit.extension.event.WorkflowEvent;

/**
 * Turns an event into the protobuf message the Kafka transport sends, wrapped in the
 * <code>BcEvent</code> envelope the cockpit server reads one topic of.
 * <p>
 * The envelope has one slot per kind of event, and the slots ending in <code>V11</code> are
 * the ones this version of the API uses; the older slots stay in the schema so that a cockpit
 * server still reading them keeps working. The message inside is the same for all four kinds
 * of user-task event, which is why one mapper serves them all - what differs is the slot and
 * the <code>updated</code> flag.
 * <p>
 * Every field is set through {@link Optional}, because a protobuf setter refuses
 * <code>null</code> while nearly everything a workflow module reports is optional.
 */
public final class ProtobufMapper {

  /**
   * The version of the cockpit's BPMS API these messages are built for. It travels with every
   * message so that a server can tell what it is reading.
   */
  public static final String API_VERSION = "1.0";

  private final ObjectMapper objectMapper;

  /**
   * @param objectMapper The mapper turning the business data of an event into a tree
   */
  public ProtobufMapper(
      final ObjectMapper objectMapper) {

    this.objectMapper = objectMapper;

  }

  /**
   * @param event The user-task event
   * @return The envelope to send
   */
  public BcEvent map(
      final UserTaskEvent event) {

    final var message = userTaskMessage(event);
    final var envelope = BcEvent.newBuilder();
    switch (event.getEventKind()) {
      case CREATED -> envelope.setUserTaskCreatedV11(message);
      case UPDATED -> envelope.setUserTaskUpdatedV11(message);
      case COMPLETED -> envelope.setUserTaskCompletedV11(message);
      case CANCELED -> envelope.setUserTaskCancelledV11(message);
    }
    return envelope.build();

  }

  /**
   * @param event The workflow event
   * @return The envelope to send
   */
  public BcEvent map(
      final WorkflowEvent event) {

    final var message = workflowMessage(event);
    final var envelope = BcEvent.newBuilder();
    switch (event.getEventKind()) {
      case CREATED -> envelope.setWorkflowCreatedV11(message);
      case UPDATED -> envelope.setWorkflowUpdatedV11(message);
      case COMPLETED -> envelope.setWorkflowCompletedV11(message);
      case CANCELLED -> envelope.setWorkflowCancelledV11(message);
    }
    return envelope.build();

  }

  /**
   * @param event The registration of a workflow module
   * @return The envelope to send
   */
  public BcEvent map(
      final RegisterWorkflowModuleEvent event) {

    final var builder = io.vanillabp.cockpit.bpms.api.protobuf.v1.RegisterWorkflowModuleEvent
        .newBuilder();
    builder.setId(event.eventId());
    builder.setTimestamp(timestampOf(event.timestamp()));
    builder.setWorkflowModuleId(event.workflowModuleId());
    builder.setUri(event.uri());
    Optional.ofNullable(event.source()).ifPresent(builder::setSource);
    Optional
        .ofNullable(event.taskProviderApiUriPath())
        .ifPresent(builder::setTaskProviderApiUriPath);
    Optional
        .ofNullable(event.workflowProviderApiUriPath())
        .ifPresent(builder::setWorkflowProviderApiUriPath);
    builder.addAllAccessibleToGroups(event.accessibleToGroups());
    event
        .groupHierarchy()
        .forEach((
            group,
            targets) -> builder
                .addGroupHierarchy(
                    GroupHierarchy.newBuilder().setGroup(group).addAllTarget(targets).build()));
    return BcEvent.newBuilder().setRegisterWorkflowModule(builder.build()).build();

  }

  private UserTaskCreatedOrUpdatedEvent userTaskMessage(
      final UserTaskEvent event) {

    final var builder = UserTaskCreatedOrUpdatedEvent.newBuilder();
    builder.setId(event.getEventId());
    builder.setApiVersion(API_VERSION);
    builder.setUserTaskId(event.getUserTaskId());
    builder.setTimestamp(timestampOf(event.getTimestamp()));
    builder.setUpdated(event.getEventKind() != io.vanillabp.cockpit.extension.spi.UserTaskEventKind.CREATED);
    Optional.ofNullable(event.getBpmnProcessId()).ifPresent(builder::setBpmnProcessId);
    Optional.ofNullable(event.getTaskDefinition()).ifPresent(builder::setTaskDefinition);
    Optional.ofNullable(event.getTitle()).ifPresent(builder::putAllTitle);
    Optional.ofNullable(event.getWorkflowModuleId()).ifPresent(builder::setWorkflowModuleId);
    Optional.ofNullable(event.getUiUriPath()).ifPresent(builder::setUiUriPath);
    Optional
        .ofNullable(event.getUiUriType())
        .ifPresent(uiUriType -> builder.setUiUriType(uiUriType.name()));
    Optional.ofNullable(event.getInitiator()).ifPresent(builder::setInitiator);
    Optional.ofNullable(event.getSource()).ifPresent(builder::setSource);
    Optional.ofNullable(event.getComment()).ifPresent(builder::setComment);
    Optional
        .ofNullable(event.getBpmnProcessVersion())
        .ifPresent(builder::setBpmnProcessVersion);
    Optional.ofNullable(event.getWorkflowTitle()).ifPresent(builder::putAllWorkflowTitle);
    Optional.ofNullable(event.getWorkflowId()).ifPresent(builder::setWorkflowId);
    Optional.ofNullable(event.getSubWorkflowId()).ifPresent(builder::setSubWorkflowId);
    Optional.ofNullable(event.getBusinessId()).ifPresent(builder::setBusinessId);
    Optional.ofNullable(event.getBpmnTaskId()).ifPresent(builder::setBpmnTaskId);
    Optional
        .ofNullable(event.getTaskDefinitionTitle())
        .ifPresent(builder::putAllTaskDefinitionTitle);
    Optional.ofNullable(event.getAssignee()).ifPresent(builder::setAssignee);
    Optional.ofNullable(event.getCandidateUsers()).ifPresent(builder::addAllCandidateUsers);
    Optional.ofNullable(event.getCandidateGroups()).ifPresent(builder::addAllCandidateGroups);
    Optional
        .ofNullable(event.getExcludedCandidateUsers())
        .ifPresent(builder::addAllExcludedCandidateUsers);
    Optional
        .ofNullable(event.getDueDate())
        .ifPresent(dueDate -> builder.setDueDate(timestampOf(dueDate)));
    Optional
        .ofNullable(event.getFollowUpDate())
        .ifPresent(followUpDate -> builder.setFollowUpDate(timestampOf(followUpDate)));
    builder.setDetails(DetailsConverter.toProtobuf(objectMapper, event.getDetails()));
    Optional
        .ofNullable(event.getDetailsFulltextSearch())
        .ifPresent(builder::setDetailsFulltextSearch);
    Optional
        .ofNullable(event.getNotificationDelivery())
        .ifPresent(
            delivery -> builder.setNotificationDelivery(NotificationDelivery.valueOf(delivery.name())));
    return builder.build();

  }

  private WorkflowCreatedOrUpdatedEvent workflowMessage(
      final WorkflowEvent event) {

    final var builder = WorkflowCreatedOrUpdatedEvent.newBuilder();
    builder.setId(event.getEventId());
    builder.setApiVersion(API_VERSION);
    builder.setWorkflowId(event.getWorkflowId());
    builder.setTimestamp(timestampOf(event.getTimestamp()));
    builder.setUpdated(event.getEventKind() != io.vanillabp.cockpit.extension.spi.WorkflowEventKind.CREATED);
    Optional.ofNullable(event.getBpmnProcessId()).ifPresent(builder::setBpmnProcessId);
    Optional.ofNullable(event.getWorkflowModuleId()).ifPresent(builder::setWorkflowModuleId);
    Optional.ofNullable(event.getUiUriPath()).ifPresent(builder::setUiUriPath);
    Optional
        .ofNullable(event.getUiUriType())
        .ifPresent(uiUriType -> builder.setUiUriType(uiUriType.name()));
    Optional.ofNullable(event.getBusinessId()).ifPresent(builder::setBusinessId);
    Optional.ofNullable(event.getInitiator()).ifPresent(builder::setInitiator);
    Optional.ofNullable(event.getSource()).ifPresent(builder::setSource);
    Optional.ofNullable(event.getTitle()).ifPresent(builder::putAllTitle);
    Optional.ofNullable(event.getComment()).ifPresent(builder::setComment);
    Optional
        .ofNullable(event.getBpmnProcessVersion())
        .ifPresent(builder::setBpmnProcessVersion);
    builder.setDetails(DetailsConverter.toProtobuf(objectMapper, event.getDetails()));
    Optional
        .ofNullable(event.getDetailsFulltextSearch())
        .ifPresent(builder::setDetailsFulltextSearch);
    Optional
        .ofNullable(event.getAccessibleToUsers())
        .ifPresent(builder::addAllAccessibleToUsers);
    Optional
        .ofNullable(event.getAccessibleToGroups())
        .ifPresent(builder::addAllAccessibleToGroups);
    return builder.build();

  }

  private static Timestamp timestampOf(
      final OffsetDateTime timestamp) {

    final var instant = timestamp.toInstant();
    return Timestamp
        .newBuilder()
        .setSeconds(instant.getEpochSecond())
        .setNanos(instant.getNano())
        .build();

  }

}
