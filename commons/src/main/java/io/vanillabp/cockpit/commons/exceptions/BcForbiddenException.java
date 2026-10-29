package io.vanillabp.cockpit.commons.exceptions;

/**
 * The user is known but may not do what the request asks for.
 * {@link RestfulExceptionHandler} answers it with {@code 403 Forbidden} and sends the message as
 * the body, so the message must not hold anything the user may not see.
 */
public class BcForbiddenException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates the exception and says how its stack trace and suppressed exceptions are handled.
   *
   * @param message            What went wrong
   * @param cause              The exception which led to this one
   * @param enableSuppression  Whether suppressed exceptions are recorded
   * @param writableStackTrace Whether the stack trace is recorded
   */
  public BcForbiddenException(
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
  public BcForbiddenException(
      String message,
      Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates the exception.
   *
   * @param message What went wrong
   */
  public BcForbiddenException(
      String message) {
    super(message);
  }

}
