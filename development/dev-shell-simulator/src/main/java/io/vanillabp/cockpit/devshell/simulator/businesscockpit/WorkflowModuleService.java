package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.WorkflowModule;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.WorkflowModuleRepository;
import io.vanillabp.cockpit.support.security.usercontext.GroupHierarchyResolver;

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
   * replaces the first one. The version counts the registrations, starting at 0, as the cockpit
   * counts the saves of a module.
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

    workflowModule.setVersion(workflowModules
        .findById(workflowModuleId)
        .map(WorkflowModule::getVersion)
        .map(version -> version + 1)
        .orElse(0L));
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

  /**
   * The workflow modules the cockpit lists for a user with these groups. A module which
   * registered groups is listed for the members of one of them. A module which registered no
   * groups is listed for everybody. The groups are completed by the group hierarchies of all
   * registered modules first, as the cockpit does it.
   *
   * @param assignedGroups The groups the user is assigned to
   * @return The modules the user sees, sorted by their id
   */
  @Transactional(readOnly = true)
  public List<WorkflowModule> getWorkflowModulesVisibleTo(
      final Collection<String> assignedGroups) {

    final var groups = GroupHierarchyResolver.resolveGroups(getGroupHierarchies(), assignedGroups);

    return workflowModules
        .findAll()
        .stream()
        .filter(workflowModule -> isVisibleTo(workflowModule, groups))
        .sorted(Comparator.comparing(WorkflowModule::getId))
        .toList();

  }

  /**
   * One workflow module, if a user with these groups sees it. The rule is the one of
   * {@link #getWorkflowModulesVisibleTo(Collection)}, so a module kept out of the list is unknown
   * by its id as well.
   *
   * @param assignedGroups The groups the user is assigned to
   * @param workflowModuleId The id of the workflow module
   * @return The module, or nothing if there is none or the user does not see it
   */
  @Transactional(readOnly = true)
  public Optional<WorkflowModule> getWorkflowModuleVisibleTo(
      final Collection<String> assignedGroups,
      final String workflowModuleId) {

    return getWorkflowModulesVisibleTo(assignedGroups)
        .stream()
        .filter(workflowModule -> workflowModule.getId().equals(workflowModuleId))
        .findFirst();

  }

  private static boolean isVisibleTo(
      final WorkflowModule workflowModule,
      final Collection<String> groups) {

    final var accessibleToGroups = workflowModule.getAccessibleToGroups();
    if ((accessibleToGroups == null) || accessibleToGroups.isEmpty()) {
      return true;
    }
    return accessibleToGroups
        .stream()
        .anyMatch(groups::contains);

  }

}
