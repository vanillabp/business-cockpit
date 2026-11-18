package io.vanillabp.cockpit.tasklist;

/**
 * Thrown if a user tries to claim, give back, propose or take a candidate off a user task whose
 * process instance is suspended. Nobody may work on such a task until it is activated again. The
 * message is written for the user and names no user, because it is the body of the answer. See decision 116.
 */
public class UserTaskSuspendedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** The id of the user task which is suspended. */
  private final String userTaskId;

  /**
   * Creates the exception.
   *
   * @param userTaskId The id of the user task which is suspended
   */
  public UserTaskSuspendedException(
      final String userTaskId) {
    super("User task '"
        + userTaskId
        + "' is suspended. Nobody can work on it until it is activated again.");
    this.userTaskId = userTaskId;
  }

  /**
   * The id of the user task which is suspended.
   *
   * @return The id
   */
  public String getUserTaskId() {
    return userTaskId;
  }

}
