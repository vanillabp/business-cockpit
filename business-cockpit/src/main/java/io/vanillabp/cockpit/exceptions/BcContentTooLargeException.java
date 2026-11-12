package io.vanillabp.cockpit.exceptions;

/**
 * A request whose content the server cannot store, because it is too large. Sending it again
 * cannot help. {@link RestfulExceptionHandler} answers it with {@code 413 Content Too Large} and
 * sends the message as the body, so the message names the limit.
 */
public class BcContentTooLargeException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates the exception.
   *
   * @param message What is too large, and which limit it breaks
   */
  public BcContentTooLargeException(
      final String message) {
    super(message);
  }

}
