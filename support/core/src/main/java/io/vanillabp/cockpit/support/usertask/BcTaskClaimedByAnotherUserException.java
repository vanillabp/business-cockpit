package io.vanillabp.cockpit.support.usertask;

import java.util.Optional;

/**
 * The user task is open, but another user claimed it. Two users opened the same form, and the one
 * who did not claim the task sent it. {@code BcUserTaskExceptionHandler} on Spring Boot and
 * {@code BcUserTaskExceptionMapper} on Quarkus answer it with {@code 409 Conflict} and send the
 * message as the body.
 */
public class BcTaskClaimedByAnotherUserException extends RuntimeException {

  /**
   * The HTTP status a form answers with: {@value}. The task is open, but another user claimed it,
   * so only that user may send the form.
   */
  public static final int HTTP_STATUS_CODE = 409;

  private static final long serialVersionUID = 1L;

  /**
   * The id of the workflow the request named.
   */
  private final String workflowId;

  /**
   * The id of the user task the request named.
   */
  private final String taskId;

  /**
   * The id of the user who claimed the task, or {@code null} if the workflow module does not know
   * it.
   */
  private final String claimedBy;

  /**
   * Creates the exception for a workflow module which knows that another user claimed the task,
   * but not who. The message names both ids.
   *
   * @param workflowId The id of the workflow the request named
   * @param taskId     The id of the user task the request named
   */
  public BcTaskClaimedByAnotherUserException(
      final String workflowId,
      final String taskId) {

    this(workflowId, taskId, null);

  }

  /**
   * Creates the exception with a message which names both ids and the user who claimed the task.
   *
   * @param workflowId The id of the workflow the request named
   * @param taskId     The id of the user task the request named
   * @param claimedBy  The id of the user who claimed the task, or {@code null} if it is not known
   */
  public BcTaskClaimedByAnotherUserException(
      final String workflowId,
      final String taskId,
      final String claimedBy) {

    super("The user task '"
        + taskId
        + "' of '"
        + workflowId
        + "' is claimed by "
        + (claimedBy == null
            ? "another user"
            : "'"
                + claimedBy
                + "'"));
    this.workflowId = workflowId;
    this.taskId = taskId;
    this.claimedBy = claimedBy;

  }

  /**
   * The id of the workflow the request named, so a handler can log it or show it.
   *
   * @return The id of the workflow
   */
  public String getWorkflowId() {

    return workflowId;

  }

  /**
   * The id of the user task the request named, so a handler can log it or show it.
   *
   * @return The id of the user task
   */
  public String getTaskId() {

    return taskId;

  }

  /**
   * The id of the user who claimed the task, so a handler can log it or show it.
   *
   * @return The id of the user, or nothing if the workflow module does not know it
   */
  public Optional<String> getClaimedBy() {

    return Optional.ofNullable(claimedBy);

  }

}
