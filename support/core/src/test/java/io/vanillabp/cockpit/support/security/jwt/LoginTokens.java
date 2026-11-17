package io.vanillabp.cockpit.support.security.jwt;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import java.util.function.Consumer;

import javax.crypto.KeyGenerator;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * Signs login tokens the way the Business Cockpit does, for the tests of both platform modules. The
 * tokens carry the claims the cockpit writes: issuer and audience {@code bc}, the user id as
 * subject, an id, the time of issue and of the login, and an expiry an hour later.
 */
public final class LoginTokens {

  private LoginTokens() {
  }

  /**
   * A new random secret of 256 bits, the length the cockpit's own generated secret has.
   *
   * @return The secret
   */
  public static byte[] newSecret() {

    try {
      return KeyGenerator
          .getInstance("HmacSha256")
          .generateKey()
          .getEncoded();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }

  }

  /**
   * A secret as the setting {@code hmacSHA256-base64} holds it.
   *
   * @param secret The secret
   * @return The secret, Base64 encoded
   */
  public static String base64(
      final byte[] secret) {

    return Base64.getEncoder().encodeToString(secret);

  }

  /**
   * A token for the user, valid for an hour.
   *
   * @param secret  The secret to sign with
   * @param subject The user id
   * @return The signed token, as the cockpit writes it into the cookie
   */
  public static String signed(
      final byte[] secret,
      final String subject) {

    return signed(secret, subject, claims -> {
    });

  }

  /**
   * A token for the user with claims changed or added by the test, for example another expiry.
   *
   * @param secret  The secret to sign with
   * @param subject The user id
   * @param changes Changes the claims the cockpit would write
   * @return The signed token
   */
  public static String signed(
      final byte[] secret,
      final String subject,
      final Consumer<JWTClaimsSet.Builder> changes) {

    final var now = Instant.now();
    final var claims = new JWTClaimsSet.Builder()
        .issuer("bc")
        .audience("bc")
        .subject(subject)
        .jwtID(UUID.randomUUID().toString())
        .issueTime(Date.from(now))
        .expirationTime(Date.from(now.plus(Duration.ofHours(1))))
        .claim("auth_time", now.getEpochSecond());
    changes.accept(claims);

    return signed(secret, JWSAlgorithm.HS256, claims.build());

  }

  /**
   * A token signed with the given algorithm, for a test of a token the cockpit would not write.
   *
   * @param secret    The secret to sign with
   * @param algorithm An HMAC algorithm
   * @param claims    The claims
   * @return The signed token
   */
  public static String signed(
      final byte[] secret,
      final JWSAlgorithm algorithm,
      final JWTClaimsSet claims) {

    try {
      final var jwt = new SignedJWT(new JWSHeader(algorithm), claims);
      jwt.sign(new MACSigner(secret));
      return jwt.serialize();
    } catch (JOSEException e) {
      throw new IllegalStateException(e);
    }

  }

}
