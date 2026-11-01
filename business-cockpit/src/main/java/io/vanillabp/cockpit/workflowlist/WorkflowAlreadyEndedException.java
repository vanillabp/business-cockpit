package io.vanillabp.cockpit.workflowlist;

/**
 * Thrown when a user changes a case which has ended already, for example by writing its note.
 * The case may have been completed or cancelled.
 */
public class WorkflowAlreadyEndedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** The id of the workflow which has ended. */
  private final String workflowId;

  /**
   * Creates the exception.
   *
   * @param workflowId The id of the workflow which has ended
   */
  public WorkflowAlreadyEndedException(
      final String workflowId) {
    super("Workflow '"
        + workflowId
        + "' has ended already");
    this.workflowId = workflowId;
  }

  /**
   * The id of the workflow which has ended.
   *
   * @return The id
   */
  public String getWorkflowId() {
    return workflowId;
  }

}
