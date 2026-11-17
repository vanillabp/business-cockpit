package io.vanillabp.cockpit.support.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The cockpit writes its tokens with this module, and a Quarkus workflow module checks them with
 * the core alone. So a token this module signs has to pass the core's check, and a token the core's
 * tests sign has to pass this module's check. Then both platforms accept the same tokens.
 */
@ExtendWith(SuppressOutputExtension.class)
class ATokenOfTheCockpitIsAcceptedByTheCoreVerifierTest {

  private final byte[] secret = LoginTokens.newSecret();

  private JwtProperties properties;

  private JwtAuthenticationTokenMapper mapper;

  @BeforeEach
  void setUp() {

    properties = new JwtProperties();
    properties.setHmacSHA256Base64(LoginTokens.base64(secret));
    mapper = new JwtAuthenticationTokenMapper(
        properties, new UserDirectoryForTests().with("john", "SALES"), Function.identity());

  }

  @Test
  void aTokenTheCockpitWritesPassesTheCoreVerifier() {

    final var user = new User("john", "", List.of(new SimpleGrantedAuthority("SALES")));
    final var token = mapper
        .toToken(new SecurityContextImpl(new UsernamePasswordAuthenticationToken(user, "", user.getAuthorities())))
        .orElseThrow()
        .getKey();

    final var checked = new LoginTokenVerifier(secret).verify(token);

    assertThat(checked.subject()).isEqualTo("john");
    assertThat(checked.claims()).containsKeys("exp", "iat", "jti", JwtMapper.LOGIN_TIME_CLAIM);
    assertThat(checked.claimAsString("iss")).isEqualTo("bc");

  }

  @Test
  void aTokenTheCoreTestsSignPassesThisModule() {

    final var token = LoginTokens.signed(secret, "john", claims -> claims.claim("email", "john@example.com"));

    final var auth = mapper.toAuth(token);

    assertThat(auth.getName()).isEqualTo("john");
    assertThat(auth.getJwt().getExpiresAt()).isAfter(Instant.now());
    assertThat(auth.getJwt().getIssuedAt()).isBeforeOrEqualTo(Instant.now());
    assertThat(auth.getJwt().getAudience()).containsExactly("bc");
    assertThat(new JwtUserDetails(auth).getEmail()).isEqualTo("john@example.com");
    assertThat(new JwtUserDetails(auth).getAuthorities()).containsExactlyInAnyOrder("SALES", "USER_john");

  }

  @Test
  void anExpiredTokenIsAValidationErrorAndAForeignOneABadToken() {

    final var expired = LoginTokens.signed(secret, "john", claims -> claims
        .expirationTime(Date.from(Instant.now().minus(Duration.ofMinutes(5)))));
    final var foreign = LoginTokens.signed(LoginTokens.newSecret(), "john");

    assertThatThrownBy(() -> mapper.toAuth(expired))
        .as("an expired token is normal and only logged at debug level by the filter")
        .isInstanceOf(JwtValidationException.class);
    assertThatThrownBy(() -> mapper.toAuth(foreign))
        .as("a token signed with another secret is worth a warning")
        .isInstanceOf(BadJwtException.class)
        .isNotInstanceOf(JwtValidationException.class);

  }

}
