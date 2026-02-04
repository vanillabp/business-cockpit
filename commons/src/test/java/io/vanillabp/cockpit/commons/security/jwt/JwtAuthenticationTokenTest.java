package io.vanillabp.cockpit.commons.security.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationTokenTest {

    private Jwt createTestJwt(String subject, String tokenValue) {
        return new Jwt(
                tokenValue,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of("sub", subject)
        );
    }

    @Test
    void extendsAbstractAuthenticationToken() {
        Jwt jwt = createTestJwt("user@example.com", "token123");
        JwtAuthenticationToken token = new JwtAuthenticationToken(jwt, List.of());

        assertThat(token).isInstanceOf(AbstractAuthenticationToken.class);
    }

    @Test
    void constructor_setsJwtAsDetails() {
        Jwt jwt = createTestJwt("user@example.com", "token123");
        JwtAuthenticationToken token = new JwtAuthenticationToken(jwt, List.of());

        assertThat(token.getDetails()).isSameAs(jwt);
    }

    @Test
    void constructor_setsAuthenticatedToTrue() {
        Jwt jwt = createTestJwt("user@example.com", "token123");
        JwtAuthenticationToken token = new JwtAuthenticationToken(jwt, List.of());

        assertThat(token.isAuthenticated()).isTrue();
    }

    @Test
    void constructor_setsAuthorities() {
        Jwt jwt = createTestJwt("user@example.com", "token123");
        Collection<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_ADMIN")
        );

        JwtAuthenticationToken token = new JwtAuthenticationToken(jwt, authorities);

        assertThat(token.getAuthorities()).hasSize(2);
        assertThat(token.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void getPrincipal_returnsSubject() {
        Jwt jwt = createTestJwt("user@example.com", "token123");
        JwtAuthenticationToken token = new JwtAuthenticationToken(jwt, List.of());

        assertThat(token.getPrincipal()).isEqualTo("user@example.com");
    }

    @Test
    void getCredentials_returnsTokenValue() {
        Jwt jwt = createTestJwt("user@example.com", "my-secret-token");
        JwtAuthenticationToken token = new JwtAuthenticationToken(jwt, List.of());

        assertThat(token.getCredentials()).isEqualTo("my-secret-token");
    }

    @Test
    void getJwt_returnsJwt() {
        Jwt jwt = createTestJwt("user@example.com", "token123");
        JwtAuthenticationToken token = new JwtAuthenticationToken(jwt, List.of());

        assertThat(token.getJwt()).isSameAs(jwt);
    }

    @Test
    void getName_returnsPrincipal() {
        Jwt jwt = createTestJwt("user@example.com", "token123");
        JwtAuthenticationToken token = new JwtAuthenticationToken(jwt, List.of());

        assertThat(token.getName()).isEqualTo("user@example.com");
    }

    @Test
    void withEmptyAuthorities_hasNoAuthorities() {
        Jwt jwt = createTestJwt("user@example.com", "token123");
        JwtAuthenticationToken token = new JwtAuthenticationToken(jwt, List.of());

        assertThat(token.getAuthorities()).isEmpty();
    }
}
