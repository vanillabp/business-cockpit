package io.vanillabp.cockpit.notification;

/**
 * The kind of user task change a notification is about.
 *
 * <ul>
 *   <li>{@link #CREATED} - a new user task was reported and is visible to the recipient.</li>
 *   <li>{@link #CANDIDATE_USER} - the recipient was added as a personal candidate of a user task.</li>
 *   <li>{@link #COMPLETED} - a user task the recipient had taken over (assignee) was completed by
 *       someone else.</li>
 *   <li>{@link #CANCELED} - a user task the recipient had taken over (assignee) was cancelled by
 *       the process.</li>
 * </ul>
 */
public enum NotificationType {

    CREATED,
    CANDIDATE_USER,
    COMPLETED,
    CANCELED

}
