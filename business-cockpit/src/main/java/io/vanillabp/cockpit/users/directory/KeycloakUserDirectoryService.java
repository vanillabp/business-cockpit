package io.vanillabp.cockpit.users.directory;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.vanillabp.cockpit.support.security.usercontext.UserDetails;

/**
 * The users and groups of a Keycloak realm, read through the admin REST API of Keycloak. The cockpit
 * brings it if {@code business-cockpit.user-directory.type} is {@code keycloak}.
 * <p>
 * It talks to Keycloak with Spring's {@link RestClient}, so it needs no client library of
 * Keycloak. It logs in with the client credentials of
 * {@link KeycloakUserDirectoryProperties#clientId()} and keeps the access token until shortly
 * before it expires.
 * <p>
 * The groups of a user are the names of the Keycloak groups the user is a member of. A user who is
 * disabled in Keycloak counts as unknown, so it has no groups and is not proposed.
 */
public class KeycloakUserDirectoryService extends CachingUserDirectoryService {

  /**
   * How many entries one request to Keycloak asks for. The lists are read page by page.
   */
  private static final int PAGE_SIZE = 100;

  /**
   * How long before it expires an access token is replaced, so a token does not expire on its way
   * to Keycloak.
   */
  private static final Duration TOKEN_RENEWAL_MARGIN = Duration.ofSeconds(30);

  private static final ParameterizedTypeReference<List<KeycloakUser>> USERS = new ParameterizedTypeReference<>() {
  };

  private static final ParameterizedTypeReference<List<KeycloakGroup>> GROUPS = new ParameterizedTypeReference<>() {
  };

  private final KeycloakUserDirectoryProperties properties;

  private final RestClient restClient;

  private final Clock clock;

  private volatile AccessToken accessToken;

  @JsonIgnoreProperties(ignoreUnknown = true)
  record KeycloakUser(
                      String id,
                      String username,
                      String email,
                      String firstName,
                      String lastName,
                      Boolean enabled) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record KeycloakGroup(
                       String name) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record TokenResponse(
                       @JsonProperty("access_token") String accessToken,
                       @JsonProperty("expires_in") long expiresIn) {
  }

  private record AccessToken(
                             String value,
                             Instant renewAt) {
  }

  /**
   * Creates the directory.
   *
   * @param properties How to reach Keycloak
   * @param cacheDuration How long a user is kept with its groups
   */
  public KeycloakUserDirectoryService(
      final KeycloakUserDirectoryProperties properties,
      final Duration cacheDuration) {

    this(properties, cacheDuration, Clock.systemUTC());

  }

  KeycloakUserDirectoryService(
      final KeycloakUserDirectoryProperties properties,
      final Duration cacheDuration,
      final Clock clock) {

    super(cacheDuration, clock);
    this.properties = properties;
    this.clock = clock;
    this.restClient = RestClient
        .builder()
        .baseUrl(properties.serverUrl())
        .build();

  }

  @Override
  Optional<UserDetails> loadUser(
      final String id) {

    final var user = switch (properties.userId()) {
      case ID -> userById(id);
      case USERNAME -> userByUsername(id);
    };
    return user
        .filter(KeycloakUserDirectoryService::isEnabled)
        .map(found -> toUserDetails(found, groupsOf(found)));

  }

  private Optional<KeycloakUser> userById(
      final String id) {

    try {
      return Optional.ofNullable(get(
          uri -> uri
              .path("/admin/realms/{realm}/users/{id}")
              .build(properties.realm(), id),
          new ParameterizedTypeReference<KeycloakUser>() {
          }));
    } catch (UserDirectoryFailedException e) {
      if ((e.getCause() instanceof HttpClientErrorException clientError) && (clientError
          .getStatusCode() == HttpStatus.NOT_FOUND)) {
        return Optional.empty();
      }
      throw e;
    }

  }

  private Optional<KeycloakUser> userByUsername(
      final String username) {

    final var users = get(
        uri -> uri
            .path("/admin/realms/{realm}/users")
            .queryParam("username", "{username}")
            .queryParam("exact", true)
            .queryParam("briefRepresentation", true)
            .build(properties.realm(), username),
        USERS);
    return users == null
        ? Optional.empty()
        : users
            .stream()
            .filter(user -> username.equalsIgnoreCase(user.username()))
            .findFirst();

  }

  private List<String> groupsOf(
      final KeycloakUser user) {

    return allPages(
        (
            first,
            max) -> get(
                uri -> uri
                    .path("/admin/realms/{realm}/users/{id}/groups")
                    .queryParam("briefRepresentation", true)
                    .queryParam("first", first)
                    .queryParam("max", max)
                    .build(properties.realm(), user.id()),
                GROUPS))
        .stream()
        .map(KeycloakGroup::name)
        .toList();

  }

  @Override
  List<UserDetails> searchUsers(
      final String query) {

    return allPages(
        (
            first,
            max) -> get(
                uri -> uri
                    .path("/admin/realms/{realm}/users")
                    .queryParam("search", "{query}")
                    .queryParam("briefRepresentation", true)
                    .queryParam("first", first)
                    .queryParam("max", max)
                    .build(properties.realm(), query),
                USERS))
        .stream()
        .filter(KeycloakUserDirectoryService::isEnabled)
        .map(user -> toUserDetails(user, List.of()))
        .toList();

  }

  @Override
  List<UserDetails> loadAllUsers() {

    return allPages(
        (
            first,
            max) -> get(
                uri -> uri
                    .path("/admin/realms/{realm}/users")
                    .queryParam("briefRepresentation", true)
                    .queryParam("first", first)
                    .queryParam("max", max)
                    .build(properties.realm()),
                USERS))
        .stream()
        .filter(KeycloakUserDirectoryService::isEnabled)
        .map(user -> toUserDetails(user, List.of()))
        .toList();

  }

  private interface Page<T> {

    List<T> read(
        int first,
        int max);

  }

  private static <T> List<T> allPages(
      final Page<T> page) {

    final var result = new ArrayList<T>();
    var first = 0;
    while (true) {
      final var entries = page.read(first, PAGE_SIZE);
      if (entries == null) {
        return result;
      }
      result.addAll(entries);
      if (entries.size() < PAGE_SIZE) {
        return result;
      }
      first += PAGE_SIZE;
    }

  }

  private static boolean isEnabled(
      final KeycloakUser user) {

    return !Boolean.FALSE.equals(user.enabled());

  }

  private UserDetails toUserDetails(
      final KeycloakUser user,
      final List<String> groups) {

    final var id = properties.userId() == KeycloakUserId.ID
        ? user.id()
        : user.username();
    return userOf(id, user.email(), user.firstName(), user.lastName(), groups);

  }

  private <T> T get(
      final Function<UriBuilder, URI> uri,
      final ParameterizedTypeReference<T> type) {

    try {
      return restClient
          .get()
          .uri(uri)
          .headers(headers -> headers.setBearerAuth(accessToken()))
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(type);
    } catch (RestClientResponseException e) {
      if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
        // Keycloak may end a token early, for example after a restart. The next request logs in
        // again
        accessToken = null;
      }
      throw new UserDirectoryFailedException(whyAnAdminRequestFailed(e), e);
    } catch (RestClientException e) {
      throw new UserDirectoryFailedException(
          "Keycloak at '%s' cannot be reached. Check 'business-cockpit.user-directory.keycloak.server-url'."
              .formatted(properties.serverUrl()), e);
    }

  }

  private String whyAnAdminRequestFailed(
      final RestClientResponseException e) {

    if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
      return ("Keycloak at '%s' does not accept the token of the client '%s' for its admin REST API. "
          + "Newer releases of Keycloak accept only a token for the audience of the admin API. Add "
          + "the client scope 'kc-admin-api' to the default client scopes of the client.")
          .formatted(properties.serverUrl(), properties.clientId());
    }
    if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
      return ("Keycloak at '%s' does not let the client '%s' read the users of the realm '%s'. "
          + "Give the service account of the client the role 'view-users' of the client "
          + "'realm-management'.").formatted(properties.serverUrl(), properties.clientId(), properties.realm());
    }
    if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
      return "Keycloak at '%s' answered 404. Check whether the realm '%s' exists."
          .formatted(properties.serverUrl(), properties.realm());
    }
    return "Keycloak at '%s' answered %s to a request about the users of the realm '%s'."
        .formatted(properties.serverUrl(), e.getStatusCode(), properties.realm());

  }

  private String accessToken() {

    final var current = accessToken;
    final var now = clock.instant();
    if ((current != null) && now.isBefore(current.renewAt())) {
      return current.value();
    }
    final var renewed = logIn(now);
    accessToken = renewed;
    return renewed.value();

  }

  private AccessToken logIn(
      final Instant now) {

    final var form = new LinkedMultiValueMap<String, String>();
    form.add("grant_type", "client_credentials");
    form.add("client_id", properties.clientId());
    form.add("client_secret", properties.clientSecret());

    final TokenResponse response;
    try {
      response = restClient
          .post()
          .uri("/realms/{realm}/protocol/openid-connect/token", Map.of("realm", properties.realm()))
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .accept(MediaType.APPLICATION_JSON)
          .body(form)
          .retrieve()
          .body(TokenResponse.class);
    } catch (RestClientResponseException e) {
      throw new UserDirectoryFailedException(
          ("Keycloak at '%s' refused the login of the client '%s' in the realm '%s' (%s). Check "
              + "'business-cockpit.user-directory.keycloak.client-id' and '.client-secret', and that "
              + "the client has 'Client authentication' and 'Service accounts roles' switched on.")
              .formatted(properties.serverUrl(), properties.clientId(), properties.realm(), e.getStatusCode()), e);
    } catch (RestClientException e) {
      throw new UserDirectoryFailedException(
          "Keycloak at '%s' cannot be reached. Check 'business-cockpit.user-directory.keycloak.server-url'."
              .formatted(properties.serverUrl()), e);
    }
    if ((response == null) || (response.accessToken() == null)) {
      throw new UserDirectoryFailedException(
          "Keycloak at '%s' answered the login of the client '%s' without an access token."
              .formatted(properties.serverUrl(), properties.clientId()), null);
    }

    final var lifetime = Duration.ofSeconds(response.expiresIn());
    final var margin = lifetime.compareTo(TOKEN_RENEWAL_MARGIN.multipliedBy(2)) > 0
        ? TOKEN_RENEWAL_MARGIN
        : lifetime.dividedBy(2);
    return new AccessToken(response.accessToken(), now.plus(lifetime).minus(margin));

  }

}
