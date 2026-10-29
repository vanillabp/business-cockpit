package io.vanillabp.cockpit.commons.exceptions;

/**
 * The request is valid, but it cannot be done in the current state, for example because
 * somebody else completed the task already. {@link RestfulExceptionHandler} answers it with
 * {@code 422 Unprocessable Content} and sends the message as the body, so write the message for
 * the user.
 */
public class BcUserMessageException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates the exception and says how its stack trace and suppressed exceptions are handled.
   *
   * @param message            What went wrong
   * @param cause              The exception which led to this one
   * @param enableSuppression  Whether suppressed exceptions are recorded
   * @param writableStackTrace Whether the stack trace is recorded
   */
  public BcUserMessageException(
      String message,
      Throwable cause,
      boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  /**
   * Creates the exception with the exception which led to it.
   *
   * @param message What went wrong
   * @param cause   The exception which led to this one
   */
  public BcUserMessageException(
      String message,
      Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates the exception.
   *
   * @param message What went wrong
   */
  public BcUserMessageException(
      String message) {
    super(message);
  }

}
