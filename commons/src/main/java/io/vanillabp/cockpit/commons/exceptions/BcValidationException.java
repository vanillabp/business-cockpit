package io.vanillabp.cockpit.commons.exceptions;

import java.util.Map;

/**
 * The values a request sent are not valid. {@link RestfulExceptionHandler} answers it with
 * {@code 400 Bad Request} and sends {@link #getViolations()} as a JSON body.
 */
public class BcValidationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** The violations by field name. A {@code Map.copyOf}, which is serializable. */
  private final Map<String, String> violations;

  /**
   * Creates the exception and says how its stack trace and suppressed exceptions are handled.
   *
   * @param message            What went wrong
   * @param cause              The exception which led to this one
   * @param enableSuppression  Whether suppressed exceptions are recorded
   * @param writableStackTrace Whether the stack trace is recorded
   */
  public BcValidationException(
      String message,
      Throwable cause,
      boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
    this.violations = Map.of();
  }

  /**
   * Creates the exception with the exception which led to it.
   *
   * @param message What went wrong
   * @param cause   The exception which led to this one
   */
  public BcValidationException(
      String message,
      Throwable cause) {
    super(message, cause);
    this.violations = Map.of();
  }

  /**
   * Creates the exception.
   *
   * @param message What went wrong
   */
  public BcValidationException(
      String message) {
    super(message);
    this.violations = Map.of();
  }

  /**
   * Creates the exception with what is wrong with each field. The answer to the request names
   * them.
   *
   * @param message    What went wrong, for the log
   * @param violations The field name as key and the problem as value
   */
  public BcValidationException(
      String message,
      Map<String, String> violations) {
    super(message);
    this.violations = violations == null ? Map.of() : Map.copyOf(violations);
  }

  /**
   * What is wrong with the request, one entry per field. An exception created without them has
   * none, and the body of the answer is an empty JSON object then. The message only goes to the
   * log.
   *
   * @return The violations, the field name as key and the problem as value
   */
  public Map<String, String> getViolations() {
    return violations;
  }

}
