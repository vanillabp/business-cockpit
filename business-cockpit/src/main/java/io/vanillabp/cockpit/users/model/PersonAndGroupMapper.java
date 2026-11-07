package io.vanillabp.cockpit.users.model;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;

/**
 * Turns the ids a workflow module reports into the persons and groups stored with a user task or a
 * workflow. The application provides it, because only the application knows its users.
 * <p>
 * Released database changesets call {@link #toModelPerson(String)} and
 * {@link #toModelGroup(String)} to turn the plain ids of old records into persons and groups, and
 * they read the id, the full text and the sort text of the answer. These methods keep what they
 * mean, or those changesets store something else on a database from an old release.
 */
public interface PersonAndGroupMapper {

  /**
   * Turns the id of a person into the person to store.
   *
   * @param personId The id of the person
   * @return The person to store
   */
  Person toModelPerson(
      String personId);

  /**
   * Turns the id of a group into the group to store.
   *
   * @param groupId The id of the group
   * @return The group to store
   */
  Group toModelGroup(
      String groupId);

  /**
   * Turns a user of the directory into the person to store.
   *
   * @param user The user
   * @return The person to store
   */
  Person toModelPerson(
      UserDetails user);
}
