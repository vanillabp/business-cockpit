package io.vanillabp.cockpit.util.candidates;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import io.vanillabp.cockpit.commons.security.jwt.JwtUserDetails;

/**
 * Something which only some users may see, like a user task or a workflow. Two lists of
 * candidates say who may see it: groups and single users.
 * <p>
 * Both lists are merged into one set of target groups. A user id becomes a group with the prefix
 * {@link JwtUserDetails#USER_AUTHORITY_PREFIX}, which is how a user's own authority is named in the
 * token. No candidates at all means that everybody may see it.
 */
public abstract class CandidatesAware {

  /**
   * Used by the subclasses.
   */
  public CandidatesAware() {

  }

  /**
   * The groups whose members may see this.
   *
   * @return The ids of the groups, or {@code null} if there are none
   */
  protected abstract List<String> getGroupIds();

  /**
   * The single users who may see this.
   *
   * @return The ids of the users, or {@code null} if there are none
   */
  protected abstract List<String> getUserIds();

  /**
   * The candidate groups and the candidate users as one set. A user id carries the prefix
   * {@link JwtUserDetails#USER_AUTHORITY_PREFIX}.
   *
   * @return The target groups, or {@code null} if everybody may see this
   */
  public Collection<String> getTargetGroups() {

    final var result = new HashSet<String>();
    if (getGroupIds() != null) {
      result.addAll(getGroupIds());
    }
    final var userIds = getUserIds();
    if (userIds != null) {
      userIds
          .forEach(user -> result.add(JwtUserDetails.USER_AUTHORITY_PREFIX + user));
    }
    if (result.isEmpty()) {
      return null; // means visible to everyone
    }
    return result;

  }

  /**
   * Whether one of the given groups is a target group of this.
   *
   * @param groups The groups to look for
   * @return {@code true} if one of them is a target group, or if no group is given
   */
  public boolean hasOneOfTargetGroups(
      final String... groups) {

    if ((groups == null) || (groups.length == 0)) {
      return true;
    }

    final var targetGroups = getTargetGroups();
    return Arrays
        .stream(groups)
        .anyMatch(targetGroups::contains);

  }

  /**
   * Whether one of the given groups is a target group of this.
   *
   * @param groups The groups to look for
   * @return {@code true} if one of them is a target group, or if no group is given
   */
  public boolean hasOneOfTargetGroups(
      final Collection<String> groups) {

    if ((groups == null) || groups.isEmpty()) {
      return true;
    }

    final var targetGroups = getTargetGroups();
    return groups
        .stream()
        .anyMatch(targetGroups::contains);

  }

}
