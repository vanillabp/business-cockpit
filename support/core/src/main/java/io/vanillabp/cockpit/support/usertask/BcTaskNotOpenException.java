package io.vanillabp.cockpit.support.usertask;

/**
 * The user task exists, but it was completed or canceled already. A form was sent twice, or it was
 * sent after the workflow took the task away. {@code BcUserTaskExceptionHandler} on Spring Boot and
 * {@code BcUserTaskExceptionMapper} on Quarkus answer it with {@code 404 Not Found}, like an unknown
 * task, and send the message as the body. For the form there is no open task of that id either way.
 */
public class BcTaskNotOpenException extends RuntimeException {

  /**
   * The HTTP status a form answers with: {@value}. The task is not open any more, which for the
   * form is the same as an unknown task: it cannot take an answer for it.
   */
  public static final int HTTP_STATUS_CODE = 404;

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
  public BcTaskNotOpenException(
      final String workflowId,
      final String taskId) {

    super("The user task '"
        + taskId
        + "' of '"
        + workflowId
        + "' is not open any more");
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
