package io.vanillabp.cockpit.devshell.simulator.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.vanillabp.cockpit.devshell.simulator.usermanagement.User;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;

/**
 * What the simulator's users log in with. The password is encoded in the configuration class rather
 * than by a helper of Spring Security's samples, and a login only works while the encoding matches
 * what Spring Security checks it against.
 */
@ExtendWith(SuppressOutputExtension.class)
class SecurityConfigTest {

    @Test
    void aSimulatedUserLogsInWithTheEncodedPassword() {

        final var userDetails =
                new SecurityConfig()
                        .inMemoryUserDetailsService(simulatedUsers())
                        .loadUserByUsername("john");

        assertThat(userDetails.getPassword())
                .as("the plain password must not end up in the user details")
                .isNotEqualTo(SecurityConfig.SIMULATED_USER_PASSWORD)
                .as("a delegating encoder names itself in front of the hash")
                .startsWith("{");
        assertThat(
                        PasswordEncoderFactories
                                .createDelegatingPasswordEncoder()
                                .matches(SecurityConfig.SIMULATED_USER_PASSWORD, userDetails.getPassword()))
                .as("Spring Security checks a login with its own default encoder")
                .isTrue();

    }

    @Test
    void theGroupsOfAUserBecomeItsAuthorities() {

        final var userDetails =
                new SecurityConfig()
                        .inMemoryUserDetailsService(simulatedUsers())
                        .loadUserByUsername("jane");

        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ADMIN", "RISK_ASSESSMENT");

    }

    private Properties simulatedUsers() {

        return new Properties(
                List.of(
                        User.builder().id("john").groups(List.of("ADMIN")).build(),
                        User.builder().id("jane").groups(List.of("ADMIN", "RISK_ASSESSMENT")).build()));

    }

}
