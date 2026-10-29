package io.vanillabp.cockpit.commons.exceptions;

/**
 * There is no user logged in, or the login is not valid.
 * {@link RestfulExceptionHandler} answers it with {@code 401 Unauthorized} and sends the message as
 * the body.
 */
public class BcUnauthorizedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates the exception and says how its stack trace and suppressed exceptions are handled.
   *
   * @param message            What went wrong
   * @param cause              The exception which led to this one
   * @param enableSuppression  Whether suppressed exceptions are recorded
   * @param writableStackTrace Whether the stack trace is recorded
   */
  public BcUnauthorizedException(
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
  public BcUnauthorizedException(
      String message,
      Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates the exception.
   *
   * @param message What went wrong
   */
  public BcUnauthorizedException(
      String message) {
    super(message);
  }

}
