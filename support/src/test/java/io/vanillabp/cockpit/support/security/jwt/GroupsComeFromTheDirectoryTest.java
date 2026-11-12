package io.vanillabp.cockpit.support.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import javax.crypto.KeyGenerator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.OctetSequenceKey;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.vanillabp.cockpit.support.security.usercontext.UserDetailsImpl;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import jakarta.servlet.http.Cookie;

/**
 * The login token names the user, and the groups come from the user directory on every request.
 * So a group the directory takes away is gone on the next request, and a token of an older
 * version, which still names groups, keeps working without its groups being used.
 */
@ExtendWith(SuppressOutputExtension.class)
class GroupsComeFromTheDirectoryTest {

  private JwtProperties properties;

  private UserDirectoryForTests directory;

  private JwtAuthenticationTokenMapper mapper;

  private ListAppender<ILoggingEvent> logged;

  @BeforeEach
  void setUp() throws Exception {

    properties = new JwtProperties();
    properties.setHmacSHA256Base64(Base64
        .getEncoder()
        .encodeToString(KeyGenerator.getInstance("HmacSha256").generateKey().getEncoded()));
    directory = new UserDirectoryForTests().with("john", "ADMIN", "SALES");
    mapper = new JwtAuthenticationTokenMapper(properties, directory, Function.identity());

    logged = new ListAppender<>();
    logged.start();
    mapperLogger().addAppender(logged);

  }

  @AfterEach
  void tearDown() {

    mapperLogger().detachAppender(logged);

  }

  @Test
  void aGroupTakenAwayInTheDirectoryIsGoneOnTheNextRequest() {

    final var token = login(new User("john", "", List.of(new SimpleGrantedAuthority("ADMIN"))));
    assertThat(authoritiesOf(mapper.toAuth(token))).containsExactlyInAnyOrder("ADMIN", "SALES", "USER_john");

    directory.with("john", "SALES");

    assertThat(authoritiesOf(mapper.toAuth(token)))
        .as("the same token, but the directory took ADMIN away")
        .containsExactlyInAnyOrder("SALES", "USER_john");

  }

  @Test
  void anOldTokenWithGroupsIsAcceptedAndItsGroupsAreNotUsed() {

    final var oldToken = tokenOfAnOlderVersion("john", List.of("ADMIN", "FORMER_TEAM"));

    final var auth = mapper.toAuth(oldToken);

    assertThat(auth.getName()).isEqualTo("john");
    assertThat(authoritiesOf(auth))
        .as("the groups of the directory, not those of the claim")
        .containsExactlyInAnyOrder("ADMIN", "SALES", "USER_john");

    directory.with("john");

    assertThat(authoritiesOf(mapper.toAuth(oldToken)))
        .as("the claim still says ADMIN, the directory says nothing")
        .containsExactly("USER_john");

  }

  @Test
  void theGroupHierarchyResolvesTheGroupsOfTheDirectory() {

    final var withHierarchy = new JwtAuthenticationTokenMapper(properties, directory, groups -> {
      final var resolved = new HashSet<>(groups);
      if (groups.contains("ADMIN")) {
        resolved.add("TASK_VIEWER");
      }
      return resolved;
    });

    final var token = login(new User("john", "", List.of()));

    assertThat(authoritiesOf(withHierarchy.toAuth(token)))
        .containsExactlyInAnyOrder("ADMIN", "SALES", "TASK_VIEWER", "USER_john");

  }

  @Test
  void aUserTheDirectoryDoesNotKnowHasOnlyItsOwnAuthorityAndIsWarnedAboutOnceAMinute() {

    final var token = login(new User("john", "", List.of(new SimpleGrantedAuthority("ADMIN"))));
    directory.remove("john");
    final var now = Instant.parse("2026-11-12T10:00:00Z");
    mapper.setClock(Clock.fixed(now, ZoneOffset.UTC));

    final var auth = mapper.toAuth(token);
    mapper.toAuth(token);
    mapper.toAuth(token);

    assertThat(auth.getName()).isEqualTo("john");
    assertThat(authoritiesOf(auth)).containsExactly("USER_john");
    assertThat(auth.getDirectoryUser()).isEmpty();
    assertThat(warnings())
        .as("three requests within a minute give one warning")
        .hasSize(1)
        .first()
        .asString()
        .contains("'john'", "USER_john");

    mapper.setClock(Clock.fixed(now.plus(Duration.ofSeconds(59)), ZoneOffset.UTC));
    mapper.toAuth(token);
    assertThat(warnings()).hasSize(1);

    mapper.setClock(Clock.fixed(now.plus(Duration.ofMinutes(1)), ZoneOffset.UTC));
    mapper.toAuth(token);
    assertThat(warnings())
        .as("a minute later the user is still unknown, which is worth saying again")
        .hasSize(2);

  }

  @Test
  void aFailingDirectoryFailsTheRequestWithALoggedError() {

    final var token = login(new User("john", "", List.of()));
    final var failure = new IllegalStateException("LDAP is down");
    directory.failWith(failure);

    assertThatThrownBy(() -> mapper.toAuth(token)).isSameAs(failure);
    assertThat(logged.list)
        .filteredOn(event -> event.getLevel() == Level.ERROR)
        .singleElement()
        .satisfies(event -> assertThat(event.getFormattedMessage()).contains("'john'"));

    final var request = new MockHttpServletRequest("GET", "/gui/api/v1/user-tasks");
    request.setCookies(new Cookie(properties.getCookie().getName(), token));
    final var filter = new PassiveJwtSecurityFilter(properties, mapper);

    assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain()))
        .as("the request does not go on as if the user had no groups")
        .isSameAs(failure);

  }

  @Test
  void theNameAndTheMailComeFromTheDirectoryAndOtherwiseFromTheToken() {

    directory.with(new UserDetailsImpl("john", "john@example.com", "Doe, John", "Doe, J.", List.of("ADMIN")));
    final var token = tokenOfAnOlderVersion("john", List.of());

    final var fromTheDirectory = new JwtUserDetails(mapper.toAuth(token));

    assertThat(fromTheDirectory.getId()).isEqualTo("john");
    assertThat(fromTheDirectory.getDisplay()).isEqualTo("Doe, John");
    assertThat(fromTheDirectory.getDisplayShort()).isEqualTo("Doe, J.");
    assertThat(fromTheDirectory.getEmail()).isEqualTo("john@example.com");
    assertThat(fromTheDirectory.getAuthorities()).containsExactlyInAnyOrder("ADMIN", "USER_john");

    directory.remove("john");
    final var fromTheToken = new JwtUserDetails(mapper.toAuth(token));

    assertThat(fromTheToken.getDisplay()).isEqualTo("Name Token");
    assertThat(fromTheToken.getEmail()).isEqualTo("token@example.com");

  }

  /**
   * Right after a login by OpenID Connect, the principal is an {@code OidcUser} and no user of
   * Spring Security. Its name is the claim Spring Security is configured to name the user by. That
   * name becomes the subject of the token, and every later request asks the directory by it.
   */
  @Test
  void theIdOfAnOpenIdConnectLoginIsTheIdOfAllLaterRequests() {

    directory.with("a1b2c3", "ADMIN").with("john.doe", "SALES");

    final var bySubject = login(oidcLogin("sub"));
    final var byConfiguredClaim = login(oidcLogin("preferred_username"));

    assertThat(mapper.toAuth(bySubject).getName()).isEqualTo("a1b2c3");
    assertThat(new JwtUserDetails(mapper.toAuth(bySubject)).getId()).isEqualTo("a1b2c3");
    assertThat(authoritiesOf(mapper.toAuth(bySubject))).containsExactlyInAnyOrder("ADMIN", "USER_a1b2c3");

    assertThat(new JwtUserDetails(mapper.toAuth(byConfiguredClaim)).getId()).isEqualTo("john.doe");
    assertThat(authoritiesOf(mapper.toAuth(byConfiguredClaim)))
        .as("the groups of the OpenID Connect login are not carried over, the directory gives them")
        .containsExactlyInAnyOrder("SALES", "USER_john.doe");

  }

  @Test
  void theIdOfALoginByUserNameIsTheIdOfAllLaterRequests() {

    final var token = login(new User("john", "", List.of(new SimpleGrantedAuthority("GROUP_OF_THE_LOGIN"))));

    assertThat(new JwtUserDetails(mapper.toAuth(token)).getId()).isEqualTo("john");
    assertThat(authoritiesOf(mapper.toAuth(token))).doesNotContain("GROUP_OF_THE_LOGIN");

  }

  private static Authentication oidcLogin(
      final String nameAttributeKey) {

    final var now = Instant.now();
    final var idToken = new OidcIdToken("id-token", now, now.plusSeconds(60), Map.of(
        "sub", "a1b2c3",
        "preferred_username", "john.doe",
        "groups", List.of("IDP_GROUP")));
    final var authorities = Set.of(new SimpleGrantedAuthority("IDP_GROUP"));
    final var oidcUser = new DefaultOidcUser(authorities, idToken, nameAttributeKey);
    return new TestingAuthenticationToken(oidcUser, null, List.copyOf(authorities));

  }

  private String login(
      final User user) {

    return login(new UsernamePasswordAuthenticationToken(user, "", user.getAuthorities()));

  }

  private String login(
      final Authentication authentication) {

    return mapper
        .toToken(new SecurityContextImpl(authentication))
        .orElseThrow()
        .getKey();

  }

  private static List<String> authoritiesOf(
      final Authentication auth) {

    return auth
        .getAuthorities()
        .stream()
        .map(GrantedAuthority::getAuthority)
        .toList();

  }

  private List<String> warnings() {

    return logged.list
        .stream()
        .filter(event -> event.getLevel() == Level.WARN)
        .map(ILoggingEvent::getFormattedMessage)
        .toList();

  }

  private static Logger mapperLogger() {

    return (Logger) LoggerFactory.getLogger(JwtAuthenticationTokenMapper.class);

  }

  /**
   * A token the way an older version wrote it, with the groups in the claim
   * {@value JwtMapper#AUTHORITIES_CLAIM}.
   */
  private String tokenOfAnOlderVersion(
      final String subject,
      final List<String> groups) {

    final var now = Instant.now();
    final var claims = JwtClaimsSet
        .builder()
        .issuer("bc")
        .subject(subject)
        .audience(List.of("bc"))
        .id(UUID.randomUUID().toString())
        .issuedAt(now)
        .expiresAt(now.plus(Duration.ofHours(1)))
        .claim("name", "Name Token")
        .claim("email", "token@example.com")
        .claim(JwtMapper.AUTHORITIES_CLAIM, groups)
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
