package io.vanillabp.cockpit.devshell.simulator.usermanagement;

import static org.assertj.core.api.Assertions.assertThat;

import io.vanillabp.cockpit.commons.security.jwt.JwtAuthenticationTokenMapper;
import io.vanillabp.cockpit.commons.security.jwt.JwtProperties;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import jakarta.servlet.http.Cookie;
import java.util.Base64;
import java.util.List;
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
import org.springframework.security.core.context.TransientSecurityContext;
import org.springframework.security.core.userdetails.User;

/**
 * The token path of the dev shell. Switching the user writes a JWT cookie, and every request after
 * it has to come back as that user. The moment that stops holding, the dev shell shows the wrong
 * person or nobody at all.
 */
@ExtendWith(SuppressOutputExtension.class)
class JwtServerSecurityContextRepositoryTest {

    private JwtProperties properties;

    private JwtServerSecurityContextRepository repository;

    @BeforeEach
    void setUp() throws Exception {

        properties = new JwtProperties();
        properties.setHmacSHA256Base64(generatedHmacKey());
        properties.getCookie().setName("dev-shell");
        properties.getCookie().setPath("/dev-shell");
        properties.getCookie().setExpiresDuration("PT2H");
        properties.getCookie().setSameSite(SameSite.STRICT);

        repository =
                new JwtServerSecurityContextRepository(
                        properties, new JwtAuthenticationTokenMapper(properties));

    }

    @Test
    void theUserSwitchedToIsReadBackFromTheCookie() {

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

        assertThat(cookie.getName()).isEqualTo("dev-shell");
        assertThat(cookie.getPath()).isEqualTo("/dev-shell");
        assertThat(cookie.getMaxAge()).isEqualTo(2 * 60 * 60);
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

    private Cookie cookieWrittenFor(
            final String userId,
            final String group) {

        final var response = new MockHttpServletResponse();
        repository.saveContext(contextOf(userId, group), new MockHttpServletRequest(), response);

        final var cookie = response.getCookie(properties.getCookie().getName());
        assertThat(cookie).as("switching the user has to write the cookie").isNotNull();
        return cookie;

    }

    /**
     * What the dev shell hands over when somebody picks a user from its dropdown.
     */
    private SecurityContext contextOf(
            final String userId,
            final String group) {

        final var authorities = List.of(new SimpleGrantedAuthority(group));
        final var user = new User(userId, "", authorities);
        return new TransientSecurityContext(
                new UsernamePasswordAuthenticationToken(user, "", authorities));

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
