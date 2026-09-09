package io.vanillabp.cockpit.extension.transport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.vanillabp.cockpit.bpms.api.v1_1.NotificationDelivery;
import io.vanillabp.cockpit.bpms.api.v1_1.UserTaskCancelledEvent;
import io.vanillabp.cockpit.bpms.api.v1_1.UserTaskCompletedEvent;
import io.vanillabp.cockpit.bpms.api.v1_1.UserTaskCreatedEvent;
import io.vanillabp.cockpit.bpms.api.v1_1.UserTaskUpdatedEvent;
import io.vanillabp.cockpit.extension.config.UiUriType;
import io.vanillabp.cockpit.extension.event.UserTaskEvent;

/**
 * Turns a user-task event into what the cockpit's REST API of version 1.1 accepts.
 * <p>
 * The API declares one schema per kind of event, and the generator therefore produced four
 * classes carrying the same fields. Nothing but the class and the <code>updated</code> flag
 * differs between the four mappings below, which is why they look alike (see decision 9 in the
 * repository's DECISIONS.md); folding them into one
 * would mean converting between the generated classes, and a conversion of a timestamp is
 * exactly the kind of thing that goes wrong silently.
 * <p>
 * Collections are copied on the way out. What travels here belongs to the event, which is
 * still readable by the application's details provider afterwards.
 */
public final class UserTaskRestMapper {

  private UserTaskRestMapper() {
  }

  /**
   * @param event The Created event
   * @return What the cockpit's API is sent
   */
  public static UserTaskCreatedEvent mapCreated(
      final UserTaskEvent event) {

    final var dto = new UserTaskCreatedEvent();
    dto.setId(event.getEventId());
    dto.setUserTaskId(event.getUserTaskId());
    dto.setInitiator(event.getInitiator());
    dto.setTimestamp(event.getTimestamp());
    dto.setSource(event.getSource());
    dto.setWorkflowModuleId(event.getWorkflowModuleId());
    dto.setComment(event.getComment());
    dto.setBpmnProcessId(event.getBpmnProcessId());
    dto.setBpmnProcessVersion(event.getBpmnProcessVersion());
    dto.setWorkflowTitle(copy(event.getWorkflowTitle()));
    dto.setWorkflowId(event.getWorkflowId());
    dto.setSubWorkflowId(event.getSubWorkflowId());
    dto.setBusinessId(event.getBusinessId());
    dto.setTitle(copy(event.getTitle()));
    dto.setBpmnTaskId(event.getBpmnTaskId());
    dto.setTaskDefinition(event.getTaskDefinition());
    dto.setTaskDefinitionTitle(copy(event.getTaskDefinitionTitle()));
    dto.setUiUriPath(event.getUiUriPath());
    dto.setUiUriType(uiUriTypeOf(event.getUiUriType()));
    dto.setAssignee(event.getAssignee());
    dto.setCandidateUsers(copy(event.getCandidateUsers()));
    dto.setCandidateGroups(copy(event.getCandidateGroups()));
    dto.setExcludedCandidateUsers(copy(event.getExcludedCandidateUsers()));
    dto.setDueDate(event.getDueDate());
    dto.setFollowUpDate(event.getFollowUpDate());
    dto.setDetails(copy(event.getDetails()));
    dto.setDetailsFulltextSearch(event.getDetailsFulltextSearch());
    dto.setNotificationDelivery(notificationDeliveryOf(event.getNotificationDelivery()));
    dto.setUpdated(false);
    return dto;

  }

  /**
   * @param event The Updated event
   * @return What the cockpit's API is sent
   */
  public static UserTaskUpdatedEvent mapUpdated(
      final UserTaskEvent event) {

    final var dto = new UserTaskUpdatedEvent();
    dto.setId(event.getEventId());
    dto.setUserTaskId(event.getUserTaskId());
    dto.setInitiator(event.getInitiator());
    dto.setTimestamp(event.getTimestamp());
    dto.setSource(event.getSource());
    dto.setWorkflowModuleId(event.getWorkflowModuleId());
    dto.setComment(event.getComment());
    dto.setBpmnProcessId(event.getBpmnProcessId());
    dto.setBpmnProcessVersion(event.getBpmnProcessVersion());
    dto.setWorkflowTitle(copy(event.getWorkflowTitle()));
    dto.setWorkflowId(event.getWorkflowId());
    dto.setSubWorkflowId(event.getSubWorkflowId());
    dto.setBusinessId(event.getBusinessId());
    dto.setTitle(copy(event.getTitle()));
    dto.setBpmnTaskId(event.getBpmnTaskId());
    dto.setTaskDefinition(event.getTaskDefinition());
    dto.setTaskDefinitionTitle(copy(event.getTaskDefinitionTitle()));
    dto.setUiUriPath(event.getUiUriPath());
    dto.setUiUriType(uiUriTypeOf(event.getUiUriType()));
    dto.setAssignee(event.getAssignee());
    dto.setCandidateUsers(copy(event.getCandidateUsers()));
    dto.setCandidateGroups(copy(event.getCandidateGroups()));
    dto.setExcludedCandidateUsers(copy(event.getExcludedCandidateUsers()));
    dto.setDueDate(event.getDueDate());
    dto.setFollowUpDate(event.getFollowUpDate());
    dto.setDetails(copy(event.getDetails()));
    dto.setDetailsFulltextSearch(event.getDetailsFulltextSearch());
    dto.setNotificationDelivery(notificationDeliveryOf(event.getNotificationDelivery()));
    dto.setUpdated(true);
    return dto;

  }

  /**
   * @param event The Completed event
   * @return What the cockpit's API is sent
   */
  public static UserTaskCompletedEvent mapCompleted(
      final UserTaskEvent event) {

    final var dto = new UserTaskCompletedEvent();
    dto.setId(event.getEventId());
    dto.setUserTaskId(event.getUserTaskId());
    dto.setInitiator(event.getInitiator());
    dto.setTimestamp(event.getTimestamp());
    dto.setSource(event.getSource());
    dto.setWorkflowModuleId(event.getWorkflowModuleId());
    dto.setComment(event.getComment());
    dto.setBpmnProcessId(event.getBpmnProcessId());
    dto.setBpmnProcessVersion(event.getBpmnProcessVersion());
    dto.setWorkflowTitle(copy(event.getWorkflowTitle()));
    dto.setWorkflowId(event.getWorkflowId());
    dto.setSubWorkflowId(event.getSubWorkflowId());
    dto.setBusinessId(event.getBusinessId());
    dto.setTitle(copy(event.getTitle()));
    dto.setBpmnTaskId(event.getBpmnTaskId());
    dto.setTaskDefinition(event.getTaskDefinition());
    dto.setTaskDefinitionTitle(copy(event.getTaskDefinitionTitle()));
    dto.setUiUriPath(event.getUiUriPath());
    dto.setUiUriType(uiUriTypeOf(event.getUiUriType()));
    dto.setAssignee(event.getAssignee());
    dto.setCandidateUsers(copy(event.getCandidateUsers()));
    dto.setCandidateGroups(copy(event.getCandidateGroups()));
    dto.setExcludedCandidateUsers(copy(event.getExcludedCandidateUsers()));
    dto.setDueDate(event.getDueDate());
    dto.setFollowUpDate(event.getFollowUpDate());
    dto.setDetails(copy(event.getDetails()));
    dto.setDetailsFulltextSearch(event.getDetailsFulltextSearch());
    dto.setNotificationDelivery(notificationDeliveryOf(event.getNotificationDelivery()));
    dto.setUpdated(true);
    return dto;

  }

  /**
   * @param event The Cancelled event
   * @return What the cockpit's API is sent
   */
  public static UserTaskCancelledEvent mapCancelled(
      final UserTaskEvent event) {

    final var dto = new UserTaskCancelledEvent();
    dto.setId(event.getEventId());
    dto.setUserTaskId(event.getUserTaskId());
    dto.setInitiator(event.getInitiator());
    dto.setTimestamp(event.getTimestamp());
    dto.setSource(event.getSource());
    dto.setWorkflowModuleId(event.getWorkflowModuleId());
    dto.setComment(event.getComment());
    dto.setBpmnProcessId(event.getBpmnProcessId());
    dto.setBpmnProcessVersion(event.getBpmnProcessVersion());
    dto.setWorkflowTitle(copy(event.getWorkflowTitle()));
    dto.setWorkflowId(event.getWorkflowId());
    dto.setSubWorkflowId(event.getSubWorkflowId());
    dto.setBusinessId(event.getBusinessId());
    dto.setTitle(copy(event.getTitle()));
    dto.setBpmnTaskId(event.getBpmnTaskId());
    dto.setTaskDefinition(event.getTaskDefinition());
    dto.setTaskDefinitionTitle(copy(event.getTaskDefinitionTitle()));
    dto.setUiUriPath(event.getUiUriPath());
    dto.setUiUriType(uiUriTypeOf(event.getUiUriType()));
    dto.setAssignee(event.getAssignee());
    dto.setCandidateUsers(copy(event.getCandidateUsers()));
    dto.setCandidateGroups(copy(event.getCandidateGroups()));
    dto.setExcludedCandidateUsers(copy(event.getExcludedCandidateUsers()));
    dto.setDueDate(event.getDueDate());
    dto.setFollowUpDate(event.getFollowUpDate());
    dto.setDetails(copy(event.getDetails()));
    dto.setDetailsFulltextSearch(event.getDetailsFulltextSearch());
    dto.setNotificationDelivery(notificationDeliveryOf(event.getNotificationDelivery()));
    dto.setUpdated(true);
    return dto;

  }

  private static io.vanillabp.cockpit.bpms.api.v1_1.UiUriType uiUriTypeOf(
      final UiUriType uiUriType) {

    return uiUriType == null
        ? null
        : io.vanillabp.cockpit.bpms.api.v1_1.UiUriType.fromValue(uiUriType.name());

  }

  private static NotificationDelivery notificationDeliveryOf(
      final io.vanillabp.spi.cockpit.usertask.NotificationDelivery notificationDelivery) {

    return notificationDelivery == null
        ? null
        : NotificationDelivery.fromValue(notificationDelivery.name());

  }

  static <T> List<T> copy(
      final List<T> values) {

    return values == null ? null : new ArrayList<>(values);

  }

  static <K, V> Map<K, V> copy(
      final Map<K, V> values) {

    return values == null ? null : new LinkedHashMap<>(values);

  }

}
