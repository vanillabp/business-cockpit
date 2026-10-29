package io.vanillabp.cockpit.workflowmodules;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.springframework.stereotype.Service;

import io.vanillabp.cockpit.commons.security.usercontext.GroupHierarchyResolver;

/**
 * Keeps the group hierarchy of every registered workflow module in memory and resolves a user's
 * groups with it. A hierarchy says which other groups a group stands for: a member of
 * {@code TEAM_LEAD} counts as a member of {@code TEAM_MEMBER} as well, for instance.
 *
 * <p>The hierarchies of all modules are merged, so a group a user gets through one module's
 * hierarchy counts in every module. The cache is static because it is filled by
 * {@link WorkflowModuleService} on startup and on every registration.
 */
@Service
public class GroupHierarchyService {

  /** Creates the service. Spring calls it, the cache itself is shared by all instances. */
  public GroupHierarchyService() {
  }

  private static final Map<String, Map<String, Collection<String>>> cachedGroupHierarchies = new HashMap<>();

  private static final Lock readLock;

  private static final Lock writeLock;

  static {

    final var readWriteLock = new ReentrantReadWriteLock();
    readLock = readWriteLock.readLock();
    writeLock = readWriteLock.writeLock();

  }

  /**
   * Stores the hierarchy a workflow module registered and replaces the one it registered before.
   *
   * @param workflowModuleId The id of the workflow module
   * @param groupHierarchy For each group, the groups it stands for
   */
  public static void putGroupHierarchy(
      final String workflowModuleId,
      final Map<String, Collection<String>> groupHierarchy) {

    try {
      writeLock.lock();

      cachedGroupHierarchies.put(workflowModuleId, groupHierarchy);

    } finally {
      writeLock.unlock();
    }

  }

  /**
   * The groups a user really has: the assigned ones plus every group they stand for in the
   * hierarchies, followed down to the end.
   *
   * @param assignedGroups The groups the user is assigned to
   * @return The assigned groups plus the groups reached through the hierarchies
   */
  public Collection<String> resolveGroups(
      final Collection<String> assignedGroups) {

    try {
      readLock.lock();

      return GroupHierarchyResolver.resolveGroups(
          cachedGroupHierarchies.values(),
          assignedGroups);

    } finally {
      readLock.unlock();
    }

  }

}
