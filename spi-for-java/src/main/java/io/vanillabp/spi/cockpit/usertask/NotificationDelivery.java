package io.vanillabp.spi.cockpit.usertask;

/**
 * How a workflow module wants notifications for a particular user task to be delivered.
 * <p>
 * A {@code null} value is read as {@link #USER_CONFIG}. One enum instead of two boolean flags,
 * so that "suppress" and "force" cannot be set at the same time.
 */
public enum NotificationDelivery {

    /** Deliver according to each user's notification configuration (the default). */
    USER_CONFIG,

    /**
     * Always notify, whatever the user configured. The notification is marked as forced, and
     * this wins over {@link #SUPPRESS} and over the user configuration.
     */
    FORCE,

    /** Never notify from the cockpit. The workflow module notifies itself. */
    SUPPRESS

}
