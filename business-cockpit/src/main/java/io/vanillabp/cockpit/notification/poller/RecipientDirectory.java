package io.vanillabp.cockpit.notification.poller;

import io.vanillabp.cockpit.notification.model.NotificationConfiguration;
import java.util.Collection;
import java.util.List;

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
     * Whether the user has logged in at least once (exists in the {@code users} collection).
     *
     * @param userId the user
     * @return {@code true} if the user has logged in before
     */
    boolean isLoggedIn(String userId);

    /**
     * Ids of all logged-in users (used to fan out CREATED notifications).
     *
     * @return the ids of every user who has logged in before
     */
    Collection<String> loggedInUserIds();

    /**
     * The authorities of a user, which are the roles, the group ids and {@code USER_<id>}. It is
     * {@code null} where they cannot be found out, because no user directory is configured for
     * example. A CREATED notification which rests on what the user may see is then skipped for
     * that user.
     *
     * @param userId the user
     * @return the authorities of the user, or {@code null} if they are not known
     */
    List<String> authoritiesOf(String userId);

    /**
     * The user's notification configuration.
     *
     * @param userId the user
     * @return the configuration, or {@code null} for the default "none"
     */
    NotificationConfiguration configOf(String userId);

}
