package io.vanillabp.cockpit.simulator.usertask;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import io.vanillabp.cockpit.bpms.api.v1_1.UserTaskCreatedEvent;
import io.vanillabp.cockpit.commons.mapstruct.NoMappingMethod;
import io.vanillabp.cockpit.gui.api.v1.UserTask;

/**
 * Turns a generated "user task created" event into a user task of the official task list API.
 */
@Mapper
public abstract class OfficialTasklistApiMapper {

  /**
   * Creates the mapper. MapStruct's generated subclass calls it.
   */
  public OfficialTasklistApiMapper() {
  }

  /**
   * Turns the event into a user task. Both timestamps are set to now, and the URIs point to
   * the workflow module behind the cockpit's proxy.
   *
   * @param event The generated event
   * @return The user task
   */
  @Mapping(target = "createdAt", expression = "java(java.time.OffsetDateTime.now())")
  @Mapping(target = "updatedAt", expression = "java(java.time.OffsetDateTime.now())")
  @Mapping(target = "endedAt", ignore = true)
  @Mapping(target = "version", constant = "1")
  @Mapping(target = "uiUri", expression = "java(proxiedUiUri(event))")
  @Mapping(target = "workflowModuleUri", expression = "java(proxiedWorkflowModuleUri(event))")
  @Mapping(target = "read", ignore = true)
  @Mapping(target = "assignee", ignore = true)
  @Mapping(target = "candidateUsers", ignore = true)
  @Mapping(target = "candidateGroups", ignore = true)
  public abstract UserTask toApi(
      UserTaskCreatedEvent event);

  /**
   * The path of the task's UI, always with a leading slash.
   *
   * @param userTask The generated event
   * @return The path, or {@code null} if the event names no workflow module or no UI path
   */
  @NoMappingMethod
  protected String proxiedUiUri(
      final UserTaskCreatedEvent userTask) {

    if (userTask.getWorkflowModuleId() == null) {
      return null;
    }
    if (userTask.getUiUriPath() == null) {
      return null;
    }

    return userTask.getUiUriPath().startsWith("/")
        ? userTask.getUiUriPath()
        : "/"
            + userTask.getUiUriPath();

  }

  /**
   * The path of the workflow module behind the cockpit's proxy: {@code /<id>/}.
   *
   * @param userTask The generated event
   * @return The path, or {@code null} if the event names no workflow module
   */
  @NoMappingMethod
  protected String proxiedWorkflowModuleUri(
      final UserTaskCreatedEvent userTask) {

    if (userTask.getWorkflowModuleId() == null) {
      return null;
    }

    return "/"
        + userTask.getWorkflowModuleId()
        + "/";

  }

}
