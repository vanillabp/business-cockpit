package io.vanillabp.spi.cockpit.workflowmodules;

import java.util.Objects;

/**
 * Says that the group hierarchy of a workflow module changed. Publish it after a
 * {@link WorkflowModuleGroupHierarchy} bean answers with a new hierarchy: on Spring Boot through
 * the {@code ApplicationEventPublisher}, on Quarkus through {@code jakarta.enterprise.event.Event}.
 *
 * <p>The Business Cockpit adapter then registers the workflow module at the cockpit again, and the
 * registration carries the new hierarchy. A module which does not report to the cockpit makes the
 * publisher fail.
 *
 * @param workflowModuleId The id of the workflow module whose hierarchy changed
 */
public record WorkflowModuleGroupHierarchyChangedEvent(
                                                       String workflowModuleId) {

  /**
   * Creates the event.
   *
   * @param workflowModuleId The id of the workflow module whose hierarchy changed
   */
  public WorkflowModuleGroupHierarchyChangedEvent {
    Objects.requireNonNull(workflowModuleId, "workflowModuleId");
  }

}
