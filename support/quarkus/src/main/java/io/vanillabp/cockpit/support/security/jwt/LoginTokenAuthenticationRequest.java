package io.vanillabp.cockpit.support.security.jwt;

import io.quarkus.security.identity.request.BaseAuthenticationRequest;

/**
 * Asks {@link LoginTokenIdentityProvider} who logged in with the token of the cockpit's cookie.
 */
public class LoginTokenAuthenticationRequest extends BaseAuthenticationRequest {

  private final String token;

  /**
   * Creates the request.
   *
   * @param token The token as it was sent in the cookie
   */
  public LoginTokenAuthenticationRequest(
      final String token) {

    this.token = token;

  }

  /**
   * The token as it was sent in the cookie.
   *
   * @return The token
   */
  public String getToken() {

    return token;

  }

}
