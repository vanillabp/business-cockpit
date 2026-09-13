package io.vanillabp.cockpit.commons.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.crypto.KeyGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.web.server.Cookie.SameSite;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.context.HttpRequestResponseHolder;

/**
 * The token path of the cockpit. Signing in once writes the JWT cookie, and every request after it
 * has to come back as that user, which is what keeps a single page application from asking for
 * credentials again on every call.
 */
@ExtendWith(SuppressOutputExtension.class)
class JwtSecurityContextRepositoryTest {

    private JwtProperties properties;

    private JwtSecurityContextRepository repository;

    @BeforeEach
    void setUp() throws Exception {

        properties = new JwtProperties();
        properties.setHmacSHA256Base64(generatedHmacKey());
        properties.getCookie().setName("bc");
        properties.getCookie().setPath("/");
        properties.getCookie().setExpiresDuration("PT12H");
        properties.getCookie().setSameSite(SameSite.STRICT);

        repository = new JwtSecurityContextRepository(
                properties,
                new JwtAuthenticationTokenMapper(properties));

    }

    @Test
    void theUserSignedInIsReadBackFromTheCookie() {

        final var request = new MockHttpServletRequest();
        request.setCookies(cookieWrittenFor("john", "ADMIN"));

        final var deferred = repository.loadDeferredContext(request);

        assertThat(deferred.isGenerated())
                .as("a context read from a token was not made up by the repository")
                .isFalse();
        final var authentication = deferred.get().getAuthentication();
        assertThat(authentication.getName()).isEqualTo("john");
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains("ADMIN");

    }

    @Test
    void theCookieCarriesWhatTheConfigurationSays() {

        final var cookie = cookieWrittenFor("john", "ADMIN");

        assertThat(cookie.getName()).isEqualTo("bc");
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge())
                .as("the cookie lives as long as the token in it")
                .isBetween(12 * 60 * 60, 12 * 60 * 60 + 10);
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("Strict");

    }

    @Test
    void aRequestWithoutTheCookieGetsAnEmptyContext() {

        final var deferred = repository.loadDeferredContext(new MockHttpServletRequest());

        assertThat(deferred.get())
                .as("the interface asks for a context, never for null")
                .isNotNull();
        assertThat(deferred.get().getAuthentication()).isNull();
        assertThat(deferred.isGenerated())
                .as("an empty context the repository made up says so")
                .isTrue();

    }

    @Test
    @SuppressWarnings("deprecation")
    void aRequestWithoutTheCookieGetsAnEmptyContextOnTheOldReadPathToo() {

        final var context = repository.loadContext(
                new HttpRequestResponseHolder(new MockHttpServletRequest(), new MockHttpServletResponse()));

        assertThat(context)
                .as("a caller written against the old method gets a context as well")
                .isNotNull();
        assertThat(context.getAuthentication()).isNull();

    }

    @Test
    void anExpiredTokenIsRefusedWhereTheContextIsAskedFor() {

        final var request = new MockHttpServletRequest();
        request.setCookies(new Cookie(properties.getCookie().getName(), tokenWhichExpiredAnHourAgo()));

        final var deferred = repository.loadDeferredContext(request);

        assertThatThrownBy(deferred::get)
                .as("an expired token is no authentication, and the filter chain has to hear about it")
                .isInstanceOf(JwtValidationException.class);

    }

    @Test
    void theCookieIsReadWhenTheContextIsAskedForAndOnlyOnce() {

        final var request = new CookieReadCountingRequest();
        request.setCookies(cookieWrittenFor("john", "ADMIN"));

        final var deferred = repository.loadDeferredContext(request);

        assertThat(request.reads)
                .as("handing out a deferred context must not decode a token yet")
                .isZero();

        deferred.get();
        deferred.get();

        assertThat(request.reads)
                .as("the first answer is kept rather than the token being decoded again")
                .isOne();

    }

    @Test
    void containsContextSaysWhetherTheCookieIsThere() {

        final var withCookie = new MockHttpServletRequest();
        withCookie.setCookies(cookieWrittenFor("john", "ADMIN"));

        assertThat(repository.containsContext(withCookie)).isTrue();
        assertThat(repository.containsContext(new MockHttpServletRequest())).isFalse();

    }

    @Test
    void loggingOutWritesTheCookieAwayAgain() {

        final var response = new MockHttpServletResponse();

        JwtSecurityContextRepository.clearCookie(properties, response);

        final var cookie = response.getCookie(properties.getCookie().getName());
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge())
                .as("a cookie which expired at once is one the browser drops")
                .isZero();

    }

    /**
     * The cookie the cockpit hands out on a successful sign in, which is the input of every read
     * the tests above do.
     */
    private Cookie cookieWrittenFor(
            final String userId,
            final String group) {

        final var response = new MockHttpServletResponse();
        repository.saveContext(contextOf(userId, group), new MockHttpServletRequest(), response);

        final var cookie = response.getCookie(properties.getCookie().getName());
        assertThat(cookie).as("signing in has to write the cookie").isNotNull();
        return cookie;

    }

    /**
     * What the basic authentication filter hands over once it has checked the credentials.
     */
    private SecurityContext contextOf(
            final String userId,
            final String group) {

        final var authorities = List.of(new SimpleGrantedAuthority(group));
        final var user = new User(userId, "", authorities);
        return new SecurityContextImpl(
                new UsernamePasswordAuthenticationToken(user, "", authorities));

    }

    /**
     * A token of a user who signed in yesterday and left the browser open. The repository cannot
     * write one, because it always dates a token from now on, so the test signs it itself with the
     * same key.
     */
    private String tokenWhichExpiredAnHourAgo() {

        final var claims = JwtClaimsSet
                .builder()
                .issuer("bc")
                .subject("john")
                .audience(List.of("bc"))
                .id(UUID.randomUUID().toString())
                .issuedAt(Instant.now().minus(Duration.ofHours(2)))
                .expiresAt(Instant.now().minus(Duration.ofHours(1)))
                .build();

        final var jwk = new OctetSequenceKey
                .Builder(properties.getHmacSHA256())
                .keyID(UUID.randomUUID().toString())
                .algorithm(JWSAlgorithm.HS256)
                .build();

        return new NimbusJwtEncoder((jwkSelector, context) -> List.of(jwk))
                .encode(JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(),
                        claims))
                .getTokenValue();

    }

    private String generatedHmacKey() throws Exception {

        return Base64
                .getEncoder()
                .encodeToString(KeyGenerator.getInstance("HmacSha256").generateKey().getEncoded());

    }

    /**
     * Counts how often the cookies of a request are looked at, which is what "deferred" comes down
     * to from the outside.
     */
    private static final class CookieReadCountingRequest extends MockHttpServletRequest {

        private int reads;

        @Override
        public Cookie[] getCookies() {

            reads++;
            return super.getCookies();

        }

    }

}
