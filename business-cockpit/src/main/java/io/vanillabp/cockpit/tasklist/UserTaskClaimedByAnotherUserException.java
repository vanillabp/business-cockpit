package io.vanillabp.cockpit.tasklist;

/**
 * Thrown if a user tries to give back a user task which another user claimed. Only the assignee can
 * give a task back. The message is written for the user and names no other user, because it is the
 * body of the answer.
 */
public class UserTaskClaimedByAnotherUserException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** The id of the user task which another user claimed. */
  private final String userTaskId;

  /**
   * Creates the exception.
   *
   * @param userTaskId The id of the user task which another user claimed
   */
  public UserTaskClaimedByAnotherUserException(
      final String userTaskId) {
    super("User task '"
        + userTaskId
        + "' is claimed by another user. Only that user can give it back.");
    this.userTaskId = userTaskId;
  }

  /**
   * The id of the user task which another user claimed.
   *
   * @return The id
   */
  public String getUserTaskId() {
    return userTaskId;
  }

}
