package io.vanillabp.cockpit.users.model;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;

/**
 * Turns the ids a workflow module reports into the persons and groups stored with a user task or a
 * workflow. The application provides it, because only the application knows its users.
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
