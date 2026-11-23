package io.vanillabp.cockpit.workflowmodules;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import io.vanillabp.cockpit.util.microserviceproxy.MicroserviceProxyRegistry;
import io.vanillabp.cockpit.workflowmodules.model.GroupHierarchy;
import io.vanillabp.cockpit.workflowmodules.model.WorkflowModule;
import io.vanillabp.cockpit.workflowmodules.model.WorkflowModuleRepository;
import jakarta.annotation.PostConstruct;

/**
 * Keeps the registrations of the workflow modules. A module registers itself when it starts, and
 * the cockpit needs the registration to pass GUI requests on to the module and to resolve the
 * groups of a user.
 */
@Service
public class WorkflowModuleService {

  private static final Logger logger = LoggerFactory.getLogger(WorkflowModuleService.class);

  /** Creates the service. Spring fills the fields. */
  public WorkflowModuleService() {
  }

  @Autowired
  private WorkflowModuleRepository workflowModules;

  @Autowired
  private MicroserviceProxyRegistry microserviceProxyRegistry;

  /**
   * Loads every stored workflow module on startup. It fills the cache of group hierarchies and
   * registers a proxy for each module, so requests can be passed on before any module registers
   * again.
   * <p>
   * A stored module which cannot be routed is passed over with an error in the log, and the
   * cockpit starts all the same. Its users lose that one module until it registers again with a
   * valid id and URI, while the other modules keep working.
   */
  @PostConstruct
  public void registerProxiesForWorkflowModules() {

    workflowModules
        .findAll()
        .forEach(this::applyStoredRegistration);

  }

  /**
   * Makes what is stored for a workflow module effective on this node: its group hierarchy and
   * the route to it. A route which cannot be built is logged, and the module has no route.
   */
  private void applyStoredRegistration(
      final WorkflowModule workflowModule) {

    GroupHierarchyService.putGroupHierarchy(
        workflowModule.getId(),
        WhatARegistrationMustSay.groupHierarchyOf(workflowModule.getGroupHierarchy()));

    final var wrongId = WhatARegistrationMustSay.whatIsWrongWithTheId(workflowModule.getId());
    final var wrongUri = WhatARegistrationMustSay.whatIsWrongWithTheUri(workflowModule.getUri());
    if ((wrongId != null) || (wrongUri != null)) {
      logger.error(
          "The stored workflow module '{}' has no route, so the cockpit cannot pass requests on to it: {}. "
              + "It gets one when it registers again with a valid id and URI.",
          workflowModule.getId(),
          wrongId != null ? wrongId : wrongUri);
      return;
    }
    try {
      microserviceProxyRegistry.registerMicroservice(workflowModule.getId(), workflowModule.getUri());
    } catch (RuntimeException e) {
      logger.error(
          "The stored workflow module '{}' has no route, so the cockpit cannot pass requests on to it. "
              + "It gets one when it registers again with a valid id and URI.",
          workflowModule.getId(),
          e);
    }

  }

  /**
   * Stores the registration of a workflow module, or updates the one stored before. If another
   * cockpit node writes the same module at the same time, the registration is applied once more
   * on top of what that node wrote. This holds for the first registration of a module too.
   *
   * @param id The id of the workflow module
   * @param uri Where the module answers the cockpit's calls
   * @param taskProviderApiUriPath The path below the URI serving the user-task provider API
   * @param workflowProviderApiUriPath The path below the URI serving the workflow provider API
   * @param accessibleToGroups The groups which may see the module, none for everybody
   * @param groupHierarchy For each group, the groups it stands for
   * @return Whether anything had to be written. A registration which repeats what is already
   *         stored leaves the document untouched
   * @throws io.vanillabp.cockpit.exceptions.BcInvalidRequestException if the id or the URI cannot
   *         become a route. Nothing is stored then.
   */
  public boolean registerOrUpdateWorkflowModule(
      final String id,
      final String uri,
      final String taskProviderApiUriPath,
      final String workflowProviderApiUriPath,
      final List<String> accessibleToGroups,
      final Map<String, Collection<String>> groupHierarchy) {

    WhatARegistrationMustSay.checkIdAndUri(id, uri);
    final var checkedGroupHierarchy = WhatARegistrationMustSay.withoutNullTargets(groupHierarchy);

    try {
      return updateWorkflowModule(id, uri, taskProviderApiUriPath, workflowProviderApiUriPath,
          accessibleToGroups, checkedGroupHierarchy);
    } catch (OptimisticLockingFailureException | DuplicateKeyException e) {
      // another node registered the same module concurrently: read the now current state and
      // apply the registration once more. If the module was not stored yet, both nodes insert,
      // and the second insert fails with a duplicate key instead of a version conflict.
      return updateWorkflowModule(id, uri, taskProviderApiUriPath, workflowProviderApiUriPath,
          accessibleToGroups, checkedGroupHierarchy);
    }

  }

  /**
   * @return Whether anything had to be written. A registration which repeats what is already
   *         stored leaves the document untouched
   */
  private boolean updateWorkflowModule(
      final String id,
      final String uri,
      final String taskProviderApiUriPath,
      final String workflowProviderApiUriPath,
      final List<String> accessibleToGroups,
      final Map<String, Collection<String>> groupHierarchy) {

    final var workflowModule = workflowModules
        .findById(id)
        .orElseGet(() -> WorkflowModule.withId(id));

    if (!hasChanged(workflowModule, uri, taskProviderApiUriPath, workflowProviderApiUriPath,
        accessibleToGroups, groupHierarchy)) {
      return false;
    }

    workflowModule.setUri(uri);
    workflowModule.setTaskProviderApiUriPath(taskProviderApiUriPath);
    workflowModule.setWorkflowProviderApiUriPath(workflowProviderApiUriPath);
    workflowModule.setAccessibleToGroups(accessibleToGroups);
    final var modelGroupHierarchy = Optional
        .ofNullable(groupHierarchy)
        .map(hierarchy -> hierarchy
            .entrySet()
            .stream()
            .map(entry -> new GroupHierarchy(entry.getKey(), new LinkedList<>(entry.getValue())))
            .toList())
        .orElse(null);
    workflowModule.setGroupHierarchy(modelGroupHierarchy);

    final var saved = workflowModules.save(workflowModule);

    // only what is stored is made effective, so a failed save leaves this node as it was
    GroupHierarchyService.putGroupHierarchy(saved.getId(), groupHierarchy);
    microserviceProxyRegistry.registerMicroservice(
        saved.getId(),
        saved.getUri());

    return true;

  }

  private boolean hasChanged(
      final WorkflowModule workflowModule,
      final String uri,
      final String taskProviderApiUriPath,
      final String workflowProviderApiUriPath,
      final List<String> accessibleToGroups,
      final Map<String, Collection<String>> groupHierarchy) {

    if ((uri == null) && (workflowModule.getUri() != null)) return true;
    if ((uri != null) && (workflowModule.getUri() == null)) return true;
    if ((uri != null) && !uri.equals(workflowModule.getUri())) return true;
    if ((taskProviderApiUriPath == null) && (workflowModule.getTaskProviderApiUriPath() != null)) return true;
    if ((taskProviderApiUriPath != null) && (workflowModule.getTaskProviderApiUriPath() == null)) return true;
    if ((taskProviderApiUriPath != null) && !taskProviderApiUriPath.equals(workflowModule.getTaskProviderApiUriPath()))
      return true;
    if ((workflowProviderApiUriPath == null) && (workflowModule.getWorkflowProviderApiUriPath() != null)) return true;
    if ((workflowProviderApiUriPath != null) && (workflowModule.getWorkflowProviderApiUriPath() == null)) return true;
    if ((workflowProviderApiUriPath != null) && !workflowProviderApiUriPath
        .equals(workflowModule.getWorkflowProviderApiUriPath()))
      return true;
    if ((accessibleToGroups == null) && (workflowModule.getAccessibleToGroups() != null)) return true;
    if ((accessibleToGroups != null) && (workflowModule.getAccessibleToGroups() == null)) return true;
    if ((accessibleToGroups != null) && !accessibleToGroups.equals(workflowModule.getAccessibleToGroups())) return true;
    if (!Objects.equals(
        asComparableHierarchy(groupHierarchy),
        asComparableHierarchy(workflowModule.getGroupHierarchy())))
      return true;
    return false;

  }

  /**
   * A registration brings the hierarchy as a map, and the document keeps it as a list of
   * entries. Both have to be brought into one shape before they can be compared. Otherwise
   * every re-registration of a module with a hierarchy would look like a change and would
   * rewrite the document.
   */
  private static Map<String, List<String>> asComparableHierarchy(
      final Map<String, Collection<String>> groupHierarchy) {

    if (groupHierarchy == null) {
      return null;
    }
    return groupHierarchy
        .entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));

  }

  private static Map<String, List<String>> asComparableHierarchy(
      final List<GroupHierarchy> groupHierarchy) {

    // read the way the startup reads it, so a stored document with a target which is null can
    // still be replaced by a registration
    return asComparableHierarchy(WhatARegistrationMustSay.groupHierarchyOf(groupHierarchy));

  }

  /**
   * One workflow module by its id, with nobody asked whether the caller may see it. Used where
   * the cockpit needs the registration itself, for instance to route a proxied request.
   * Everything answering a person goes through the overload taking a
   * {@link WorkflowModuleVisibility}.
   *
   * @param id The id of the workflow module
   * @return The module, or {@code null} when there is none or the id is {@code null}
   */
  public WorkflowModule getWorkflowModule(
      final String id) {

    if (id == null) {
      return null;
    }
    return workflowModules
        .findById(id)
        .orElse(null);

  }

  /**
   * The module of that id which the given visibility lets through, or {@code null} when there is
   * none. The visibility goes into the query the same way it goes into the query behind the list,
   * so the two cannot drift apart.
   *
   * @param visibility Which modules the caller may reach
   * @param id The id of the workflow module
   * @return The module, or {@code null}
   */
  public WorkflowModule getWorkflowModule(
      final WorkflowModuleVisibility visibility,
      final String id) {

    if (id == null) {
      return null;
    }
    if (visibility.accessibleToGroups() == null) {
      return getWorkflowModule(id);
    }
    return workflowModules
        .findByIdAndAccessibleToGroups(id, List.copyOf(visibility.accessibleToGroups()))
        .orElse(null);

  }

  /**
   * The modules the given visibility lets through, for the module list of the GUI.
   *
   * @param visibility Which modules the caller may reach
   * @return The modules, in no particular order
   */
  public List<WorkflowModule> getWorkflowModules(
      final WorkflowModuleVisibility visibility) {

    if (visibility.accessibleToGroups() == null) {
      return workflowModules.findAll();
    }
    return workflowModules.findByAccessibleToGroups(
        List.copyOf(visibility.accessibleToGroups()));

  }

}
