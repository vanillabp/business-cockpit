package io.vanillabp.cockpit.support.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.support.security.usercontext.LoggedInUser;
import io.vanillabp.cockpit.support.security.usercontext.UserDetailsImpl;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The name and the e-mail address of a user come from the directory, and from the claims of the
 * token where the directory has none.
 */
@ExtendWith(SuppressOutputExtension.class)
class TokenUserDetailsTest {

  @Test
  void theDirectoryWins() {

    final var directoryUser = new UserDetailsImpl("john", "john@example.com", "Doe, John", "Doe, J.", List.of());
    final var token = token(Map.of("name", "Name Token", "email", "token@example.com"));

    final var details = new TokenUserDetails(token, new LoggedInUser("john", Optional.of(directoryUser), Set.of(
        "USER_john", "ADMIN")));

    assertThat(details.getId()).isEqualTo("john");
    assertThat(details.getDisplay()).isEqualTo("Doe, John");
    assertThat(details.getDisplayShort()).isEqualTo("Doe, J.");
    assertThat(details.getEmail()).isEqualTo("john@example.com");
    assertThat(details.getAuthorities()).containsExactlyInAnyOrder("USER_john", "ADMIN");

  }

  @Test
  void theFamilyAndTheGivenNameOfTheTokenComeFirst() {

    final var details = fromTheTokenOnly(Map.of(
        "name", "John Doe", "given_name", "John", "family_name", "Doe", "email", "token@example.com"));

    assertThat(details.getDisplay()).isEqualTo("Doe, John");
    assertThat(details.getDisplayShort()).isEqualTo("Doe, John");
    assertThat(details.getEmail()).isEqualTo("token@example.com");

  }

  @Test
  void aFamilyNameAloneIsTheName() {

    final var details = fromTheTokenOnly(Map.of("family_name", "Doe"));

    assertThat(details.getDisplay()).isEqualTo("Doe");
    assertThat(details.getDisplayShort()).isEqualTo("Doe");

  }

  @Test
  void theFullNameIsSplitForTheShortForm() {

    final var details = fromTheTokenOnly(Map.of("name", "John  Doe "));

    assertThat(details.getDisplay()).isEqualTo("John  Doe ");
    assertThat(details.getDisplayShort()).isEqualTo("John, Doe");

  }

  @Test
  void aNameOfOneWordStaysAsItIs() {

    assertThat(fromTheTokenOnly(Map.of("name", "John")).getDisplayShort()).isEqualTo("John");
    assertThat(fromTheTokenOnly(Map.of("name", " John")).getDisplayShort()).isEqualTo(" John");

  }

  @Test
  void withoutAnyNameThereIsNone() {

    final var details = fromTheTokenOnly(Map.of());

    assertThat(details.getDisplay()).isNull();
    assertThat(details.getDisplayShort()).isNull();
    assertThat(details.getEmail()).isNull();

  }

  private static TokenUserDetails fromTheTokenOnly(
      final Map<String, Object> claims) {

    return new TokenUserDetails(token(claims).claims(), Optional.empty(), List.of("USER_john"));

  }

  private static LoginToken token(
      final Map<String, Object> claims) {

    final var withSubject = new HashMap<>(claims);
    withSubject.put("sub", "john");
    return new LoginToken("token", Map.of("alg", "HS256"), withSubject);

  }

}
