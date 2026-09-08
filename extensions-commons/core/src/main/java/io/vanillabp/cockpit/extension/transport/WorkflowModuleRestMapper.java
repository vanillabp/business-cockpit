package io.vanillabp.cockpit.extension.transport;

import java.util.LinkedList;

import io.vanillabp.cockpit.bpms.api.v1_1.GroupHierarchy;
import io.vanillabp.cockpit.extension.event.RegisterWorkflowModuleEvent;

/**
 * Turns the registration of a workflow module into what the cockpit's REST API accepts. The
 * module id is not part of the body; it is the path the registration is sent to.
 */
public final class WorkflowModuleRestMapper {

  private WorkflowModuleRestMapper() {
  }

  /**
   * @param event The registration
   * @return What the cockpit's API is sent
   */
  public static io.vanillabp.cockpit.bpms.api.v1_1.RegisterWorkflowModuleEvent map(
      final RegisterWorkflowModuleEvent event) {

    final var dto = new io.vanillabp.cockpit.bpms.api.v1_1.RegisterWorkflowModuleEvent();
    dto.setUri(event.uri());
    dto.setTaskProviderApiUriPath(event.taskProviderApiUriPath());
    dto.setWorkflowProviderApiUriPath(event.workflowProviderApiUriPath());
    dto.setAccessibleToGroups(new LinkedList<>(event.accessibleToGroups()));
    final var hierarchy = new LinkedList<GroupHierarchy>();
    event
        .groupHierarchy()
        .forEach((
            group,
            targets) -> hierarchy
                .add(new GroupHierarchy().group(group).targets(new LinkedList<>(targets))));
    dto.setGroupHierarchy(hierarchy);
    return dto;

  }

}
