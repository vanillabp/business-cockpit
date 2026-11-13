package io.vanillabp.cockpit.support.usertask;

/**
 * There is no user task of that id and kind in that workflow. A form sent a task id which does
 * not belong to the workflow, or which belongs to a task of another kind.
 * {@link BcUserTaskExceptionHandler} answers it with {@code 404 Not Found} and sends the message as
 * the body.
 */
public class BcUnknownTaskException extends RuntimeException {

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
   * Creates the exception with a message which names both ids.
   *
   * @param workflowId The id of the workflow the request named
   * @param taskId     The id of the user task the request named
   */
  public BcUnknownTaskException(
      final String workflowId,
      final String taskId) {

    super("There is no such user task '"
        + taskId
        + "' in '"
        + workflowId
        + "'");
    this.workflowId = workflowId;
    this.taskId = taskId;

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

}
