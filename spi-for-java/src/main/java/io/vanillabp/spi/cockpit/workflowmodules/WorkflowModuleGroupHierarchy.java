package io.vanillabp.spi.cockpit.workflowmodules;

import java.util.Collection;
import java.util.Map;

/**
 * Gives the group hierarchy of one workflow module, for a hierarchy which does not come from the
 * configuration.
 *
 * <p>A group hierarchy says which other groups a group stands for: a member of {@code TEAM_LEAD}
 * counts as a member of {@code TEAM_MEMBER} as well, for instance. Usually the hierarchy is
 * configuration, written below {@code vanillabp.workflow-modules.<id>.cockpit.group-hierarchy}.
 * Put a bean implementing this interface into the application instead when the hierarchy has to
 * change while the application runs. One example is a test environment in which a team works out
 * its roles and their rights, and copies the result into the configuration once it is done.
 *
 * <p>There is one bean for each workflow module which takes its hierarchy from a bean. A bean
 * for a module which also configures {@code group-hierarchy}, a bean for a module which does not
 * report to the Business Cockpit, and two beans for one module each stop the application from
 * starting.
 *
 * <p>The Business Cockpit adapter calls {@link #getGroupHierarchy()} each time it registers the
 * workflow module at the cockpit. That happens once when the application starts, and again for
 * every {@link WorkflowModuleGroupHierarchyChangedEvent}. To report a change, publish that event
 * with the id of the module: on Spring Boot through the {@code ApplicationEventPublisher}, on
 * Quarkus through {@code jakarta.enterprise.event.Event}. The cockpit then uses the new hierarchy.
 *
 * <p>Where the workflow module resolves the groups of a request with this hierarchy itself, for
 * example in the group resolver of the {@code JwtAuthenticationTokenMapper} of the module
 * {@code support}, call {@link #getGroupHierarchy()} on every request rather than keeping a copy.
 * Then a change counts from the next request on, and there is no cache to clear.
 */
public interface WorkflowModuleGroupHierarchy {

  /**
   * The id of the workflow module this bean answers for, the same id the module is configured
   * with.
   *
   * @return The workflow module id
   */
  String getWorkflowModuleId();

  /**
   * The group hierarchy as it is now: each group mapped to the groups it stands for.
   *
   * @return The hierarchy. An empty map means that no group stands for another
   */
  Map<String, Collection<String>> getGroupHierarchy();

}
