package io.vanillabp.cockpit.users.directory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import io.vanillabp.cockpit.support.security.usercontext.UserDetails;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The Keycloak directory against a real Keycloak, which imports the realm of
 * {@code users/directory/keycloak-realm.json} at its start. The realm has the users jane (approvers
 * and clerks), john (clerks) and former (disabled), the client of the directory and a client whose
 * service account has no rights.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class KeycloakUserDirectoryServiceTest {

  /**
   * The image of Keycloak. A recent release, written here because the container images of
   * {@code test-utils} have none for Keycloak.
   */
  private static final String KEYCLOAK_IMAGE = "quay.io/keycloak/keycloak:26.8.2";

  private static final String REALM = "business-cockpit";

  private static final String ADMIN = "admin";

  private static final Duration CACHE_DURATION = Duration.ofMinutes(5);

  private static final GenericContainer<?> KEYCLOAK = new GenericContainer<>(KEYCLOAK_IMAGE)
      .withCommand("start-dev", "--import-realm")
      .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", ADMIN)
      .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", ADMIN)
      .withCopyFileToContainer(
          MountableFile.forClasspathResource("users/directory/keycloak-realm.json"),
          "/opt/keycloak/data/import/business-cockpit-realm.json")
      .withExposedPorts(8080)
      .waitingFor(Wait
          .forHttp("/realms/"
              + REALM)
          .forPort(8080)
          .forStatusCode(200)
          .withStartupTimeout(Duration.ofMinutes(3)));

  private final MovableClock clock = new MovableClock(Instant.now(), ZoneOffset.UTC);

  @BeforeAll
  static void startKeycloak() {

    KEYCLOAK.start();

  }

  @AfterAll
  static void stopKeycloak() {

    KEYCLOAK.stop();

  }

  private static String serverUrl() {

    return "http://"
        + KEYCLOAK.getHost()
        + ":"
        + KEYCLOAK.getMappedPort(8080);

  }

  private KeycloakUserDirectoryService directory(
      final KeycloakUserId userId) {

    return new KeycloakUserDirectoryService(
        new KeycloakUserDirectoryProperties(
            serverUrl(), REALM, "business-cockpit-directory", "directory-secret", userId), CACHE_DURATION, clock);

  }

  @Test
  void aUserComesWithItsNameAndTheGroupsOfKeycloak() {

    final var jane = directory(KeycloakUserId.USERNAME).getUser("jane");

    assertThat(jane).isPresent();
    assertThat(jane.get().getId()).isEqualTo("jane");
    assertThat(jane.get().getEmail()).isEqualTo("jane.doe@example.com");
    assertThat(jane.get().getDisplay()).isEqualTo("Doe, Jane");
    assertThat(jane.get().getDisplayShort()).isEqualTo("Doe, J.");
    assertThat(jane.get().getAuthorities()).containsExactlyInAnyOrder("approvers", "clerks");

  }

  @Test
  void aUserKeycloakDoesNotKnowIsEmpty() {

    assertThat(directory(KeycloakUserId.USERNAME).getUser("nobody")).isEmpty();

  }

  @Test
  void aDisabledUserCountsAsUnknown() {

    assertThat(directory(KeycloakUserId.USERNAME).getUser("former")).isEmpty();

  }

  /**
   * Spring Security names a user by the claim {@code sub} if nothing else is configured, and that
   * is the internal id of Keycloak.
   */
  @Test
  void aUserIsFoundByTheIdOfKeycloakIfTheLoginNamesItSo() {

    final var keycloakId = keycloakIdOf("jane");

    final var jane = directory(KeycloakUserId.ID).getUser(keycloakId);

    assertThat(jane).isPresent();
    assertThat(jane.get().getId()).isEqualTo(keycloakId);
    assertThat(jane.get().getAuthorities()).containsExactlyInAnyOrder("approvers", "clerks");
    assertThat(directory(KeycloakUserId.ID).getUser("00000000-0000-0000-0000-000000000000")).isEmpty();

  }

  @Test
  void aGroupTakenAwayIsGoneOnceTheCacheExpires() {

    final var directory = directory(KeycloakUserId.USERNAME);
    assertThat(directory.getUser("john").orElseThrow().getAuthorities()).containsExactly("clerks");

    final var johnsId = keycloakIdOf("john");
    final var clerksId = groupIdOf("clerks");
    admin()
        .delete()
        .uri("/admin/realms/{realm}/users/{user}/groups/{group}", REALM, johnsId, clerksId)
        .retrieve()
        .toBodilessEntity();
    try {
      clock.moveBy(CACHE_DURATION.minusSeconds(1));
      assertThat(directory.getUser("john").orElseThrow().getAuthorities()).containsExactly("clerks");

      clock.moveBy(Duration.ofSeconds(1));
      assertThat(directory.getUser("john").orElseThrow().getAuthorities()).isEmpty();
    } finally {
      admin()
          .put()
          .uri("/admin/realms/{realm}/users/{user}/groups/{group}", REALM, johnsId, clerksId)
          .retrieve()
          .toBodilessEntity();
    }

  }

  @Test
  void aSearchFindsTheEnabledUsersWithoutTheirGroups() {

    final var found = directory(KeycloakUserId.USERNAME).findUsers("doe");

    assertThat(found)
        .extracting(UserDetails::getId)
        .containsExactlyInAnyOrder("jane", "john");
    assertThat(found)
        .allSatisfy(user -> assertThat(user.getAuthorities()).isEmpty());

  }

  @Test
  void allUsersAreTheEnabledUsersAndSomeCanBeLeftOut() {

    final var directory = directory(KeycloakUserId.USERNAME);

    assertThat(directory.getAllUsers())
        .extracting(UserDetails::getId)
        .containsExactlyInAnyOrder("jane", "john");
    assertThat(directory.getAllUsers(List.of("jane")))
        .extracting(UserDetails::getId)
        .containsExactly("john");
    assertThat(directory.findUsers("doe", List.of("john")))
        .extracting(UserDetails::getId)
        .containsExactly("jane");

  }

  @Test
  void aWrongSecretFailsAndSaysWhatToCheck() {

    final var directory = new KeycloakUserDirectoryService(
        new KeycloakUserDirectoryProperties(
            serverUrl(), REALM, "business-cockpit-directory", "wrong", KeycloakUserId.USERNAME), CACHE_DURATION, clock);

    assertThatThrownBy(() -> directory.getUser("jane"))
        .isInstanceOf(UserDirectoryFailedException.class)
        .hasMessageContaining("client-secret")
        .hasMessageContaining("'business-cockpit-directory'");

  }

  @Test
  void aClientWithoutTheRoleFailsAndNamesTheRole() {

    final var directory = new KeycloakUserDirectoryService(
        new KeycloakUserDirectoryProperties(
            serverUrl(), REALM, "without-rights", "without-rights-secret", KeycloakUserId.USERNAME), CACHE_DURATION, clock);

    assertThatThrownBy(() -> directory.getUser("jane"))
        .isInstanceOf(UserDirectoryFailedException.class)
        .hasMessageContaining("'view-users'")
        .hasMessageContaining("'realm-management'");

  }

  private static RestClient admin() {

    final var form = new LinkedMultiValueMap<String, String>();
    form.add("grant_type", "password");
    form.add("client_id", "admin-cli");
    form.add("username", ADMIN);
    form.add("password", ADMIN);
    final var token = RestClient
        .create(serverUrl())
        .post()
        .uri("/realms/master/protocol/openid-connect/token")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(form)
        .retrieve()
        .body(new ParameterizedTypeReference<Map<String, Object>>() {
        });
    return RestClient
        .builder()
        .baseUrl(serverUrl())
        .defaultHeaders(headers -> headers.setBearerAuth((String) token.get("access_token")))
        .build();

  }

  private static String keycloakIdOf(
      final String username) {

    return (String) admin()
        .get()
        .uri("/admin/realms/{realm}/users?username={username}&exact=true", REALM, username)
        .retrieve()
        .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {
        })
        .getFirst()
        .get("id");

  }

  private static String groupIdOf(
      final String name) {

    return (String) admin()
        .get()
        .uri("/admin/realms/{realm}/groups?search={name}&exact=true", REALM, name)
        .retrieve()
        .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {
        })
        .getFirst()
        .get("id");

  }

}
