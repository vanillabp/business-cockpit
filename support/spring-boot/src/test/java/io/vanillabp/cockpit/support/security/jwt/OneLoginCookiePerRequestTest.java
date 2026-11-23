package io.vanillabp.cockpit.support.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import javax.crypto.KeyGenerator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.OctetSequenceKey;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import jakarta.servlet.http.Cookie;

/**
 * A request which carries the login cookie more than once is answered as one user only if the
 * cookies agree on the user. Cookies of two different users give no user at all, so the security
 * rules answer 401 where a login is needed. A browser may keep an old cookie when the path or the
 * domain of the cookie changed. A cookie of the same user, or one which is not valid any more, does
 * not stand in the way.
 */
@ExtendWith(SuppressOutputExtension.class)
class OneLoginCookiePerRequestTest {

  private JwtProperties properties;

  private JwtAuthenticationTokenMapper mapper;

  @BeforeEach
  void setUp() throws Exception {

    properties = new JwtProperties();
    properties.setHmacSHA256Base64(Base64
        .getEncoder()
        .encodeToString(KeyGenerator.getInstance("HmacSha256").generateKey().getEncoded()));
    mapper = new JwtAuthenticationTokenMapper(properties, new UserDirectoryForTests()
        .with("alice", "CLERK")
        .with("bob", "CLERK"), Function.identity());

  }

  @Test
  void aRequestWithTheLoginCookiesOfTwoUsersGoesOnWithoutAUser() throws Exception {

    final var users = usersOf(login("alice"), login("bob"));

    assertThat(users)
        .as("the cookies name two users, so the request is answered as neither of them")
        .containsExactly((String) null);

  }

  @Test
  void aRequestWithOneLoginCookieGoesOnAsItsUser() throws Exception {

    assertThat(usersOf(login("bob"))).containsExactly("bob");

  }

  @Test
  void aRequestWithTwoLoginCookiesOfTheSameUserGoesOnAsThatUser() throws Exception {

    assertThat(usersOf(login("bob"), login("bob"))).containsExactly("bob");

  }

  /**
   * The cookie of an earlier login stays in the browser if the path or the domain of the cookie
   * changed since. Once it expired, it does not stand in the way of the current login.
   */
  @Test
  void anExpiredLoginCookieNextToAValidOneIsLeftOut() throws Exception {

    final var expired = signed("alice", Instant.now().minus(Duration.ofHours(13)),
        Instant.now().minus(Duration.ofHours(1)));

    assertThat(usersOf(expired, login("bob"))).containsExactly("bob");

  }

  @Test
  void aRequestWithTwoLoginCookiesWhichAreBothExpiredGoesOnWithoutAUser() throws Exception {

    final var first = signed("alice", Instant.now().minus(Duration.ofHours(13)),
        Instant.now().minus(Duration.ofHours(1)));
    final var second = signed("bob", Instant.now().minus(Duration.ofHours(14)),
        Instant.now().minus(Duration.ofHours(2)));

    assertThat(usersOf(first, second)).containsExactly((String) null);

  }

  /**
   * The repository reads the cookie the same way, for an application which makes it the context
   * repository of its own chain.
   */
  @Test
  void theContextRepositoryGivesNoUserForTheLoginCookiesOfTwoUsers() {

    final var request = requestWith(login("alice"), login("bob"));

    final var context = new JwtSecurityContextRepository(properties, mapper)
        .loadDeferredContext(request)
        .get();

    assertThat(context.getAuthentication()).isNull();

  }

  /** The user each request went on with, {@code null} for none. */
  private List<String> usersOf(
      final String... tokens) throws Exception {

    final var users = new ArrayList<String>();
    new PassiveJwtSecurityFilter(properties, mapper).doFilter(requestWith(tokens), new MockHttpServletResponse(), (
        request,
        response) -> {
      final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      users.add(authentication == null ? null : authentication.getName());
    });
    return users;

  }

  private MockHttpServletRequest requestWith(
      final String... tokens) {

    final var request = new MockHttpServletRequest("GET", "/gui/api/v1/app/current-user");
    request.setCookies(Arrays
        .stream(tokens)
        .map(token -> new Cookie(properties.getCookie().getName(), token))
        .toArray(Cookie[]::new));
    return request;

  }

  private String login(
      final String userId) {

    final var context = new SecurityContextImpl(new UsernamePasswordAuthenticationToken(
        new User(userId, "", List.of()), "", List.of()));
    return mapper.toToken(context).orElseThrow().getKey();

  }

  private String signed(
      final String userId,
      final Instant issuedAt,
      final Instant expiresAt) {

    final var claims = JwtClaimsSet
        .builder()
        .issuer("bc")
        .subject(userId)
        .audience(List.of("bc"))
        .id(UUID.randomUUID().toString())
        .issuedAt(issuedAt)
        .expiresAt(expiresAt)
        .claim(JwtMapper.LOGIN_TIME_CLAIM, issuedAt.getEpochSecond())
        .build();
    final var jwk = new OctetSequenceKey.Builder(properties.getHmacSHA256())
        .keyID(UUID.randomUUID().toString())
        .algorithm(JWSAlgorithm.HS256)
        .build();
    return new NimbusJwtEncoder((
        jwkSelector,
        context) -> List.of(jwk))
        .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
        .getTokenValue();

  }

}
