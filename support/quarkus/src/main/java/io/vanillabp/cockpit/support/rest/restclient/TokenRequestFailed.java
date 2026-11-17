package io.vanillabp.cockpit.support.rest.restclient;

import jakarta.ws.rs.ProcessingException;

/**
 * A request could not be sent, because the OAuth token endpoint gave no token: it refused, it
 * could not be reached, or its answer was no token. The message names the key which is most likely
 * wrong. It is a {@link ProcessingException}, like every other failure to send a request of
 * Quarkus' REST client, but it is never retried: the next attempt would get the same answer.
 */
public class TokenRequestFailed extends ProcessingException {

  private static final long serialVersionUID = 1L;

  /**
   * Explains what went wrong.
   *
   * @param message what went wrong and which key to check
   */
  public TokenRequestFailed(
      final String message) {
    super(message);
  }

}
