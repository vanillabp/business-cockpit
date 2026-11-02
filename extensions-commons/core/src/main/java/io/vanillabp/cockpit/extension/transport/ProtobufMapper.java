package io.vanillabp.cockpit.extension.transport;

import java.time.OffsetDateTime;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Timestamp;

import io.vanillabp.cockpit.bpms.api.protobuf.v1.BcEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.GroupHierarchy;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.NotificationDelivery;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskActivatedEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskSuspendedEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowActivatedEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowSuspendedEvent;
import io.vanillabp.cockpit.extension.event.RegisterWorkflowModuleEvent;
import io.vanillabp.cockpit.extension.event.UserTaskEvent;
import io.vanillabp.cockpit.extension.event.WorkflowEvent;
import io.vanillabp.cockpit.extension.spi.UserTaskEventKind;
import io.vanillabp.cockpit.extension.spi.WorkflowEventKind;

/**
 * Turns an event into the protobuf message the Kafka transport sends, wrapped in the
 * <code>BcEvent</code> envelope the cockpit server reads one topic of.
 * <p>
 * The envelope has one slot per kind of event. The slots ending in <code>V11</code> are the
 * ones this version of the API uses, and the older slots stay in the schema so that a cockpit
 * server still reading them keeps working. The message inside is the same for all four kinds of
 * user-task event, which is why one mapper serves them all. What differs is the slot and the
 * <code>updated</code> flag. A suspension and an activation have messages of their own, which
 * carry the identifiers, the moment and the person who suspended or activated the instance.
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
   * Creates the mapper.
   *
   * @param objectMapper The mapper turning the business data of an event into a tree
   */
  public ProtobufMapper(
      final ObjectMapper objectMapper) {

    this.objectMapper = objectMapper;

  }

  /**
   * Turns a user-task event into the protobuf envelope the Kafka transport sends.
   *
   * @param event The user-task event
   * @return The envelope to send
   */
  public BcEvent map(
      final UserTaskEvent event) {

    final var envelope = BcEvent.newBuilder();
    switch (event.getEventKind()) {
      case CREATED -> envelope.setUserTaskCreatedV11(userTaskMessage(event));
      case UPDATED -> envelope.setUserTaskUpdatedV11(userTaskMessage(event));
      case COMPLETED -> envelope.setUserTaskCompletedV11(userTaskMessage(event));
      case CANCELED -> envelope.setUserTaskCancelledV11(userTaskMessage(event));
      case SUSPENDED -> envelope.setUserTaskSuspended(userTaskSuspendedMessage(event));
      case ACTIVATED -> envelope.setUserTaskActivated(userTaskActivatedMessage(event));
    }
    return envelope.build();

  }

  /**
   * Turns a workflow event into the protobuf envelope the Kafka transport sends.
   *
   * @param event The workflow event
   * @return The envelope to send
   */
  public BcEvent map(
      final WorkflowEvent event) {

    final var envelope = BcEvent.newBuilder();
    switch (event.getEventKind()) {
      case CREATED -> envelope.setWorkflowCreatedV11(workflowMessage(event));
      case UPDATED -> envelope.setWorkflowUpdatedV11(workflowMessage(event));
      case COMPLETED -> envelope.setWorkflowCompletedV11(workflowMessage(event));
      case CANCELLED -> envelope.setWorkflowCancelledV11(workflowMessage(event));
      case SUSPENDED -> envelope.setWorkflowSuspended(workflowSuspendedMessage(event));
      case ACTIVATED -> envelope.setWorkflowActivated(workflowActivatedMessage(event));
    }
    return envelope.build();

  }

  /**
   * Turns the registration of a workflow module into the protobuf envelope the Kafka transport
   * sends.
   *
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

  // the two messages are alike field by field, but protobuf generated two classes with no common
  // type, so each one is built on its own
  private UserTaskSuspendedEvent userTaskSuspendedMessage(
      final UserTaskEvent event) {

    final var builder = UserTaskSuspendedEvent.newBuilder();
    builder.setId(event.getEventId());
    builder.setApiVersion(API_VERSION);
    builder.setUserTaskId(event.getUserTaskId());
    builder.setTimestamp(timestampOf(event.getTimestamp()));
    Optional.ofNullable(event.getInitiator()).ifPresent(builder::setInitiator);
    Optional.ofNullable(event.getSource()).ifPresent(builder::setSource);
    Optional.ofNullable(event.getComment()).ifPresent(builder::setComment);
    Optional.ofNullable(event.getSuspendedBy()).ifPresent(builder::setSuspendedBy);
    return builder.build();

  }

  private UserTaskActivatedEvent userTaskActivatedMessage(
      final UserTaskEvent event) {

    final var builder = UserTaskActivatedEvent.newBuilder();
    builder.setId(event.getEventId());
    builder.setApiVersion(API_VERSION);
    builder.setUserTaskId(event.getUserTaskId());
    builder.setTimestamp(timestampOf(event.getTimestamp()));
    Optional.ofNullable(event.getInitiator()).ifPresent(builder::setInitiator);
    Optional.ofNullable(event.getSource()).ifPresent(builder::setSource);
    Optional.ofNullable(event.getComment()).ifPresent(builder::setComment);
    Optional.ofNullable(event.getSuspendedBy()).ifPresent(builder::setSuspendedBy);
    return builder.build();

  }

  private WorkflowSuspendedEvent workflowSuspendedMessage(
      final WorkflowEvent event) {

    final var builder = WorkflowSuspendedEvent.newBuilder();
    builder.setId(event.getEventId());
    builder.setApiVersion(API_VERSION);
    builder.setWorkflowId(event.getWorkflowId());
    builder.setTimestamp(timestampOf(event.getTimestamp()));
    Optional.ofNullable(event.getInitiator()).ifPresent(builder::setInitiator);
    Optional.ofNullable(event.getSource()).ifPresent(builder::setSource);
    Optional.ofNullable(event.getComment()).ifPresent(builder::setComment);
    Optional.ofNullable(event.getSuspendedBy()).ifPresent(builder::setSuspendedBy);
    return builder.build();

  }

  private WorkflowActivatedEvent workflowActivatedMessage(
      final WorkflowEvent event) {

    final var builder = WorkflowActivatedEvent.newBuilder();
    builder.setId(event.getEventId());
    builder.setApiVersion(API_VERSION);
    builder.setWorkflowId(event.getWorkflowId());
    builder.setTimestamp(timestampOf(event.getTimestamp()));
    Optional.ofNullable(event.getInitiator()).ifPresent(builder::setInitiator);
    Optional.ofNullable(event.getSource()).ifPresent(builder::setSource);
    Optional.ofNullable(event.getComment()).ifPresent(builder::setComment);
    Optional.ofNullable(event.getSuspendedBy()).ifPresent(builder::setSuspendedBy);
    return builder.build();

  }

  private UserTaskCreatedOrUpdatedEvent userTaskMessage(
      final UserTaskEvent event) {

    final var builder = UserTaskCreatedOrUpdatedEvent.newBuilder();
    builder.setId(event.getEventId());
    builder.setApiVersion(API_VERSION);
    builder.setUserTaskId(event.getUserTaskId());
    builder.setTimestamp(timestampOf(event.getTimestamp()));
    builder.setUpdated(event.getEventKind() != UserTaskEventKind.CREATED);
    Optional.ofNullable(event.getBpmnProcessId()).ifPresent(builder::setBpmnProcessId);
    Optional.ofNullable(event.getTaskDefinition()).ifPresent(builder::setTaskDefinition);
    Optional.ofNullable(event.getTitle()).ifPresent(builder::putAllTitle);
    Optional.ofNullable(event.getWorkflowModuleId()).ifPresent(builder::setWorkflowModuleId);
    Optional.ofNullable(event.getUiUriPath()).ifPresent(builder::setUiUriPath);
    Optional.ofNullable(event.getUiUriType()).ifPresent(builder::setUiUriType);
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
    Optional.ofNullable(event.getAdmittedUsers()).ifPresent(builder::addAllAdmittedUsers);
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
    // the cockpit reads it only from an end, so it travels only with one
    if ((event.getEventKind() == UserTaskEventKind.COMPLETED) || (event.getEventKind() == UserTaskEventKind.CANCELED)) {
      Optional
          .ofNullable(event.getCreatedAt())
          .ifPresent(createdAt -> builder.setCreatedAt(timestampOf(createdAt)));
    }
    return builder.build();

  }

  private WorkflowCreatedOrUpdatedEvent workflowMessage(
      final WorkflowEvent event) {

    final var builder = WorkflowCreatedOrUpdatedEvent.newBuilder();
    builder.setId(event.getEventId());
    builder.setApiVersion(API_VERSION);
    builder.setWorkflowId(event.getWorkflowId());
    builder.setTimestamp(timestampOf(event.getTimestamp()));
    builder.setUpdated(event.getEventKind() != WorkflowEventKind.CREATED);
    // the cockpit reads it only from an end, so it travels only with one
    if ((event.getEventKind() == WorkflowEventKind.COMPLETED) || (event
        .getEventKind() == WorkflowEventKind.CANCELLED)) {
      Optional
          .ofNullable(event.getCreatedAt())
          .ifPresent(createdAt -> builder.setCreatedAt(timestampOf(createdAt)));
    }
    Optional.ofNullable(event.getBpmnProcessId()).ifPresent(builder::setBpmnProcessId);
    Optional.ofNullable(event.getWorkflowModuleId()).ifPresent(builder::setWorkflowModuleId);
    Optional.ofNullable(event.getUiUriPath()).ifPresent(builder::setUiUriPath);
    Optional.ofNullable(event.getUiUriType()).ifPresent(builder::setUiUriType);
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
