package io.vanillabp.cockpit.tasklist.model;

/**
 * Why a user task ended. {@code null} while the task is still open.
 * <p>
 * Introduced so the notification poller can tell a completion (AC func 2c) apart from a
 * cancellation by the process (AC func 2d) - the entity otherwise only records {@code endedAt}.
 */
public enum UserTaskEndReason {

    COMPLETED,
    CANCELLED

}
