package io.vanillabp.cockpit.tasklist;

/**
 * Thrown when a user changes a user task which has ended already, for example by setting its
 * follow-up date. The task may have been completed or cancelled.
 */
public class UserTaskAlreadyCompletedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** The id of the user task which has ended. */
  private final String userTaskId;

  /**
   * Creates the exception.
   *
   * @param userTaskId The id of the user task which has ended
   */
  public UserTaskAlreadyCompletedException(
      final String userTaskId) {
    super("User task '"
        + userTaskId
        + "' is already completed");
    this.userTaskId = userTaskId;
  }

  /**
   * The id of the user task which has ended.
   *
   * @return The id
   */
  public String getUserTaskId() {
    return userTaskId;
  }

}
