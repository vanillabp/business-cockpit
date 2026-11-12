package io.vanillabp.cockpit.devshell.simulator.usermanagement;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.devshell.simulator.config.Properties;
import io.vanillabp.cockpit.support.security.usercontext.UserDetails;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The configured users answer as a user directory. A token of the dev shell carries no groups, so
 * whoever reads one gets the groups from here.
 */
@ExtendWith(SuppressOutputExtension.class)
class SimulatedUserDirectoryServiceTest {

  private final SimulatedUserDirectoryService directory = new SimulatedUserDirectoryService(new Properties(List.of(
      User.builder().id("john").firstName("John").lastName("Doe").email("john@example.com").groups(List.of("ADMIN"))
          .build(),
      User.builder().id("jane").lastName("Roe").build(),
      User.builder().id("bot").build())));

  @Test
  void aUserIsFoundByItsIdWithItsGroups() {

    final var john = directory.getUser("john").orElseThrow();

    assertThat(john.getAuthorities()).containsExactly("ADMIN");
    assertThat(john.getDisplay()).isEqualTo("Doe, John");
    assertThat(john.getDisplayShort()).isEqualTo("Doe, J.");
    assertThat(john.getEmail()).isEqualTo("john@example.com");

  }

  @Test
  void aUserWithoutGroupsHasAnEmptyList() {

    assertThat(directory.getUser("jane").orElseThrow().getAuthorities()).isEmpty();
    assertThat(directory.getUser("jane").orElseThrow().getDisplay()).isEqualTo("Roe");
    assertThat(directory.getUser("bot").orElseThrow().getDisplay()).isEqualTo("bot");

  }

  @Test
  void anUnknownIdGivesNoUser() {

    assertThat(directory.getUser("nobody")).isEmpty();
    assertThat(directory.getUser(null)).isEmpty();

  }

  @Test
  void theListsLeaveOutTheExcludedUsersAndFilterByTheQuery() {

    assertThat(directory.getAllUsers()).extracting(UserDetails::getId).containsExactly("john", "jane", "bot");
    assertThat(directory.getAllUsers(List.of("jane"))).extracting(UserDetails::getId).containsExactly("john", "bot");
    assertThat(directory.findUsers("doe")).extracting(UserDetails::getId).containsExactly("john");
    assertThat(directory.findUsers(null, List.of("john"))).extracting(UserDetails::getId).containsExactly("jane",
        "bot");

  }

}
