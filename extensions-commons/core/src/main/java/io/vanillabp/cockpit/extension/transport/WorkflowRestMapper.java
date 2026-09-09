package io.vanillabp.cockpit.extension.transport;

import static io.vanillabp.cockpit.extension.transport.UserTaskRestMapper.copy;

import io.vanillabp.cockpit.bpms.api.v1_1.WorkflowCancelledEvent;
import io.vanillabp.cockpit.bpms.api.v1_1.WorkflowCompletedEvent;
import io.vanillabp.cockpit.bpms.api.v1_1.WorkflowCreatedEvent;
import io.vanillabp.cockpit.bpms.api.v1_1.WorkflowUpdatedEvent;
import io.vanillabp.cockpit.extension.config.UiUriType;
import io.vanillabp.cockpit.extension.event.WorkflowEvent;

/**
 * Turns a workflow event into what the cockpit's REST API of version 1.1 accepts. It works the
 * way {@link UserTaskRestMapper} works, and the four mappings look alike for the same reason.
 */
public final class WorkflowRestMapper {

  private WorkflowRestMapper() {
  }

  /**
   * @param event The Created event
   * @return What the cockpit's API is sent
   */
  public static WorkflowCreatedEvent mapCreated(
      final WorkflowEvent event) {

    final var dto = new WorkflowCreatedEvent();
    dto.setId(event.getEventId());
    dto.setWorkflowId(event.getWorkflowId());
    dto.setBusinessId(event.getBusinessId());
    dto.setInitiator(event.getInitiator());
    dto.setTimestamp(event.getTimestamp());
    dto.setSource(event.getSource());
    dto.setWorkflowModuleId(event.getWorkflowModuleId());
    dto.setTitle(copy(event.getTitle()));
    dto.setComment(event.getComment());
    dto.setBpmnProcessId(event.getBpmnProcessId());
    dto.setBpmnProcessVersion(event.getBpmnProcessVersion());
    dto.setUiUriPath(event.getUiUriPath());
    dto.setUiUriType(uiUriTypeOf(event.getUiUriType()));
    dto.setAccessibleToUsers(copy(event.getAccessibleToUsers()));
    dto.setAccessibleToGroups(copy(event.getAccessibleToGroups()));
    dto.setDetails(copy(event.getDetails()));
    dto.setDetailsFulltextSearch(event.getDetailsFulltextSearch());
    dto.setUpdated(false);
    return dto;

  }

  /**
   * @param event The Updated event
   * @return What the cockpit's API is sent
   */
  public static WorkflowUpdatedEvent mapUpdated(
      final WorkflowEvent event) {

    final var dto = new WorkflowUpdatedEvent();
    dto.setId(event.getEventId());
    dto.setWorkflowId(event.getWorkflowId());
    dto.setBusinessId(event.getBusinessId());
    dto.setInitiator(event.getInitiator());
    dto.setTimestamp(event.getTimestamp());
    dto.setSource(event.getSource());
    dto.setWorkflowModuleId(event.getWorkflowModuleId());
    dto.setTitle(copy(event.getTitle()));
    dto.setComment(event.getComment());
    dto.setBpmnProcessId(event.getBpmnProcessId());
    dto.setBpmnProcessVersion(event.getBpmnProcessVersion());
    dto.setUiUriPath(event.getUiUriPath());
    dto.setUiUriType(uiUriTypeOf(event.getUiUriType()));
    dto.setAccessibleToUsers(copy(event.getAccessibleToUsers()));
    dto.setAccessibleToGroups(copy(event.getAccessibleToGroups()));
    dto.setDetails(copy(event.getDetails()));
    dto.setDetailsFulltextSearch(event.getDetailsFulltextSearch());
    dto.setUpdated(true);
    return dto;

  }

  /**
   * @param event The Completed event
   * @return What the cockpit's API is sent
   */
  public static WorkflowCompletedEvent mapCompleted(
      final WorkflowEvent event) {

    final var dto = new WorkflowCompletedEvent();
    dto.setId(event.getEventId());
    dto.setWorkflowId(event.getWorkflowId());
    dto.setBusinessId(event.getBusinessId());
    dto.setInitiator(event.getInitiator());
    dto.setTimestamp(event.getTimestamp());
    dto.setSource(event.getSource());
    dto.setWorkflowModuleId(event.getWorkflowModuleId());
    dto.setTitle(copy(event.getTitle()));
    dto.setComment(event.getComment());
    dto.setBpmnProcessId(event.getBpmnProcessId());
    dto.setBpmnProcessVersion(event.getBpmnProcessVersion());
    dto.setUiUriPath(event.getUiUriPath());
    dto.setUiUriType(uiUriTypeOf(event.getUiUriType()));
    dto.setAccessibleToUsers(copy(event.getAccessibleToUsers()));
    dto.setAccessibleToGroups(copy(event.getAccessibleToGroups()));
    dto.setDetails(copy(event.getDetails()));
    dto.setDetailsFulltextSearch(event.getDetailsFulltextSearch());
    dto.setUpdated(true);
    return dto;

  }

  /**
   * @param event The Cancelled event
   * @return What the cockpit's API is sent
   */
  public static WorkflowCancelledEvent mapCancelled(
      final WorkflowEvent event) {

    final var dto = new WorkflowCancelledEvent();
    dto.setId(event.getEventId());
    dto.setWorkflowId(event.getWorkflowId());
    dto.setBusinessId(event.getBusinessId());
    dto.setInitiator(event.getInitiator());
    dto.setTimestamp(event.getTimestamp());
    dto.setSource(event.getSource());
    dto.setWorkflowModuleId(event.getWorkflowModuleId());
    dto.setTitle(copy(event.getTitle()));
    dto.setComment(event.getComment());
    dto.setBpmnProcessId(event.getBpmnProcessId());
    dto.setBpmnProcessVersion(event.getBpmnProcessVersion());
    dto.setUiUriPath(event.getUiUriPath());
    dto.setUiUriType(uiUriTypeOf(event.getUiUriType()));
    dto.setAccessibleToUsers(copy(event.getAccessibleToUsers()));
    dto.setAccessibleToGroups(copy(event.getAccessibleToGroups()));
    dto.setDetails(copy(event.getDetails()));
    dto.setDetailsFulltextSearch(event.getDetailsFulltextSearch());
    dto.setUpdated(true);
    return dto;

  }

  private static io.vanillabp.cockpit.bpms.api.v1_1.UiUriType uiUriTypeOf(
      final UiUriType uiUriType) {

    return uiUriType == null
        ? null
        : io.vanillabp.cockpit.bpms.api.v1_1.UiUriType.fromValue(uiUriType.name());

  }

}
