package io.vanillabp.cockpit.commons.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.oauth2.jwt.Jwt;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The details of a user come from the claims of the login token. The subject is the user id. An
 * e-mail address is a claim of its own, and a token without it says nothing about the e-mail.
 */
@ExtendWith(SuppressOutputExtension.class)
class JwtUserDetailsTest {

  private static JwtUserDetails detailsOf(
      final Map<String, Object> claims) {

    final var jwt = Jwt
        .withTokenValue("token")
        .header("alg", "HS256")
        .subject("u4711")
        .issuedAt(Instant.now())
        .claims(all -> all.putAll(claims))
        .build();
    return new JwtUserDetails(new JwtAuthenticationToken(jwt, List.of()));

  }

  @Test
  void theEmailIsTheClaimOfThatName() {

    final var user = detailsOf(Map.of("email", "petra.huber@example.com"));

    assertThat(user.getId()).isEqualTo("u4711");
    assertThat(user.getEmail()).isEqualTo("petra.huber@example.com");

  }

  @Test
  void aTokenWithoutAnEmailGivesNone() {

    final var user = detailsOf(Map.of("name", "Petra Huber"));

    assertThat(user.getEmail()).isNull();

  }

}
