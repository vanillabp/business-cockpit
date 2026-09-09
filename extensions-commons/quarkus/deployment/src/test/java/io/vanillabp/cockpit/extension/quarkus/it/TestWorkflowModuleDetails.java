package io.vanillabp.cockpit.extension.quarkus.it;

import java.util.List;

import io.vanillabp.spi.cockpit.workflowmodules.WorkflowModuleDetailsProvider;
import jakarta.enterprise.context.ApplicationScoped;

/** What the test application says about the groups allowed to see its cases. */
@ApplicationScoped
public class TestWorkflowModuleDetails implements WorkflowModuleDetailsProvider {

  /** The groups the registration reports. */
  public static final List<String> ACCESSIBLE_TO_GROUPS = List.of("clerks", "approvers");

  @Override
  public List<String> getAccessibleToGroups() {

    return ACCESSIBLE_TO_GROUPS;

  }

  @Override
  public String getWorkflowModuleId() {

    return "test-module";

  }

}
