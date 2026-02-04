package io.vanillabp.cockpit.commons.security.jwt;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUserDetailsTest {

    private JwtAuthenticationToken createAuthToken(Map<String, Object> claims, String... authorities) {
        Jwt jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                claims
        );
        return new JwtAuthenticationToken(jwt,
                List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList());
    }

    @Test
    void implementsUserDetails() {
        JwtAuthenticationToken token = createAuthToken(Map.of("sub", "user@example.com"));
        JwtUserDetails userDetails = new JwtUserDetails(token);

        assertThat(userDetails).isInstanceOf(UserDetails.class);
    }

    @Test
    void userAuthorityPrefix_hasExpectedValue() {
        assertThat(JwtUserDetails.USER_AUTHORITY_PREFIX).isEqualTo("USER_");
    }

    @Test
    void getId_returnsSubject() {
        JwtAuthenticationToken token = createAuthToken(Map.of("sub", "user-123"));
        JwtUserDetails userDetails = new JwtUserDetails(token);

        assertThat(userDetails.getId()).isEqualTo("user-123");
    }

    @Test
    void getEmail_returnsSubject() {
        JwtAuthenticationToken token = createAuthToken(Map.of("sub", "user@example.com"));
        JwtUserDetails userDetails = new JwtUserDetails(token);

        assertThat(userDetails.getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void getDisplay_withFamilyAndGivenName_returnsCombined() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user@example.com");
        claims.put("given_name", "John");
        claims.put("family_name", "Doe");
        JwtAuthenticationToken token = createAuthToken(claims);
        JwtUserDetails userDetails = new JwtUserDetails(token);

        assertThat(userDetails.getDisplay()).isEqualTo("Doe, John");
    }

    @Test
    void getDisplay_withOnlyFamilyName_returnsFamilyName() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user@example.com");
        claims.put("family_name", "Doe");
        JwtAuthenticationToken token = createAuthToken(claims);
        JwtUserDetails userDetails = new JwtUserDetails(token);

        assertThat(userDetails.getDisplay()).isEqualTo("Doe");
    }

    @Test
    void getDisplay_withOnlyName_returnsName() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user@example.com");
        claims.put("name", "John Doe");
        JwtAuthenticationToken token = createAuthToken(claims);
        JwtUserDetails userDetails = new JwtUserDetails(token);

        assertThat(userDetails.getDisplay()).isEqualTo("John Doe");
    }

    @Test
    void getDisplay_withNoNames_returnsNull() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user@example.com");
        JwtAuthenticationToken token = createAuthToken(claims);
        JwtUserDetails userDetails = new JwtUserDetails(token);

        assertThat(userDetails.getDisplay()).isNull();
    }

    @Test
    void getDisplayShort_withFamilyAndGivenName_returnsCombined() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user@example.com");
        claims.put("given_name", "John");
        claims.put("family_name", "Doe");
        JwtAuthenticationToken token = createAuthToken(claims);
        JwtUserDetails userDetails = new JwtUserDetails(token);

        assertThat(userDetails.getDisplayShort()).isEqualTo("Doe, John");
    }

    @Test
    void getDisplayShort_withOnlyFamilyName_returnsFamilyName() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user@example.com");
        claims.put("family_name", "Doe");
        JwtAuthenticationToken token = createAuthToken(claims);
        JwtUserDetails userDetails = new JwtUserDetails(token);

        assertThat(userDetails.getDisplayShort()).isEqualTo("Doe");
    }

    @Test
    void getDisplayShort_withNameWithSpaces_parsesFamilyAndGiven() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user@example.com");
        claims.put("name", "John Doe");
        JwtAuthenticationToken token = createAuthToken(claims);
        JwtUserDetails userDetails = new JwtUserDetails(token);

        assertThat(userDetails.getDisplayShort()).isEqualTo("John, Doe");
    }

    @Test
    void getDisplayShort_withSingleWordName_returnsName() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "user@example.com");
        claims.put("name", "Admin");
        JwtAuthenticationToken token = createAuthToken(claims);
        JwtUserDetails userDetails = new JwtUserDetails(token);

        assertThat(userDetails.getDisplayShort()).isEqualTo("Admin");
    }

    @Test
    void getAuthorities_returnsAuthoritiesFromToken() {
        JwtAuthenticationToken token = createAuthToken(
                Map.of("sub", "user@example.com"),
                "ROLE_USER", "ROLE_ADMIN");
        JwtUserDetails userDetails = new JwtUserDetails(token);

        assertThat(userDetails.getAuthorities())
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void getAuthorities_withNoAuthorities_returnsEmptyList() {
        JwtAuthenticationToken token = createAuthToken(Map.of("sub", "user@example.com"));
        JwtUserDetails userDetails = new JwtUserDetails(token);

        assertThat(userDetails.getAuthorities()).isEmpty();
    }
}
