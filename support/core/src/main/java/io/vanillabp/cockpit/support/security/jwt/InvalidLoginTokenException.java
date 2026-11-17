package io.vanillabp.cockpit.support.security.jwt;

import java.io.Serial;

/**
 * Says that a login token is not valid: it cannot be read, its signature does not match the key, it
 * has expired or it is not valid yet. A request with such a token is a request without a login.
 */
public class InvalidLoginTokenException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Whether the only fault of the token is its time.
   */
  private final boolean expired;

  /**
   * Creates the exception.
   *
   * @param message Why the token is not valid
   * @param expired Whether the only fault of the token is its time: expired or not valid yet
   * @param cause   What the token library reported, or {@code null}
   */
  public InvalidLoginTokenException(
      final String message,
      final boolean expired,
      final Throwable cause) {

    super(message, cause);
    this.expired = expired;

  }

  /**
   * Whether the token was a good token once, or will be one, and only its time does not fit. Such a
   * token is normal, for example after a user came back from a long break, so it is not worth a
   * warning.
   *
   * @return {@code true} if the token has expired or is not valid yet
   */
  public boolean isExpired() {

    return expired;

  }

}
