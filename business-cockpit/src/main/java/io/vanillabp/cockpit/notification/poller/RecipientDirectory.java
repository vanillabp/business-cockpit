package io.vanillabp.cockpit.notification.poller;

import io.vanillabp.cockpit.notification.model.NotificationConfiguration;
import java.util.Collection;
import java.util.List;

/**
 * Read-only view of the data the {@link NotificationScanner} needs, so the classification logic can
 * be unit-tested without MongoDB or a real user directory. Built once per poll cycle by the poller.
 */
public interface RecipientDirectory {

    /** The available medium types (from the {@code NotificationService} beans). */
    Collection<String> mediaTypes();

    /** Whether the user has logged in at least once (exists in the {@code users} collection). */
    boolean isLoggedIn(String userId);

    /** Ids of all logged-in users (used to fan out CREATED notifications). */
    Collection<String> loggedInUserIds();

    /**
     * The authorities (roles + group ids + {@code USER_<id>}) of a user, or {@code null} if they
     * cannot be determined (e.g. no user directory is configured) - in which case visibility-based
     * CREATED notifications are skipped for that user.
     */
    List<String> authoritiesOf(String userId);

    /** The user's notification configuration ({@code null} = the default "none"). */
    NotificationConfiguration configOf(String userId);

}
