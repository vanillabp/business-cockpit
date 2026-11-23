package io.vanillabp.cockpit.support.security.jwt;

import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;

/**
 * Checks a login token of the Business Cockpit. The cockpit writes the token into a cookie after a
 * login and signs it with HMAC SHA-256 and a secret it shares with every workflow module behind it.
 * A workflow module checks the token with the same secret.
 * <p>
 * A token is valid if it is signed with HS256 and this secret, and if it has not expired and is
 * valid already. A clock of the cockpit and one of the workflow module may differ a little, so
 * both times get a minute of tolerance, the same Spring Security gives.
 * <p>
 * The verifier knows no platform. The Spring Boot module and the Quarkus module of the support
 * both check a token with it, so both accept the same tokens.
 */
public class LoginTokenVerifier {

  /**
   * How far the clock of the cockpit and the clock of a workflow module may differ.
   */
  static final Duration CLOCK_SKEW = Duration.ofSeconds(60);

  private final MACVerifier signatureVerifier;

  private final Clock clock;

  /**
   * Creates a verifier which checks the time of a token against the clock of the system.
   *
   * @param secret The secret the cockpit signs its tokens with, at least 256 bits long
   * @throws IllegalArgumentException If the secret is shorter than 256 bits
   */
  public LoginTokenVerifier(
      final byte[] secret) {

    this(secret, Clock.systemUTC());

  }

  /**
   * Creates a verifier which checks the time of a token against the given clock.
   *
   * @param secret The secret the cockpit signs its tokens with, at least 256 bits long
   * @param clock  The clock which says whether a token has expired
   * @throws IllegalArgumentException If the secret is shorter than 256 bits
   */
  public LoginTokenVerifier(
      final byte[] secret,
      final Clock clock) {

    try {
      this.signatureVerifier = new MACVerifier(secret);
    } catch (JOSEException e) {
      throw new IllegalArgumentException("""
          The secret for the login tokens is %d bits long, but HMAC SHA-256 needs at least 256 \
          bits. Use the secret the Business Cockpit signs its tokens with."""
          .formatted(secret.length * 8), e);
    }
    this.clock = clock;

  }

  /**
   * Checks the signature and the time of a token and reads its headers and claims.
   *
   * @param token The token as it was sent in the cookie
   * @return The checked token
   * @throws InvalidLoginTokenException If the token cannot be read, is not signed with HS256 and the
   *                                    secret, has expired or is not valid yet
   */
  public LoginToken verify(
      final String token) throws InvalidLoginTokenException {

    final SignedJWT jwt;
    final Map<String, Object> claims;
    try {
      jwt = SignedJWT.parse(token);
      claims = jwt.getJWTClaimsSet().getClaims();
    } catch (ParseException e) {
      throw new InvalidLoginTokenException("The login token cannot be read: "
          + e.getMessage(), false, e);
    }

    final var algorithm = jwt.getHeader().getAlgorithm();
    if (!JWSAlgorithm.HS256.equals(algorithm)) {
      throw new InvalidLoginTokenException(
          "The login token is signed with '%s', but the Business Cockpit signs with 'HS256'"
              .formatted(algorithm), false, null);
    }
    try {
      if (!jwt.verify(signatureVerifier)) {
        throw new InvalidLoginTokenException(
            "The signature of the login token does not match the secret", false, null);
      }
    } catch (JOSEException e) {
      throw new InvalidLoginTokenException(
          "The signature of the login token cannot be checked: "
              + e.getMessage(), false, e);
    }

    final var now = clock.instant();
    final var expiresAt = instantOf(claims.get("exp"));
    if ((expiresAt != null) && now.minus(CLOCK_SKEW).isAfter(expiresAt)) {
      throw new InvalidLoginTokenException("The login token expired at "
          + expiresAt, true, null);
    }
    final var notBefore = instantOf(claims.get("nbf"));
    if ((notBefore != null) && now.plus(CLOCK_SKEW).isBefore(notBefore)) {
      throw new InvalidLoginTokenException("The login token is not valid before "
          + notBefore, true, null);
    }

    return new LoginToken(
        token, jwt.getHeader().toJSONObject(), withInstants(claims));

  }

  /**
   * The text which explains that the secret is missing. It says how to create a secret, and it
   * holds none: a secret written to a log is known to everybody who reads the log.
   *
   * @param property The name of the setting which holds the secret, Base64 encoded
   * @return The explanation
   */
  public static String explainTheMissingSecret(
      final String property) {

    return "No property '"
        + property
        + "' set! It holds the secret the Business Cockpit signs its login tokens with, at least "
        + "256 bits in Base64. A new secret is created by 'openssl rand -base64 32'.";

  }

  private static Map<String, Object> withInstants(
      final Map<String, Object> claims) {

    final var result = new LinkedHashMap<String, Object>();
    claims.forEach((
        name,
        value) -> {
      if (value != null) {
        result.put(name, value instanceof Date date ? date.toInstant() : value);
      }
    });
    return result;

  }

  private static Instant instantOf(
      final Object claim) {

    return claim instanceof Date date
        ? date.toInstant()
        : null;

  }

}
