package io.vanillabp.cockpit.users.directory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import io.vanillabp.cockpit.support.security.usercontext.UserDetails;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The LDAP directory against a real LDAP server. The server is LLDAP, a small LDAP server which
 * keeps its users below {@code ou=people} and its groups below {@code ou=groups}. Its groups name
 * their members by DN in the attribute {@code member}, which is what the defaults of the directory
 * expect. The users and groups are created through the GraphQL API of LLDAP before the tests.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class LdapUserDirectoryServiceTest {

  /**
   * The image of LLDAP. A recent release, written here because the container images of
   * {@code test-utils} have no LDAP server.
   */
  private static final String LLDAP_IMAGE = "lldap/lldap:2026-09-22";

  private static final String BASE = "dc=example,dc=com";

  private static final String ADMIN_PASSWORD = "admin-password";

  private static final Duration CACHE_DURATION = Duration.ofMinutes(5);

  private static final GenericContainer<?> LLDAP = new GenericContainer<>(LLDAP_IMAGE)
      .withEnv("LLDAP_JWT_SECRET", "a-secret-only-for-this-test-of-the-cockpit")
      .withEnv("LLDAP_KEY_SEED", "a-seed-only-for-this-test-of-the-cockpit")
      .withEnv("LLDAP_LDAP_BASE_DN", BASE)
      .withEnv("LLDAP_LDAP_USER_PASS", ADMIN_PASSWORD)
      .withExposedPorts(3890, 17170)
      .waitingFor(Wait
          .forHttp("/health")
          .forPort(17170)
          .forStatusCode(200)
          .withStartupTimeout(Duration.ofMinutes(2)));

  private static final Map<String, Long> GROUP_IDS = new HashMap<>();

  private final MovableClock clock = new MovableClock(Instant.now(), ZoneOffset.UTC);

  @BeforeAll
  static void startTheServerWithUsersAndGroups() {

    LLDAP.start();

    createUser("jane", "jane.doe@example.com", "Jane", "Doe");
    createUser("john", "john.doe@example.com", "John", "Doe");
    createGroup("approvers");
    createGroup("clerks");
    addToGroup("jane", "approvers");
    addToGroup("jane", "clerks");
    addToGroup("john", "clerks");

  }

  @AfterAll
  static void stopTheServer() {

    LLDAP.stop();

  }

  private LdapUserDirectoryService directory(
      final String bindPassword) {

    return new LdapUserDirectoryService(
        new LdapUserDirectoryProperties(
            "ldap://"
                + LLDAP.getHost()
                + ":"
                + LLDAP.getMappedPort(3890), BASE, "uid=admin,ou=people,"
                    + BASE, bindPassword, "ou=people", "inetOrgPerson", "uid", "mail", "givenName", "sn", "cn", "ou=groups", "member", "cn"), CACHE_DURATION, clock);

  }

  private LdapUserDirectoryService directory() {

    return directory(ADMIN_PASSWORD);

  }

  @Test
  void aUserComesWithItsNameAndItsGroups() {

    final var jane = directory().getUser("jane");

    assertThat(jane).isPresent();
    assertThat(jane.get().getId()).isEqualTo("jane");
    assertThat(jane.get().getEmail()).isEqualTo("jane.doe@example.com");
    assertThat(jane.get().getDisplay()).isEqualTo("Doe, Jane");
    assertThat(jane.get().getDisplayShort()).isEqualTo("Doe, J.");
    assertThat(jane.get().getAuthorities()).containsExactlyInAnyOrder("approvers", "clerks");

  }

  @Test
  void aUserTheDirectoryDoesNotKnowIsEmpty() {

    assertThat(directory().getUser("nobody")).isEmpty();

  }

  /**
   * The id comes from the login and goes into a search filter. A character with a meaning in a
   * filter must not change the search.
   */
  @Test
  void anIdWithCharactersOfAFilterFindsNobody() {

    assertThat(directory().getUser("*")).isEmpty();
    assertThat(directory().getUser("jane)(uid=*")).isEmpty();

  }

  @Test
  void aGroupTakenAwayIsGoneOnceTheCacheExpires() {

    final var directory = directory();
    assertThat(directory.getUser("john").orElseThrow().getAuthorities()).containsExactly("clerks");

    removeFromGroup("john", "clerks");
    try {
      clock.moveBy(CACHE_DURATION.minusSeconds(1));
      assertThat(directory.getUser("john").orElseThrow().getAuthorities()).containsExactly("clerks");

      clock.moveBy(Duration.ofSeconds(1));
      assertThat(directory.getUser("john").orElseThrow().getAuthorities()).isEmpty();
    } finally {
      addToGroup("john", "clerks");
    }

  }

  @Test
  void aSearchFindsTheUsersWithoutTheirGroups() {

    final var found = directory().findUsers("doe");

    assertThat(found)
        .extracting(UserDetails::getId)
        .containsExactlyInAnyOrder("jane", "john");
    assertThat(found)
        .allSatisfy(user -> assertThat(user.getAuthorities()).isEmpty());
    assertThat(directory().findUsers("jane*"))
        .extracting(UserDetails::getId)
        .containsExactly("jane");

  }

  @Test
  void allUsersAreListedAndSomeCanBeLeftOut() {

    final var directory = directory();

    assertThat(directory.getAllUsers())
        .extracting(UserDetails::getId)
        .contains("jane", "john");
    assertThat(directory.getAllUsers(List.of("jane")))
        .extracting(UserDetails::getId)
        .contains("john")
        .doesNotContain("jane");
    assertThat(directory.findUsers("doe", List.of("john")))
        .extracting(UserDetails::getId)
        .containsExactly("jane");

  }

  @Test
  void aWrongPasswordFailsAndSaysWhatToCheck() {

    assertThatThrownBy(() -> directory("wrong-password").getUser("jane"))
        .isInstanceOf(UserDirectoryFailedException.class)
        .hasMessageContaining("bind-password");

  }

  private static RestClient graphQl() {

    final var api = RestClient.create("http://"
        + LLDAP.getHost()
        + ":"
        + LLDAP.getMappedPort(17170));
    final var login = api
        .post()
        .uri("/auth/simple/login")
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("username", "admin", "password", ADMIN_PASSWORD))
        .retrieve()
        .body(new ParameterizedTypeReference<Map<String, Object>>() {
        });
    return api
        .mutate()
        .defaultHeaders(headers -> headers.setBearerAuth((String) login.get("token")))
        .build();

  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> mutation(
      final String mutation) {

    final var answer = graphQl()
        .post()
        .uri("/api/graphql")
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("query", "mutation { "
            + mutation
            + " }"))
        .retrieve()
        .body(new ParameterizedTypeReference<Map<String, Object>>() {
        });
    assertThat(answer).doesNotContainKey("errors");
    return (Map<String, Object>) answer.get("data");

  }

  private static void createUser(
      final String id,
      final String email,
      final String firstName,
      final String lastName) {

    mutation(
        ("createUser(user: { id: \"%s\", email: \"%s\", firstName: \"%s\", lastName: \"%s\", displayName: \"%s %s\" }) "
            + "{ id }").formatted(id, email, firstName, lastName, firstName, lastName));

  }

  @SuppressWarnings("unchecked")
  private static void createGroup(
      final String name) {

    final var data = mutation("createGroup(name: \"%s\") { id }".formatted(name));
    GROUP_IDS.put(name, ((Number) ((Map<String, Object>) data.get("createGroup")).get("id")).longValue());

  }

  private static void addToGroup(
      final String userId,
      final String group) {

    mutation("addUserToGroup(userId: \"%s\", groupId: %d) { ok }".formatted(userId, GROUP_IDS.get(group)));

  }

  private static void removeFromGroup(
      final String userId,
      final String group) {

    mutation("removeUserFromGroup(userId: \"%s\", groupId: %d) { ok }".formatted(userId, GROUP_IDS.get(group)));

  }

}
