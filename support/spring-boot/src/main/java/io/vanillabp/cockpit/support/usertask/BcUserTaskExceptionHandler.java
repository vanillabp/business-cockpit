package io.vanillabp.cockpit.support.usertask;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Answers the two exceptions of this package for every controller of the application. The body is
 * the message of the exception as plain text, so a form can show it to the user.
 * <p>
 * Nothing registers this class on its own. A workflow module imports it into one of its
 * configurations with {@code @Import(BcUserTaskExceptionHandler.class)}, or a component scan
 * reaches it. An annotation on the exception, {@code @ResponseStatus}, would set the status as
 * well, but Spring Boot then sends its error page as the body and drops the message by default.
 */
@RestControllerAdvice
public class BcUserTaskExceptionHandler {

  /**
   * Creates the handler. Spring calls this constructor.
   */
  public BcUserTaskExceptionHandler() {
  }

  /**
   * Answers a task id which does not belong to the workflow, or belongs to a task of another kind,
   * with {@code 404 Not Found}.
   *
   * @param exception What the workflow module found
   * @return The answer, with the message as the body
   */
  @ExceptionHandler(BcUnknownTaskException.class)
  public ResponseEntity<String> unknownTask(
      final BcUnknownTaskException exception) {

    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .contentType(MediaType.TEXT_PLAIN)
        .body(exception.getMessage());

  }

  /**
   * Answers a task which was completed or canceled before, for example by a form sent twice, with
   * {@code 409 Conflict}.
   *
   * @param exception What the workflow module found
   * @return The answer, with the message as the body
   */
  @ExceptionHandler(BcTaskNotOpenException.class)
  public ResponseEntity<String> taskNotOpen(
      final BcTaskNotOpenException exception) {

    return ResponseEntity
        .status(HttpStatus.CONFLICT)
        .contentType(MediaType.TEXT_PLAIN)
        .body(exception.getMessage());

  }

}
