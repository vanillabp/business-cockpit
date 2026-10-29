package io.vanillabp.cockpit.commons.security.usercontext;

import java.util.Collection;
import java.util.Map;

/**
 * Something which knows the group hierarchy of a workflow module.
 */
public interface WorkflowModuleGroupHierarchy {

  /**
   * The group hierarchy: each group mapped to the groups it includes.
   *
   * @return The hierarchy
   */
  Map<String, Collection<String>> getGroupHierarchy();

}
