package io.vanillabp.cockpit.notification.poller;

import java.util.Collection;
import java.util.List;

import io.vanillabp.cockpit.notification.model.NotificationConfiguration;

/**
 * A read-only view of what the {@link NotificationScanner} needs. It lets the scanner be
 * unit-tested without MongoDB and without a real user directory. The poller builds it once per
 * cycle.
 */
public interface RecipientDirectory {

  /**
   * The available medium types (from the {@code NotificationService} beans).
   *
   * @return the medium types, e.g. {@code "email"}
   */
  Collection<String> mediaTypes();

  /**
   * Whether the user may get a notification at all. That is a user who used the cockpit recently
   * and whom the user directory knows. Whether a notification about a certain task is due is
   * decided by the scanner.
   *
   * @param userId the user
   * @return {@code true} if the user may be notified
   */
  boolean isRecipient(
      String userId);

  /**
   * The ids of every user who may get a notification, see {@link #isRecipient(String)}. A
   * CREATED notification is planned for each of them who can see the task.
   *
   * @return the ids of the users who may be notified
   */
  Collection<String> recipientIds();

  /**
   * The authorities of a user, which are the roles, the group ids and {@code USER_<id>}. It is
   * {@code null} where they cannot be found out, because no user directory is configured for
   * example. A CREATED notification which rests on what the user may see is then skipped for
   * that user.
   *
   * @param userId the user
   * @return the authorities of the user, or {@code null} if they are not known
   */
  List<String> authoritiesOf(
      String userId);

  /**
   * The user's notification configuration.
   *
   * @param userId the user
   * @return the configuration, or {@code null} for the default "none"
   */
  NotificationConfiguration configOf(
      String userId);

}
