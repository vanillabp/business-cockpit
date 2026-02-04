package io.vanillabp.cockpit.commons.security.usercontext;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserDetailsTest {

    private UserDetails createUserDetails(List<String> authorities) {
        return new UserDetails() {
            @Override
            public String getId() {
                return "user-123";
            }

            @Override
            public String getEmail() {
                return "user@example.com";
            }

            @Override
            public String getDisplay() {
                return "John Doe";
            }

            @Override
            public String getDisplayShort() {
                return "Doe, John";
            }

            @Override
            public List<String> getAuthorities() {
                return authorities;
            }
        };
    }

    @Test
    void hasAuthority_withMatchingAuthority_returnsTrue() {
        UserDetails user = createUserDetails(List.of("ROLE_USER", "ROLE_ADMIN"));

        assertThat(user.hasAuthority("ROLE_USER")).isTrue();
        assertThat(user.hasAuthority("ROLE_ADMIN")).isTrue();
    }

    @Test
    void hasAuthority_withNonMatchingAuthority_returnsFalse() {
        UserDetails user = createUserDetails(List.of("ROLE_USER"));

        assertThat(user.hasAuthority("ROLE_ADMIN")).isFalse();
    }

    @Test
    void hasAuthority_withEmptyAuthorities_returnsFalse() {
        UserDetails user = createUserDetails(List.of());

        assertThat(user.hasAuthority("ROLE_USER")).isFalse();
    }

    @Test
    void hasAuthority_withNull_returnsFalse() {
        UserDetails user = createUserDetails(List.of("ROLE_USER"));

        assertThat(user.hasAuthority(null)).isFalse();
    }

    @Test
    void hasOneAuthorityOf_withMatchingAuthority_returnsTrue() {
        UserDetails user = createUserDetails(List.of("ROLE_USER", "ROLE_VIEWER"));

        assertThat(user.hasOneAuthorityOf(Set.of("ROLE_ADMIN", "ROLE_USER"))).isTrue();
    }

    @Test
    void hasOneAuthorityOf_withNoMatchingAuthority_returnsFalse() {
        UserDetails user = createUserDetails(List.of("ROLE_VIEWER"));

        assertThat(user.hasOneAuthorityOf(Set.of("ROLE_ADMIN", "ROLE_USER"))).isFalse();
    }

    @Test
    void hasOneAuthorityOf_withEmptyUserAuthorities_returnsFalse() {
        UserDetails user = createUserDetails(List.of());

        assertThat(user.hasOneAuthorityOf(Set.of("ROLE_ADMIN", "ROLE_USER"))).isFalse();
    }

    @Test
    void hasOneAuthorityOf_withEmptyRequiredAuthorities_returnsFalse() {
        UserDetails user = createUserDetails(List.of("ROLE_USER"));

        assertThat(user.hasOneAuthorityOf(Set.of())).isFalse();
    }

    @Test
    void hasAllAuthoritiesOf_withAllMatchingAuthorities_returnsTrue() {
        UserDetails user = createUserDetails(List.of("ROLE_USER", "ROLE_ADMIN", "ROLE_VIEWER"));

        assertThat(user.hasAllAuthoritiesOf(Set.of("ROLE_USER", "ROLE_ADMIN"))).isTrue();
    }

    @Test
    void hasAllAuthoritiesOf_withPartialMatchingAuthorities_returnsFalse() {
        UserDetails user = createUserDetails(List.of("ROLE_USER"));

        assertThat(user.hasAllAuthoritiesOf(Set.of("ROLE_USER", "ROLE_ADMIN"))).isFalse();
    }

    @Test
    void hasAllAuthoritiesOf_withNoMatchingAuthorities_returnsFalse() {
        UserDetails user = createUserDetails(List.of("ROLE_VIEWER"));

        assertThat(user.hasAllAuthoritiesOf(Set.of("ROLE_USER", "ROLE_ADMIN"))).isFalse();
    }

    @Test
    void hasAllAuthoritiesOf_withEmptyUserAuthorities_returnsFalse() {
        UserDetails user = createUserDetails(List.of());

        assertThat(user.hasAllAuthoritiesOf(Set.of("ROLE_USER"))).isFalse();
    }

    @Test
    void hasAllAuthoritiesOf_withEmptyRequiredAuthorities_returnsTrue() {
        UserDetails user = createUserDetails(List.of("ROLE_USER"));

        assertThat(user.hasAllAuthoritiesOf(Set.of())).isTrue();
    }

    @Test
    void interfaceMethods_returnExpectedValues() {
        UserDetails user = createUserDetails(List.of("ROLE_USER"));

        assertThat(user.getId()).isEqualTo("user-123");
        assertThat(user.getEmail()).isEqualTo("user@example.com");
        assertThat(user.getDisplay()).isEqualTo("John Doe");
        assertThat(user.getDisplayShort()).isEqualTo("Doe, John");
        assertThat(user.getAuthorities()).containsExactly("ROLE_USER");
    }
}
