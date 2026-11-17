package io.vanillabp.cockpit.support.rest.httpservice;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * When a token is replaced: when a tenth of its lifetime is left, but at most
 * {@code oauth.renew-before} before it expires.
 */
@ExtendWith(SuppressOutputExtension.class)
public class ClientCredentialsTest {

  private static final Instant ISSUED = Instant.parse("2026-01-31T12:00:00Z");

  private static OAuth2AccessToken livingFor(
      final Duration lifetime) {
    return new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "token", ISSUED, ISSUED.plus(lifetime));
  }

  @Test
  public void a_short_token_is_replaced_when_a_tenth_of_its_lifetime_is_left() {

    final var token = livingFor(Duration.ofSeconds(60));
    final var renewBefore = Duration.ofSeconds(30);

    assertThat(ClientCredentials.isDue(token, ISSUED.plusSeconds(53), renewBefore)).isFalse();
    assertThat(ClientCredentials.isDue(token, ISSUED.plusSeconds(54), renewBefore)).isTrue();

  }

  @Test
  public void a_long_token_is_replaced_renew_before_ahead_of_its_end() {

    final var token = livingFor(Duration.ofHours(1));
    final var renewBefore = Duration.ofSeconds(30);

    assertThat(ClientCredentials.isDue(token, ISSUED.plusSeconds(3569), renewBefore)).isFalse();
    assertThat(ClientCredentials.isDue(token, ISSUED.plusSeconds(3570), renewBefore)).isTrue();

  }

  @Test
  public void a_token_without_an_end_is_never_due() {

    final var token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "token", ISSUED, null);

    assertThat(ClientCredentials.isDue(token, ISSUED.plusSeconds(86_400), Duration.ofSeconds(30))).isFalse();

  }

}
