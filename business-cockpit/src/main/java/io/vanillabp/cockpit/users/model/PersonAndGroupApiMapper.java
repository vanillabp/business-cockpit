package io.vanillabp.cockpit.users.model;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.gui.api.v1.Group;
import io.vanillabp.cockpit.gui.api.v1.Person;

/**
 * Turns persons and groups into what the GUI API answers with. The application provides it,
 * because only the application knows how its users are shown.
 */
public interface PersonAndGroupApiMapper {

  /**
   * Turns a user of the directory into a person of the GUI API.
   *
   * @param user The user
   * @return The person
   */
  Person userToApiPerson(
      UserDetails user);

  /**
   * Turns a stored person into a person of the GUI API.
   *
   * @param person The stored person
   * @return The person
   */
  Person personToApiPerson(
      io.vanillabp.cockpit.users.model.Person person);

  /**
   * Turns the id of a person into a person of the GUI API.
   *
   * @param personId The id of the person
   * @return The person
   */
  Person toApiPerson(
      String personId);

  /**
   * Turns a stored group into a group of the GUI API.
   *
   * @param group The stored group
   * @return The group
   */
  Group groupToApiGroup(
      io.vanillabp.cockpit.users.model.Group group);

  /**
   * Turns an authority of a user into a group of the GUI API.
   *
   * @param authority The authority
   * @return The group
   */
  Group authorityToApiGroup(
      String authority);
}
