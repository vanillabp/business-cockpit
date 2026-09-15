package io.vanillabp.cockpit.tasklist.model;

/**
 * Why a user task ended. {@code null} while the task is still open.
 * <p>
 * It was added so the notification poller can tell a completion from a cancellation by the
 * process. The entity otherwise records nothing but {@code endedAt}.
 */
public enum UserTaskEndReason {

    COMPLETED,
    CANCELLED

}
