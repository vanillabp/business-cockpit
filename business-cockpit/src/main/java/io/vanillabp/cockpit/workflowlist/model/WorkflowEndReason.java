package io.vanillabp.cockpit.workflowlist.model;

/**
 * Why a case ended. {@code null} while the case is still running. The workflow module reports a
 * completion and a cancellation as two different reports, and this keeps the difference, so a
 * list can show a cancelled case in its own way.
 * <p>
 * Released database changesets write the names {@code COMPLETED} and {@code CANCELLED}, so
 * these two are never renamed. {@code NamesTheReleasedChangesetsReadTest} fails when one is.
 */
public enum WorkflowEndReason {

  /** The case reached its end. */
  COMPLETED,
  /** The case was cancelled before it reached its end. */
  CANCELLED

}
