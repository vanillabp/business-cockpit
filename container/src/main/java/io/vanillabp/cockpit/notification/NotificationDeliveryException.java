package io.vanillabp.cockpit.notification;

import java.util.List;
import java.util.Set;

/**
 * Thrown by a {@link NotificationService} when it could deliver a notification to some, but not all
 * recipients of a bulk.
 * <p>
 * Reporting the failed recipients lets the notification poller keep exactly those pending and mark
 * the rest as sent. Without that information a single bad address would make the whole bulk be
 * retried, sending the notification again to everybody it already reached.
 * <p>
 * A {@link NotificationService} signalling failure by any other exception is treated as "the whole
 * bulk failed", which is correct but repeats successful deliveries on the next attempt.
 */
public class NotificationDeliveryException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Set<String> failedRecipientUserIds;

    /**
     * @param failedRecipientUserIds the recipients the notification could not be delivered to; an
     *                               empty collection is understood as "all of them"
     * @param cause                  the last failure, may be {@code null}
     */
    public NotificationDeliveryException(
            final List<String> failedRecipientUserIds,
            final Throwable cause) {

        super("Could not deliver notification to users " + failedRecipientUserIds, cause);
        this.failedRecipientUserIds = Set.copyOf(failedRecipientUserIds);

    }

    /**
     * @return the recipients which have to be retried; empty means the whole bulk has to be retried
     */
    public Set<String> getFailedRecipientUserIds() {

        return failedRecipientUserIds;

    }

}
