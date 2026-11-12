package io.vanillabp.cockpit.support.rest.adapter.oauth;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import io.vanillabp.cockpit.support.rest.adapter.Authentication;
import io.vanillabp.cockpit.support.rest.adapter.OauthAuthentication;
import io.vanillabp.cockpit.support.rest.adapter.bearer.BearerTokenBasedAuthInterceptor;
import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adds a bearer token to the &quot;Authorization&quot; header. The token comes from the token
 * endpoint of an OAuth server, by the client credentials grant (RFC 6749, section 4.4).
 * <p>
 * The request to the token endpoint is a form-encoded {@code POST}. The client id and secret go
 * into an HTTP basic authentication header or into the form, as {@code basic} says. The request
 * uses the same proxy, TLS and logging settings as the REST client itself.
 * <p>
 * The token is used until shortly before the time the server gives in {@code expires_in}. Then a
 * new one is fetched. A token without {@code expires_in} is used until a request is answered with
 * 401. See {@code OauthBearerTokenHandlerTest}.
 */
public class OauthBearerTokenHandler extends BearerTokenBasedAuthInterceptor {

  /** The beginning of every property key of the OAuth settings, below the key of the client. */
  private static final String KEYS = "authentication.oauth.";

  private static final JsonMapper JSON = new JsonMapper();

  private final OauthAuthentication properties;

  private final OkHttpClient httpClient;

  /**
   * Creates the handler. The requests to the token endpoint use the client settings found in
   * {@code authenticationProperties.getOauth()}.
   *
   * @param authenticationProperties The authentication settings of the REST client
   * @throws IllegalStateException if the client id or the client secret is missing
   */
  public OauthBearerTokenHandler(
      final Authentication authenticationProperties) {

    properties = authenticationProperties.getOauth();
    requireSet(properties.getClientId(), "client-id");
    requireSet(properties.getClientSecret(), "client-secret");

    // apply common configuration (proxy, logging, etc.) to the requests for a token
    final var httpClientBuilder = new OkHttpClient.Builder();
    super.configureOkHttpClient(this.getClass(), properties, httpClientBuilder, null);
    httpClient = httpClientBuilder.build();

  }

  private void requireSet(
      final String value,
      final String key) {

    if ((value == null) || value.isBlank()) {
      throw new IllegalStateException(
          """
              The REST client has an OAuth token endpoint '%s', but no '%s%s'. \
              Add the property key '%s%s' next to '%sbase-url'."""
              .formatted(properties.getBaseUrl(), KEYS, key, KEYS, key, KEYS));
    }

  }

  @Override
  protected String createToken() {

    return createBearerToken().value();

  }

  @Override
  protected BearerToken createBearerToken() {

    final var form = new FormBody.Builder().add("grant_type", "client_credentials");
    final var request = new Request.Builder()
        .url(properties.getBaseUrl())
        .header("Accept", "application/json");
    if (properties.isBasic()) {
      request.header("Authorization", Credentials.basic(
          properties.getClientId(),
          properties.getClientSecret()));
    } else {
      form
          .add("client_id", properties.getClientId())
          .add("client_secret", properties.getClientSecret());
    }
    request.post(form.build());

    final var fetchedAt = Instant.now();
    try (var response = httpClient.newCall(request.build()).execute()) {

      final var json = readJson(response.body().string());
      if (!response.isSuccessful()) {
        throw new IllegalStateException(explainRefusal(response.code(), json));
      }
      final var accessToken = stringOf(json, "access_token");
      if ((accessToken == null) || accessToken.isBlank()) {
        throw new IllegalStateException(
            """
                The OAuth token endpoint '%s' answered without an 'access_token'. \
                Check that '%sbase-url' is the token endpoint of the OAuth server."""
                .formatted(properties.getBaseUrl(), KEYS));
      }
      final var expiresIn = secondsOf(json, "expires_in");
      return expiresIn == null
          ? new BearerToken(accessToken, null)
          : BearerToken.expiringAfter(accessToken, fetchedAt, Duration.ofSeconds(expiresIn));

    } catch (IOException e) {
      throw new IllegalStateException(
          """
              The OAuth token endpoint '%s' could not be reached. \
              Check '%sbase-url' and, if a proxy is needed, '%sproxy.*'."""
              .formatted(properties.getBaseUrl(), KEYS, KEYS), e);
    }

  }

  /**
   * Turns an error response of the token endpoint (RFC 6749, section 5.2) into a message. The
   * message names the property key which is most likely wrong.
   */
  private String explainRefusal(
      final int status,
      final JsonNode json) {

    final var error = stringOf(json, "error");
    final var description = stringOf(json, "error_description");
    final var answer = "The OAuth token endpoint '%s' refused a token for client '%s' with status %d"
        .formatted(properties.getBaseUrl(), properties.getClientId(), status)
        + (error == null ? "" : ", error '"
            + error
            + "'")
        + (description == null ? "" : ": "
            + description)
        + ". ";
    if (error == null) {
      return answer + "This is no OAuth error. Check that '%sbase-url' is the token endpoint of the OAuth server."
          .formatted(KEYS);
    }
    return answer + switch (error) {
      case "invalid_client" -> """
          Check '%sclient-id' and '%sclient-secret'. Some servers want them in an HTTP basic \
          authentication header and others in the form. If the values are right, change '%sbasic'."""
          .formatted(KEYS, KEYS, KEYS);
      case "unauthorized_client", "unsupported_grant_type" -> """
          The server does not allow the client credentials grant for this client. Allow it on the \
          server, or check that '%sclient-id' names the right client."""
          .formatted(KEYS);
      case "invalid_request" -> """
          Check '%sbasic'. It says whether the client id and secret go into an HTTP basic \
          authentication header or into the form."""
          .formatted(KEYS);
      case "invalid_scope" -> """
          The server wants a scope, and this client sends none. Give the client '%s' a default \
          scope on the server."""
          .formatted(properties.getClientId());
      default -> "Check '%sclient-id', '%sclient-secret' and '%sbasic'.".formatted(KEYS, KEYS, KEYS);
    };

  }

  private static JsonNode readJson(
      final String body) {

    if (body.isBlank()) {
      return null;
    }
    try {
      return JSON.readTree(body);
    } catch (JacksonException e) {
      // not JSON, so it holds neither a token nor an OAuth error
      return null;
    }

  }

  private static String stringOf(
      final JsonNode json,
      final String field) {

    if (json == null) {
      return null;
    }
    final var value = json.path(field);
    return value.isString() ? value.stringValue() : null;

  }

  /**
   * Reads a number of seconds. RFC 6749 asks for a JSON number, but some servers send a string.
   */
  private static Long secondsOf(
      final JsonNode json,
      final String field) {

    final var value = json.path(field);
    if (value.isNumber()) {
      return value.longValue();
    }
    if (value.isString()) {
      try {
        return Long.parseLong(value.stringValue().trim());
      } catch (NumberFormatException e) {
        return null;
      }
    }
    return null;

  }

}
