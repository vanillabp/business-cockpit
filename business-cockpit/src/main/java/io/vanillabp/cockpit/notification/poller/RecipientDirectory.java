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

    /** The available medium types (from the {@code NotificationService} beans). */
    Collection<String> mediaTypes();

    /** Whether the user has logged in at least once (exists in the {@code users} collection). */
    boolean isLoggedIn(String userId);

    /** Ids of all logged-in users (used to fan out CREATED notifications). */
    Collection<String> loggedInUserIds();

    /**
     * The authorities of a user, which are the roles, the group ids and {@code USER_<id>}. It is
     * {@code null} where they cannot be found out, because no user directory is configured for
     * example. A CREATED notification which rests on what the user may see is then skipped for
     * that user.
     */
    List<String> authoritiesOf(String userId);

    /** The user's notification configuration ({@code null} = the default "none"). */
    NotificationConfiguration configOf(String userId);

}
