package io.vanillabp.cockpit.simulator.workflow;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import io.vanillabp.cockpit.bpms.api.v1_1.WorkflowCreatedEvent;
import io.vanillabp.cockpit.commons.mapstruct.NoMappingMethod;
import io.vanillabp.cockpit.gui.api.v1.Workflow;

/**
 * Turns a generated "workflow created" event into a workflow of the official workflow list API.
 */
@Mapper
public abstract class OfficialWorkflowlistApiMapper {

  /**
   * Creates the mapper. MapStruct's generated subclass calls it.
   */
  public OfficialWorkflowlistApiMapper() {
  }

  /**
   * Turns the event into a workflow. Both timestamps are set to now, and the URIs point to
   * the workflow module behind the cockpit's proxy. Initiator and access rights stay empty.
   *
   * @param event The generated event
   * @return The workflow
   */
  @Mapping(target = "createdAt", expression = "java(java.time.OffsetDateTime.now())")
  @Mapping(target = "updatedAt", expression = "java(java.time.OffsetDateTime.now())")
  @Mapping(target = "endedAt", ignore = true)
  @Mapping(target = "version", constant = "1")
  @Mapping(target = "uiUri", expression = "java(proxiedUiUri(event))")
  @Mapping(target = "workflowModuleUri", expression = "java(proxiedWorkflowModuleUri(event))")
  @Mapping(target = "accessibleToUsers", ignore = true)  // TODO
  @Mapping(target = "accessibleToGroups", ignore = true) // TODO
  @Mapping(target = "initiator", ignore = true)          // TODO
  public abstract Workflow toApi(
      WorkflowCreatedEvent event);

  /**
   * The path of the workflow's UI, always with a leading slash.
   *
   * @param workflow The generated event
   * @return The path, or {@code null} if the event names no workflow module or no UI path
   */
  @NoMappingMethod
  protected String proxiedUiUri(
      final WorkflowCreatedEvent workflow) {

    if (workflow.getWorkflowModuleId() == null) {
      return null;
    }
    if (workflow.getUiUriPath() == null) {
      return null;
    }

    return workflow.getUiUriPath().startsWith("/")
        ? workflow.getUiUriPath()
        : "/"
            + workflow.getUiUriPath();

  }

  /**
   * The path of the workflow module behind the cockpit's proxy: {@code /<id>/}.
   *
   * @param workflow The generated event
   * @return The path, or {@code null} if the event names no workflow module
   */
  @NoMappingMethod
  protected String proxiedWorkflowModuleUri(
      final WorkflowCreatedEvent workflow) {

    if (workflow.getWorkflowModuleId() == null) {
      return null;
    }

    return "/"
        + workflow.getWorkflowModuleId()
        + "/";

  }

}
