package io.vanillabp.cockpit.notification;

/**
 * The kind of user task change a notification is about.
 *
 * <ul>
 *   <li>{@link #CREATED}: a new user task was reported and the recipient can see it.</li>
 *   <li>{@link #CANDIDATE_USER}: the recipient became a personal candidate of a user task.</li>
 *   <li>{@link #COMPLETED}: somebody else completed a user task the recipient had taken over.</li>
 *   <li>{@link #CANCELED}: the process cancelled a user task the recipient had taken over.</li>
 * </ul>
 */
public enum NotificationType {

    CREATED,
    CANDIDATE_USER,
    COMPLETED,
    CANCELED

}
