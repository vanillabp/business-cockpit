package io.vanillabp.cockpit.support.security.usercontext;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The plain holder of a user returns what it was built with, and the questions about authorities
 * which {@link UserDetails} answers for every implementation read those authorities.
 */
@ExtendWith(SuppressOutputExtension.class)
public class UserDetailsImplTest {

  private final UserDetails jane = new UserDetailsImpl(
      "jane", "jane@example.com", "Doe, Jane", "JD", List.of("clerks", "USER_jane"));

  @Test
  public void theDetailsAreTheOnesItWasBuiltWith() {

    assertThat(jane.getId()).isEqualTo("jane");
    assertThat(jane.getEmail()).isEqualTo("jane@example.com");
    assertThat(jane.getDisplay()).isEqualTo("Doe, Jane");
    assertThat(jane.getDisplayShort()).isEqualTo("JD");
    assertThat(jane.getAuthorities()).containsExactly("clerks", "USER_jane");

  }

  @Test
  public void oneAuthorityIsFoundAmongTheAuthorities() {

    assertThat(jane.hasAuthority("clerks")).isTrue();
    assertThat(jane.hasAuthority("managers")).isFalse();

  }

  @Test
  public void oneOfSeveralAuthoritiesIsEnough() {

    assertThat(jane.hasOneAuthorityOf(List.of("managers", "clerks"))).isTrue();
    assertThat(jane.hasOneAuthorityOf(List.of("managers", "auditors"))).isFalse();

  }

  @Test
  public void allAuthoritiesHaveToBeThere() {

    assertThat(jane.hasAllAuthoritiesOf(List.of("clerks", "USER_jane"))).isTrue();
    assertThat(jane.hasAllAuthoritiesOf(List.of("clerks", "managers"))).isFalse();

  }

}
