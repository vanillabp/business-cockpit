package io.vanillabp.cockpit.support.security;

/**
 * There is no user logged in, or the login is not valid. {@code RestfulExceptionHandler} of the
 * cockpit and {@code BcUnauthorizedExceptionMapper} on Quarkus answer it with
 * {@code 401 Unauthorized} and send the message as the body.
 */
public class BcUnauthorizedException extends RuntimeException {

  /**
   * The HTTP status a form answers with: {@value}. The request carries no valid login, so the form
   * does not know who sends it.
   */
  public static final int HTTP_STATUS_CODE = 401;

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
