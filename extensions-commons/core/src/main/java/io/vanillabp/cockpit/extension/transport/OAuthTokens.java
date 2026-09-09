package io.vanillabp.cockpit.extension.transport;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import javax.net.ssl.HttpsURLConnection;

import com.fasterxml.jackson.databind.ObjectMapper;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.vanillabp.cockpit.extension.config.RestTransportConfiguration;
import io.vanillabp.integration.spi.PhaseTwoRetryLater;

/**
 * The bearer token every report to the cockpit server carries, fetched from an authorization
 * server with the client-credentials flow.
 * <p>
 * There is no user in this flow. The workflow module is the client, it identifies itself with
 * an id and a secret, and the token it gets back says which module reported. A token is kept
 * until shortly before it expires and then asked for again - the flow has no refresh token, and
 * asking again is what it is for.
 * <p>
 * The token request goes through whatever stands between the workflow module and the outside:
 * the same proxy, the same truststore and the same timeouts as the reports themselves. An
 * authorization server which is unreachable gives the report back to the outbox rather than
 * ending it: a token which cannot be fetched now is the kind of thing which works in a minute.
 */
public class OAuthTokens implements RequestInterceptor {

  /**
   * How long before its expiry a token is replaced. A token which expires while a report is on
   * its way would be refused by the cockpit server, and the report would be repeated for a
   * reason nobody can see in the log.
   */
  static final Duration REPLACED_BEFORE_EXPIRY = Duration.ofSeconds(30);

  /** What an authorization server which does not say how long its token lives is assumed to mean. */
  static final Duration ASSUMED_LIFETIME = Duration.ofMinutes(5);

  private static final ObjectMapper JSON = new ObjectMapper();

  private final RestTransportConfiguration.OAuth oauth;

  private volatile String token;

  private volatile Instant replaceAfter = Instant.MIN;

  /**
   * @param configuration What the application configured about the cockpit server, of which the
   *          client-credentials flow - and with it the connection to the authorization server -
   *          is one section
   */
  public OAuthTokens(
      final RestTransportConfiguration configuration) {

    this.oauth = configuration.oauth();

  }

  @Override
  public void apply(
      final RequestTemplate template) {

    template.header("Authorization", "Bearer "
        + current());

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

    try {
      final var connection = open();
      try {
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection
            .setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("Accept", "application/json");
        final var proxyAuthorization = RestClientSetup.proxyAuthorizationOf(oauth.proxy());
        if (proxyAuthorization != null) {
          connection.setRequestProperty("Proxy-Authorization", proxyAuthorization);
        }
        if (oauth.clientInAuthorizationHeader()) {
          connection
              .setRequestProperty(
                  "Authorization",
                  RestClientSetup.basic(oauth.clientId(), oauth.clientSecret()));
        }
        connection.getOutputStream().write(form().getBytes(StandardCharsets.UTF_8));
        final var status = connection.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
          throw new PhaseTwoRetryLater(
              """
                  The authorization server at '%s' answered the token request of client '%s' with \
                  %d. The report waits and is sent again once a token can be fetched."""
                  .formatted(oauth.tokenUrl(), oauth.clientId(), status), null);
        }
        try (InputStream answer = connection.getInputStream()) {
          final var json = JSON.readTree(answer.readAllBytes());
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
      } finally {
        connection.disconnect();
      }
    } catch (final IOException e) {
      final var unreachable = new PhaseTwoRetryLater(
          """
              The authorization server at '%s' could not be reached for a token of client '%s'. \
              The report waits and is sent again."""
              .formatted(oauth.tokenUrl(), oauth.clientId()), null);
      unreachable.initCause(e);
      throw unreachable;
    }

  }

  private HttpURLConnection open() throws IOException {

    final var url = URI.create(oauth.tokenUrl()).toURL();
    final var proxy = oauth.proxy();
    final var connection = (HttpURLConnection) (proxy == null
        ? url.openConnection()
        : url
            .openConnection(
                new Proxy(
                    Proxy.Type.HTTP, new InetSocketAddress(proxy.host(), proxy.port()))));
    final var timeouts = RestClientSetup.timeoutsOf(oauth);
    connection.setConnectTimeout(timeouts.connectTimeoutMillis());
    connection.setReadTimeout(timeouts.readTimeoutMillis());
    if (connection instanceof final HttpsURLConnection https) {
      final var context = RestClientSetup.sslContextOf(oauth);
      if (context != null) {
        https.setSSLSocketFactory(context.getSocketFactory());
      }
      final var hostnames = RestClientSetup.hostnameVerifierOf(oauth);
      if (hostnames != null) {
        https.setHostnameVerifier(hostnames);
      }
    }
    return connection;

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
