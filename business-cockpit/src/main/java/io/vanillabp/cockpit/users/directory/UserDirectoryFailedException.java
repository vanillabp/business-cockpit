package io.vanillabp.cockpit.users.directory;

/**
 * Thrown when a directory of the cockpit cannot answer, because the remote system cannot be reached
 * or refuses the cockpit. The request which asked fails, and the message says what to check.
 */
public class UserDirectoryFailedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates the exception.
   *
   * @param message What failed and what to check
   * @param cause The failure of the remote system
   */
  public UserDirectoryFailedException(
      final String message,
      final Throwable cause) {

    super(message, cause);

  }

}
