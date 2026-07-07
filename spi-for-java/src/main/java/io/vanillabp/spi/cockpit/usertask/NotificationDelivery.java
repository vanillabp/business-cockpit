package io.vanillabp.spi.cockpit.usertask;

/**
 * How a workflow module wants notifications for a particular user task to be delivered.
 * <p>
 * A {@code null} value is interpreted as {@link #USER_CONFIG}. Using a single enum instead of
 * separate boolean flags avoids contradictory combinations (e.g. "suppress" and "force" at once).
 */
public enum NotificationDelivery {

    /** Deliver according to each user's notification configuration (the default). */
    USER_CONFIG,

    /**
     * Always notify, overriding the user's configuration; the notification is marked as forced.
     * Takes precedence over {@link #SUPPRESS} and over the user configuration.
     */
    FORCE,

    /** Never notify from the cockpit - the workflow module notifies itself. */
    SUPPRESS

}
