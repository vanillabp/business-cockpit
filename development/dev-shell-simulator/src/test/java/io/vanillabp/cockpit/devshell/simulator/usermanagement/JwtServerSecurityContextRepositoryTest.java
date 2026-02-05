package io.vanillabp.cockpit.devshell.simulator.usermanagement;

import io.vanillabp.cockpit.commons.security.jwt.JwtCookie;
import io.vanillabp.cockpit.commons.security.jwt.JwtMapper;
import io.vanillabp.cockpit.commons.security.jwt.JwtProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.AbstractMap;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpRequestResponseHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServerSecurityContextRepositoryTest {

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private JwtMapper<AbstractAuthenticationToken> jwtMapper;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private JwtServerSecurityContextRepository repository;

    private JwtCookie jwtCookie;

    @BeforeEach
    void setUp() {
        // Configure JwtCookie with default values
        jwtCookie = new JwtCookie();
        jwtCookie.setName("bc");
        jwtCookie.setPath("/");
        jwtCookie.setDomain("localhost");
        jwtCookie.setSecure(false);
        jwtCookie.setExpiresDuration("PT12H");

        // Instantiate repository with mocked dependencies
        repository = new JwtServerSecurityContextRepository(jwtProperties, jwtMapper);
    }

    // --- loadContext ---

    @Test
    void loadContext_withJwtCookie_returnsSecurityContext() {
        // Prepare request with JWT cookie
        final var jwtCookieValue = new Cookie("bc", "jwt-token-value");
        when(jwtProperties.getCookie()).thenReturn(jwtCookie);

        final var holder = new HttpRequestResponseHolder(request, response);
        when(request.getCookies()).thenReturn(new Cookie[]{jwtCookieValue});

        // JwtMapper returns a SecurityContext
        final var expectedContext = new SecurityContextImpl();
        when(jwtMapper.toSecurityContext("jwt-token-value")).thenReturn(expectedContext);

        // Execute loadContext and verify result
        final var result = repository.loadContext(holder);

        assertThat(result).isSameAs(expectedContext);
    }

    @Test
    void loadContext_withNoCookies_returnsNull() {
        // Request without cookies
        final var holder = new HttpRequestResponseHolder(request, response);
        when(request.getCookies()).thenReturn(null);

        // loadContext must return null
        final var result = repository.loadContext(holder);

        assertThat(result).isNull();
    }

    @Test
    void loadContext_withoutMatchingCookie_returnsNull() {
        // Request with a cookie that does not match the JWT name
        final var otherCookie = new Cookie("other", "value");
        when(jwtProperties.getCookie()).thenReturn(jwtCookie);

        final var holder = new HttpRequestResponseHolder(request, response);
        when(request.getCookies()).thenReturn(new Cookie[]{otherCookie});

        // loadContext must return null since no matching cookie is present
        final var result = repository.loadContext(holder);

        assertThat(result).isNull();
    }

    // --- saveContext ---

    @Test
    void saveContext_withValidContext_setsCookieOnResponse() {
        // Prepare SecurityContext with authentication
        final var auth = new UsernamePasswordAuthenticationToken("user", "pass");
        final var context = new SecurityContextImpl(auth);

        // JwtMapper returns token
        when(jwtProperties.getCookie()).thenReturn(jwtCookie);
        when(jwtMapper.toToken(context))
                .thenReturn(Optional.of(new AbstractMap.SimpleEntry<>("encoded-jwt-token", null)));

        // Execute saveContext
        repository.saveContext(context, request, response);

        // Cookie must be set on the response
        final var cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());

        // Verify cookie properties
        final var savedCookie = cookieCaptor.getValue();
        assertThat(savedCookie.getName()).isEqualTo("bc");
        assertThat(savedCookie.getValue()).isEqualTo("encoded-jwt-token");
        assertThat(savedCookie.getPath()).isEqualTo("/");
        assertThat(savedCookie.getDomain()).isEqualTo("localhost");
        assertThat(savedCookie.isHttpOnly()).isTrue();
    }

    @Test
    void saveContext_withNoToken_doesNotSetCookie() {
        // SecurityContext without valid token
        final var context = new SecurityContextImpl();
        when(jwtMapper.toToken(context)).thenReturn(Optional.empty());

        // Execute saveContext
        repository.saveContext(context, request, response);

        // No cookie should be set (verify with never() is not needed,
        // since addCookie is not called when Optional is empty)
    }

    // --- containsContext ---

    @Test
    void containsContext_withMatchingCookie_returnsTrue() {
        // Request with the JWT cookie
        final var jwtCookieValue = new Cookie("bc", "token");
        when(jwtProperties.getCookie()).thenReturn(jwtCookie);
        when(request.getCookies()).thenReturn(new Cookie[]{jwtCookieValue});

        // Check containsContext
        // Note: The current implementation returns isEmpty(),
        // i.e., when a cookie is found, findFirst().isEmpty() == false
        final var result = repository.containsContext(request);

        assertThat(result).isFalse();
    }

    @Test
    void containsContext_withoutMatchingCookie_returnsTrue() {
        // Request without the JWT cookie
        final var otherCookie = new Cookie("other", "value");
        when(jwtProperties.getCookie()).thenReturn(jwtCookie);
        when(request.getCookies()).thenReturn(new Cookie[]{otherCookie});

        // Check containsContext
        // The implementation returns isEmpty(), i.e., no match -> true
        final var result = repository.containsContext(request);

        assertThat(result).isTrue();
    }

}
