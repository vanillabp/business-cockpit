package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.WorkflowModule;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.WorkflowModuleRepository;

/**
 * Keeps the workflow modules which registered at the simulator. They are stored in the
 * simulator's in-memory database.
 */
@Service
public class WorkflowModuleService {

  @Autowired
  private WorkflowModuleRepository workflowModules;

  /**
   * Creates the service. Spring creates it, nobody else does.
   */
  public WorkflowModuleService() {
  }

  /**
   * Stores the registration of a workflow module. A second registration of the same module
   * replaces the first one.
   *
   * @param workflowModuleId The id of the workflow module
   * @param workflowModule The workflow module to store
   */
  public void registerWorkflowModule(
      final String workflowModuleId,
      final WorkflowModule workflowModule) {

    if ((workflowModuleId == null) || (workflowModule == null)) {
      throw new IllegalArgumentException("Workflow Module ID or event must not be null!");
    }

    workflowModules.save(workflowModule);

  }

  /**
   * Reads one workflow module. An unknown id throws an {@link IllegalStateException}.
   *
   * @param workflowModuleId The id of the workflow module
   * @return The workflow module
   */
  public WorkflowModule getWorkflowModule(
      final String workflowModuleId) {

    if (workflowModuleId == null) {
      throw new IllegalArgumentException("Workflow Module ID must not be null");
    }

    return workflowModules
        .findById(workflowModuleId)
        .orElseThrow(() -> new IllegalStateException("Workflow module with ID "
            + workflowModuleId
            + " not found"));

  }

  /**
   * The group hierarchy of every registered workflow module which sent one. The cockpit merges
   * them to find all groups of a user, and so does the simulator.
   *
   * @return One hierarchy per workflow module, each saying for a group which groups it stands for
   */
  @Transactional(readOnly = true)
  public List<Map<String, Collection<String>>> getGroupHierarchies() {

    return workflowModules
        .findAll()
        .stream()
        .map(WorkflowModule::getGroupHierarchy)
        .filter(Objects::nonNull)
        .map(hierarchy -> Map.<String, Collection<String>>copyOf(hierarchy))
        .toList();

  }

}
