package io.vanillabp.cockpit.notification.poller;

import io.vanillabp.cockpit.notification.NotificationType;

/**
 * A notification the scanner decided to send: one recipient, one user task, one medium.
 *
 * @param userTaskId      the user task the notification is about
 * @param notificationType the kind of change
 * @param medium          the medium type ({@code NotificationService#getType()})
 * @param recipientUserId the recipient
 * @param forced          whether the workflow module forced this notification (delivery = FORCE)
 */
public record PlannedNotification(
        String userTaskId,
        NotificationType notificationType,
        String medium,
        String recipientUserId,
        boolean forced) {

}
