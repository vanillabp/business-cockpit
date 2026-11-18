package io.vanillabp.cockpit.extension.transport;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.vanillabp.cockpit.extension.config.RestTransportConfiguration;
import io.vanillabp.integration.spi.PhaseTwoRetryLater;

/**
 * The bearer token every report to the cockpit server carries, fetched from an authorization
 * server with the client-credentials flow.
 * <p>
 * There is no user in this flow. The workflow module is the client, it identifies itself with
 * an id and a secret, and the token it gets back says which module reported. A token is kept
 * until shortly before it expires and is then asked for again. The flow has no refresh token,
 * and asking again is what it is for.
 * <p>
 * The token request goes through whatever stands between the workflow module and the outside:
 * the same proxy, the same truststore and the same timeouts as the reports themselves. An
 * authorization server which is unreachable gives the report back to the outbox instead of
 * ending it. A token which cannot be fetched now is the kind of thing which works in a minute.
 */
public class OAuthTokens {

  /**
   * How long before its expiry a token is replaced. A token which expires while a report is on
   * its way would be refused by the cockpit server, and the report would be repeated for a
   * reason nobody can see in the log.
   */
  static final Duration REPLACED_BEFORE_EXPIRY = Duration.ofSeconds(30);

  /** What an authorization server which does not say how long its token lives is assumed to mean. */
  static final Duration ASSUMED_LIFETIME = Duration.ofMinutes(5);

  private static final ObjectMapper JSON = new ObjectMapper();

  private static final int OK = 200;

  private final RestTransportConfiguration.OAuth oauth;

  private final HttpClient client;

  private volatile String token;

  private volatile Instant replaceAfter = Instant.MIN;

  /**
   * Creates the token source. It fetches the first token when the first request needs one.
   *
   * @param configuration What the application configured about the cockpit server. One section
   *          of it is the client-credentials flow, and with it the connection to the
   *          authorization server
   */
  public OAuthTokens(
      final RestTransportConfiguration configuration) {

    this.oauth = configuration.oauth();
    this.client = RestClientSetup.httpClientOf(oauth).build();

  }

  /**
   * What a report carries as its <code>Authorization</code>: a token which is valid now, fetched
   * where the one held has run out.
   *
   * @return The value of the header
   */
  public String authorization() {

    return "Bearer "
        + current();

  }

  /**
   * @return A token which is valid now, fetched where the one held has run out
   */
  synchronized String current() {

    if ((token != null) && Instant.now().isBefore(replaceAfter)) {
      return token;
    }
    final var fetched = fetch();
    token = fetched.accessToken();
    replaceAfter = Instant.now().plus(fetched.livesFor()).minus(REPLACED_BEFORE_EXPIRY);
    return token;

  }

  /**
   * What the authorization server answered.
   *
   * @param accessToken The token
   * @param livesFor How long it is good for
   */
  record Token(
               String accessToken,
               Duration livesFor) {
  }

  private Token fetch() {

    final var request = HttpRequest
        .newBuilder(URI.create(oauth.tokenUrl()))
        .timeout(RestClientSetup.readTimeoutOf(oauth))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .header("Accept", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(form(), StandardCharsets.UTF_8));
    final var proxyAuthorization = RestClientSetup.proxyAuthorizationOf(oauth.proxy());
    if (proxyAuthorization != null) {
      request.header("Proxy-Authorization", proxyAuthorization);
    }
    if (oauth.clientInAuthorizationHeader()) {
      request.header("Authorization", RestClientSetup.basic(oauth.clientId(), oauth.clientSecret()));
    }
    final HttpResponse<byte[]> answer;
    try {
      answer = client.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
    } catch (final IOException e) {
      throw unreachable(e);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw unreachable(e);
    }
    if (answer.statusCode() != OK) {
      throw new PhaseTwoRetryLater(
          """
              The authorization server at '%s' answered the token request of client '%s' with \
              %d. The report waits and is sent again once a token can be fetched."""
              .formatted(oauth.tokenUrl(), oauth.clientId(), answer.statusCode()), null);
    }
    final JsonNode json;
    try {
      json = JSON.readTree(answer.body());
    } catch (final IOException e) {
      throw unreachable(e);
    }
    final var accessToken = json.path("access_token").asText(null);
    if ((accessToken == null) || accessToken.isBlank()) {
      throw new PhaseTwoRetryLater(
          """
              The authorization server at '%s' answered the token request of client '%s' \
              without an 'access_token'."""
              .formatted(oauth.tokenUrl(), oauth.clientId()), null);
    }
    final var expiresIn = json.path("expires_in").asLong(0);
    return new Token(
        accessToken, expiresIn > 0 ? Duration.ofSeconds(expiresIn) : ASSUMED_LIFETIME);

  }

  private PhaseTwoRetryLater unreachable(
      final Exception cause) {

    final var unreachable = new PhaseTwoRetryLater(
        """
            The authorization server at '%s' could not be reached for a token of client '%s'. \
            The report waits and is sent again."""
            .formatted(oauth.tokenUrl(), oauth.clientId()), null);
    unreachable.initCause(cause);
    return unreachable;

  }

  private String form() {

    final var body = new StringBuilder("grant_type=client_credentials");
    if (!oauth.clientInAuthorizationHeader()) {
      body
          .append("&client_id=")
          .append(URLEncoder.encode(oauth.clientId(), StandardCharsets.UTF_8))
          .append("&client_secret=")
          .append(URLEncoder.encode(oauth.clientSecret(), StandardCharsets.UTF_8));
    }
    return body.toString();

  }

}
