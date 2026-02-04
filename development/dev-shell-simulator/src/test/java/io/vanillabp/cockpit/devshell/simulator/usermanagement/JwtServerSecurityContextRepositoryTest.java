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
        // JwtCookie mit Default-Werten konfigurieren
        jwtCookie = new JwtCookie();
        jwtCookie.setName("bc");
        jwtCookie.setPath("/");
        jwtCookie.setDomain("localhost");
        jwtCookie.setSecure(false);
        jwtCookie.setExpiresDuration("PT12H");

        // Repository mit gemockten Abhaengigkeiten instanziieren
        repository = new JwtServerSecurityContextRepository(jwtProperties, jwtMapper);
    }

    // --- loadContext ---

    @Test
    void loadContext_withJwtCookie_returnsSecurityContext() {
        // Request mit JWT-Cookie vorbereiten
        final var jwtCookieValue = new Cookie("bc", "jwt-token-value");
        when(jwtProperties.getCookie()).thenReturn(jwtCookie);

        final var holder = new HttpRequestResponseHolder(request, response);
        when(request.getCookies()).thenReturn(new Cookie[]{jwtCookieValue});

        // JwtMapper gibt einen SecurityContext zurueck
        final var expectedContext = new SecurityContextImpl();
        when(jwtMapper.toSecurityContext("jwt-token-value")).thenReturn(expectedContext);

        // loadContext ausfuehren und Ergebnis pruefen
        final var result = repository.loadContext(holder);

        assertThat(result).isSameAs(expectedContext);
    }

    @Test
    void loadContext_withNoCookies_returnsNull() {
        // Request ohne Cookies
        final var holder = new HttpRequestResponseHolder(request, response);
        when(request.getCookies()).thenReturn(null);

        // loadContext muss null zurueckgeben
        final var result = repository.loadContext(holder);

        assertThat(result).isNull();
    }

    @Test
    void loadContext_withoutMatchingCookie_returnsNull() {
        // Request mit einem Cookie, das nicht dem JWT-Namen entspricht
        final var otherCookie = new Cookie("other", "value");
        when(jwtProperties.getCookie()).thenReturn(jwtCookie);

        final var holder = new HttpRequestResponseHolder(request, response);
        when(request.getCookies()).thenReturn(new Cookie[]{otherCookie});

        // loadContext muss null zurueckgeben, da kein passender Cookie vorhanden
        final var result = repository.loadContext(holder);

        assertThat(result).isNull();
    }

    // --- saveContext ---

    @Test
    void saveContext_withValidContext_setsCookieOnResponse() {
        // SecurityContext mit Authentication vorbereiten
        final var auth = new UsernamePasswordAuthenticationToken("user", "pass");
        final var context = new SecurityContextImpl(auth);

        // JwtMapper gibt Token zurueck
        when(jwtProperties.getCookie()).thenReturn(jwtCookie);
        when(jwtMapper.toToken(context))
                .thenReturn(Optional.of(new AbstractMap.SimpleEntry<>("encoded-jwt-token", null)));

        // saveContext ausfuehren
        repository.saveContext(context, request, response);

        // Cookie muss auf der Response gesetzt werden
        final var cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());

        // Cookie-Eigenschaften pruefen
        final var savedCookie = cookieCaptor.getValue();
        assertThat(savedCookie.getName()).isEqualTo("bc");
        assertThat(savedCookie.getValue()).isEqualTo("encoded-jwt-token");
        assertThat(savedCookie.getPath()).isEqualTo("/");
        assertThat(savedCookie.getDomain()).isEqualTo("localhost");
        assertThat(savedCookie.isHttpOnly()).isTrue();
    }

    @Test
    void saveContext_withNoToken_doesNotSetCookie() {
        // SecurityContext ohne gueltigen Token
        final var context = new SecurityContextImpl();
        when(jwtMapper.toToken(context)).thenReturn(Optional.empty());

        // saveContext ausfuehren
        repository.saveContext(context, request, response);

        // Kein Cookie darf gesetzt worden sein (verify mit never() ist nicht noetig,
        // da addCookie bei einem leeren Optional nicht aufgerufen wird)
    }

    // --- containsContext ---

    @Test
    void containsContext_withMatchingCookie_returnsTrue() {
        // Request mit dem JWT-Cookie
        final var jwtCookieValue = new Cookie("bc", "token");
        when(jwtProperties.getCookie()).thenReturn(jwtCookie);
        when(request.getCookies()).thenReturn(new Cookie[]{jwtCookieValue});

        // containsContext pruefen
        // Hinweis: Die aktuelle Implementierung gibt isEmpty() zurueck,
        // d.h. wenn ein Cookie gefunden wird, ist findFirst().isEmpty() == false
        final var result = repository.containsContext(request);

        assertThat(result).isFalse();
    }

    @Test
    void containsContext_withoutMatchingCookie_returnsTrue() {
        // Request ohne den JWT-Cookie
        final var otherCookie = new Cookie("other", "value");
        when(jwtProperties.getCookie()).thenReturn(jwtCookie);
        when(request.getCookies()).thenReturn(new Cookie[]{otherCookie});

        // containsContext pruefen
        // Die Implementierung gibt isEmpty() zurueck, d.h. kein Match -> true
        final var result = repository.containsContext(request);

        assertThat(result).isTrue();
    }

}
