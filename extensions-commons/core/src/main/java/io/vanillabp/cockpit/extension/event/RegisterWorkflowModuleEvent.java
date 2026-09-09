package io.vanillabp.cockpit.extension.event;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * What the cockpit server is told about a workflow module once per start: where it lives, what
 * it serves, and which groups may see its cases.
 *
 * @param eventId The identifier of this registration
 * @param timestamp When it was built
 * @param source Which instance of the workflow module sent it
 * @param workflowModuleId The module being registered
 * @param uri Where the module answers the cockpit's calls
 * @param taskProviderApiUriPath The path below the URI serving the user-task provider API
 * @param workflowProviderApiUriPath The path below the URI serving the workflow provider API
 * @param accessibleToGroups The groups allowed to see this module's cases at all
 * @param groupHierarchy Which groups a group stands for
 */
public record RegisterWorkflowModuleEvent(
                                          String eventId,
                                          OffsetDateTime timestamp,
                                          String source,
                                          String workflowModuleId,
                                          String uri,
                                          String taskProviderApiUriPath,
                                          String workflowProviderApiUriPath,
                                          List<String> accessibleToGroups,
                                          Map<String, Collection<String>> groupHierarchy) {

  /**
   * The path the Business Cockpit calls a workflow module's user-task provider API under. It
   * is a convention of the cockpit's own workflow-provider API and not configurable.
   */
  public static final String TASK_PROVIDER_API_URI_PATH = "/task-provider";

  /** The same for the workflow provider API. */
  public static final String WORKFLOW_PROVIDER_API_URI_PATH = "/workflow-provider";

  public RegisterWorkflowModuleEvent {
    accessibleToGroups = accessibleToGroups == null ? List.of() : List.copyOf(accessibleToGroups);
    groupHierarchy = groupHierarchy == null ? Map.of() : Map.copyOf(groupHierarchy);
  }

}
