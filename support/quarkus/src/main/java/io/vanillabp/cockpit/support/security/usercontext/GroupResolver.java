package io.vanillabp.cockpit.support.security.usercontext;

import java.util.Collection;

/**
 * Turns the groups the {@link UserDirectoryService} gives for a user into all groups of the user,
 * for example by a group hierarchy. The result becomes the roles of the request.
 * <p>
 * The module brings one which takes the groups as they are. An application replaces it with a bean
 * of its own:
 *
 * <pre>{@code
 * @ApplicationScoped
 * public class GroupsByHierarchy implements GroupResolver {
 *
 *   public Collection<String> resolve(Collection<String> groups) {
 *     return GroupHierarchyResolver.resolveGroups(List.of(hierarchy), groups);
 *   }
 *
 * }
 * }</pre>
 */
@FunctionalInterface
public interface GroupResolver {

  /**
   * All groups of a user.
   *
   * @param groups The groups the directory gives
   * @return The groups together with those they imply
   */
  Collection<String> resolve(
      Collection<String> groups);

}
