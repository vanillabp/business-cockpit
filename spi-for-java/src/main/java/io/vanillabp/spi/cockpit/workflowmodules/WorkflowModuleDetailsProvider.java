package io.vanillabp.spi.cockpit.workflowmodules;

import java.util.List;

/**
 * Says which groups may see a workflow module in the Business Cockpit.
 *
 * <p>Put one bean implementing this interface into the application for each workflow module which
 * should not be visible to everybody. The cockpit asks it when the module registers with the
 * cockpit. A module without such a bean is visible to everybody who may use the cockpit. Two beans
 * for the same module stop the application from starting.
 *
 * <p>This decides only which modules the list of modules shows and which module can be opened.
 * The tasks and workflows of a module carry their own visibility, so a user who cannot see a
 * module may still see what it reported.
 */
public interface WorkflowModuleDetailsProvider {

  /**
   * The groups whose members may see the workflow module. An empty list makes the module visible
   * to everybody.
   *
   * @return The group ids
   */
  List<String> getAccessibleToGroups();

  /**
   * The id of the workflow module this bean answers for, the same id the module is configured
   * with.
   *
   * @return The workflow module id
   */
  String getWorkflowModuleId();

}
