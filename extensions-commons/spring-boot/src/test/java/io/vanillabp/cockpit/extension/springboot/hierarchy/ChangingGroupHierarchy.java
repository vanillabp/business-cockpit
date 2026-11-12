package io.vanillabp.cockpit.extension.springboot.hierarchy;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import io.vanillabp.spi.cockpit.workflowmodules.WorkflowModuleGroupHierarchy;

/**
 * The group hierarchy of the test application's workflow module, given by a bean which a test
 * changes while the application runs. It lives outside the package of the test application, so
 * only a test which names it gets it.
 */
public class ChangingGroupHierarchy implements WorkflowModuleGroupHierarchy {

  /** The group the hierarchy names when the application starts. */
  public static final String GROUP_AT_START = "LEAD_FROM_A_BEAN";

  private volatile Map<String, Collection<String>> hierarchy = Map
      .of(GROUP_AT_START, List.of("MEMBER_FROM_A_BEAN"));

  /** Creates the bean with the hierarchy of the start. */
  public ChangingGroupHierarchy() {
  }

  @Override
  public String getWorkflowModuleId() {

    return "test-module";

  }

  @Override
  public Map<String, Collection<String>> getGroupHierarchy() {

    return hierarchy;

  }

  /**
   * Changes the hierarchy. The caller still has to report the change.
   *
   * @param hierarchy The new hierarchy
   */
  public void changeTo(
      final Map<String, Collection<String>> hierarchy) {

    this.hierarchy = hierarchy;

  }

}
