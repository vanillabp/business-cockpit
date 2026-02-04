package io.vanillabp.cockpit.commons.security.jwt;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.commons.security.usercontext.UserDetailsProvider;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUserDetailsProviderTest {

    private final JwtUserDetailsProvider provider = new JwtUserDetailsProvider();

    private JwtAuthenticationToken createJwtToken(String subject) {
        Jwt jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of("sub", subject)
        );
        return new JwtAuthenticationToken(jwt, List.of());
    }

    @Test
    void implementsUserDetailsProvider() {
        assertThat(provider).isInstanceOf(UserDetailsProvider.class);
    }

    @Test
    void getUserDetails_withNull_returnsNull() {
        UserDetails result = provider.getUserDetails(null);

        assertThat(result).isNull();
    }

    @Test
    void getUserDetails_withJwtAuthenticationToken_returnsJwtUserDetails() {
        JwtAuthenticationToken token = createJwtToken("user@example.com");

        UserDetails result = provider.getUserDetails(token);

        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(JwtUserDetails.class);
        assertThat(result.getId()).isEqualTo("user@example.com");
    }

    @Test
    void getUserDetails_withNonJwtAuthentication_returnsNull() {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("user", "password");

        UserDetails result = provider.getUserDetails(token);

        assertThat(result).isNull();
    }

    @Test
    void getUserDetails_withJwtToken_returnsCorrectEmail() {
        JwtAuthenticationToken token = createJwtToken("test@domain.com");

        UserDetails result = provider.getUserDetails(token);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@domain.com");
    }
}
