package io.vanillabp.cockpit.tasklist.model;

/**
 * Why a user task ended. {@code null} while the task is still open.
 * <p>
 * The notification poller reads it to tell a completion from a cancellation by the process, and
 * the GUI API shows it, so a list can show a cancelled task in its own way.
 * <p>
 * Released database changesets write the names {@code COMPLETED} and {@code CANCELLED}, so
 * these two are never renamed. {@code NamesTheReleasedChangesetsReadTest} fails when one is.
 */
public enum UserTaskEndReason {

  /** A user completed the task. */
  COMPLETED,
  /** The task was cancelled, for example by the process. */
  CANCELLED

}
