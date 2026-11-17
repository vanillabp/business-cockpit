package io.vanillabp.cockpit.support.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A workflow module accepts exactly the tokens the cockpit signs with the shared secret, while they
 * are valid, and refuses every other token with a reason.
 */
@ExtendWith(SuppressOutputExtension.class)
class LoginTokenVerifierTest {

  private final byte[] secret = LoginTokens.newSecret();

  @Test
  void aTokenOfTheCockpitIsAcceptedWithItsClaims() {

    final var token = LoginTokens.signed(secret, "john", claims -> claims.claim("name", "John Doe"));

    final var checked = new LoginTokenVerifier(secret).verify(token);

    assertThat(checked.value()).isEqualTo(token);
    assertThat(checked.subject()).isEqualTo("john");
    assertThat(checked.claimAsString("name")).isEqualTo("John Doe");
    assertThat(checked.claimAsString("missing")).isNull();
    assertThat(checked.claims().get("exp")).isInstanceOf(Instant.class);
    assertThat(checked.claims().get("iat")).isInstanceOf(Instant.class);
    assertThat(checked.headers()).containsEntry("alg", "HS256");

  }

  @Test
  void aTokenSignedWithAnotherSecretIsRefused() {

    final var token = LoginTokens.signed(LoginTokens.newSecret(), "john");

    assertThatThrownBy(() -> new LoginTokenVerifier(secret).verify(token))
        .isInstanceOf(InvalidLoginTokenException.class)
        .hasMessageContaining("signature")
        .matches(e -> !((InvalidLoginTokenException) e).isExpired());

  }

  @Test
  void anExpiredTokenIsRefusedAsExpired() {

    final var expiry = Instant.parse("2026-11-17T10:00:00Z");
    final var token = LoginTokens.signed(secret, "john", claims -> claims.expirationTime(Date.from(expiry)));

    assertThatThrownBy(() -> verifierAt(expiry.plus(Duration.ofSeconds(61))).verify(token))
        .isInstanceOf(InvalidLoginTokenException.class)
        .hasMessageContaining("expired")
        .matches(e -> ((InvalidLoginTokenException) e).isExpired());

  }

  @Test
  void aTokenExpiredLessThanAMinuteAgoIsAcceptedBecauseClocksDiffer() {

    final var expiry = Instant.parse("2026-11-17T10:00:00Z");
    final var token = LoginTokens.signed(secret, "john", claims -> claims.expirationTime(Date.from(expiry)));

    assertThat(verifierAt(expiry.plus(Duration.ofSeconds(59))).verify(token).subject()).isEqualTo("john");

  }

  @Test
  void aTokenWhichIsNotValidYetIsRefused() {

    final var now = Instant.parse("2026-11-17T10:00:00Z");
    final var token = LoginTokens.signed(secret, "john", claims -> claims
        .notBeforeTime(Date.from(now.plus(Duration.ofMinutes(5))))
        .expirationTime(Date.from(now.plus(Duration.ofHours(1)))));

    assertThatThrownBy(() -> verifierAt(now).verify(token))
        .isInstanceOf(InvalidLoginTokenException.class)
        .hasMessageContaining("not valid before")
        .matches(e -> ((InvalidLoginTokenException) e).isExpired());

    assertThat(verifierAt(now.plus(Duration.ofMinutes(4))).verify(token).subject())
        .as("a minute early is within the tolerance")
        .isEqualTo("john");

  }

  @Test
  void aTokenSignedWithAnotherAlgorithmIsRefused() {

    final var longSecret = new byte[64];
    System.arraycopy(secret, 0, longSecret, 0, secret.length);
    System.arraycopy(secret, 0, longSecret, secret.length, secret.length);
    final var token = LoginTokens.signed(longSecret, JWSAlgorithm.HS512, new JWTClaimsSet.Builder()
        .subject("john")
        .build());

    assertThatThrownBy(() -> new LoginTokenVerifier(longSecret).verify(token))
        .isInstanceOf(InvalidLoginTokenException.class)
        .hasMessageContaining("'HS512'")
        .hasMessageContaining("'HS256'");

  }

  @Test
  void anUnsignedTokenIsRefused() {

    final var token = new PlainJWT(new JWTClaimsSet.Builder().subject("john").build()).serialize();

    assertThatThrownBy(() -> new LoginTokenVerifier(secret).verify(token))
        .isInstanceOf(InvalidLoginTokenException.class)
        .hasMessageContaining("cannot be read");

  }

  @Test
  void textWhichIsNoTokenIsRefused() {

    assertThatThrownBy(() -> new LoginTokenVerifier(secret).verify("no-token"))
        .isInstanceOf(InvalidLoginTokenException.class)
        .hasMessageContaining("cannot be read")
        .matches(e -> !((InvalidLoginTokenException) e).isExpired());

  }

  @Test
  void aSecretWhichIsTooShortIsExplained() {

    assertThatThrownBy(() -> new LoginTokenVerifier(new byte[16]))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("128 bits")
        .hasMessageContaining("at least 256 bits");

  }

  @Test
  void theExplanationOfAMissingSecretSuggestsOne() {

    final var explanation = LoginTokenVerifier.explainTheMissingSecret("business-cockpit.jwt.hmacSHA256-base64");

    assertThat(explanation).startsWith("No property 'business-cockpit.jwt.hmacSHA256-base64' set!");
    final var suggested = explanation.replaceAll("(?s).*key: '([^']+)'.*", "$1");
    assertThat(Base64.getDecoder().decode(suggested))
        .as("the suggested secret works with the verifier")
        .hasSize(32);
    new LoginTokenVerifier(Base64.getDecoder().decode(suggested));

  }

  private LoginTokenVerifier verifierAt(
      final Instant now) {

    return new LoginTokenVerifier(secret, Clock.fixed(now, ZoneOffset.UTC));

  }

}
