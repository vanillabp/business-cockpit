package io.vanillabp.cockpit.support.security.jwt;

import java.util.Map;

/**
 * A login token of the Business Cockpit whose signature and lifetime {@link LoginTokenVerifier}
 * checked. The claims {@code exp}, {@code iat} and {@code nbf} are {@link java.time.Instant}s, all
 * other claims are as the token holds them.
 *
 * @param value   The token as it was sent in the cookie
 * @param headers The headers of the token, for example {@code alg}
 * @param claims  The claims of the token, for example {@code sub}
 */
public record LoginToken(
                         String value,
                         Map<String, Object> headers,
                         Map<String, Object> claims) {

  /**
   * Keeps unmodifiable copies of the headers and the claims.
   *
   * @param value   The token as it was sent in the cookie
   * @param headers The headers of the token
   * @param claims  The claims of the token
   */
  public LoginToken {

    headers = Map.copyOf(headers);
    claims = Map.copyOf(claims);

  }

  /**
   * The id of the user who logged in. The cockpit writes it as the subject of the token.
   *
   * @return The user id
   */
  public String subject() {

    return claimAsString("sub");

  }

  /**
   * A claim as text.
   *
   * @param name The name of the claim
   * @return The claim as text, or {@code null} if the token does not hold it
   */
  public String claimAsString(
      final String name) {

    return asString(claims.get(name));

  }

  static String asString(
      final Object claim) {

    return claim == null
        ? null
        : claim.toString();

  }

}
