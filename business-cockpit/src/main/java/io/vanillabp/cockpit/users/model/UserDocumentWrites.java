package io.vanillabp.cockpit.users.model;

import java.util.Map;

import io.vanillabp.cockpit.notification.model.NotificationConfiguration;

/**
 * The writes of {@link UserRepository} which change one part of a user's document and leave the
 * rest as it is.
 * <p>
 * Several things write the same document: the login, each setting of the user interface, the
 * notification configuration and the recipient configuration of each medium. Reading the whole
 * document, changing one part and saving it again fails the version check whenever one of the
 * others writes in between, and the user gets an error, although nothing is wrong. So each of
 * them is one atomic update of its own field, which creates the document if the user has none
 * yet. Each update also raises {@code version}, as the settings do, so that a save of the whole
 * document which read it before still fails instead of wiping out the change.
 */
public interface UserDocumentWrites {

  /**
   * Replaces what the user configured about notifications.
   *
   * @param userId The user
   * @param notificationConfiguration The whole notification configuration
   */
  void writeNotificationConfiguration(
      String userId,
      NotificationConfiguration notificationConfiguration);

  /**
   * Replaces the recipient configuration of one medium, and leaves the ones of the other media as
   * they are.
   *
   * @param userId The user
   * @param mediumType The type of the medium, {@code "email"} for example
   * @param values The values of the recipient configuration, {@code {"emailAddress": "a@b.c"}}
   *        for example
   * @throws IllegalArgumentException If the type of the medium holds a dot or starts with
   *         {@code $}, which MongoDB would read as a step into a nested document or as an operator
   */
  void writeRecipientConfiguration(
      String userId,
      String mediumType,
      Map<String, String> values);

}
