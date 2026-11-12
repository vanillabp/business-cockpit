package io.vanillabp.cockpit.support.rest.adapter.oauth;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import feign.RequestTemplate;
import io.vanillabp.cockpit.support.rest.adapter.Authentication;
import io.vanillabp.cockpit.support.rest.adapter.OauthAuthentication;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The handler asks a token endpoint for a token by the client credentials grant. This test serves
 * that endpoint from a local HTTP server and counts how often it is asked. The endpoint is the
 * identity provider of a real installation, and every request of the cockpit used to ask it for a
 * new token. The test also checks what the request for a token looks like, and what a developer
 * reads when the endpoint refuses a token.
 */
@ExtendWith(SuppressOutputExtension.class)
class OauthBearerTokenHandlerTest {

  private HttpServer tokenEndpoint;

  private final AtomicInteger tokensIssued = new AtomicInteger();

  /** The JSON field {@code expires_in} the endpoint answers with, or {@code null} for none. */
  private Integer expiresIn;

  /** An error the endpoint answers with instead of a token, or {@code null} to issue tokens. */
  private String refusal;

  private volatile String lastRequest;

  @BeforeEach
  void startTokenEndpoint() throws IOException {

    // port 0 lets the system pick a free one, which the handler reads back below
    tokenEndpoint = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    tokenEndpoint.createContext("/token", this::issueToken);
    tokenEndpoint.start();

  }

  @AfterEach
  void stopTokenEndpoint() {

    tokenEndpoint.stop(0);

  }

  private void issueToken(
      final HttpExchange exchange) throws IOException {

    lastRequest = exchange.getRequestMethod()
        + " "
        + exchange.getRequestHeaders().getFirst("Content-Type")
        + " "
        + exchange.getRequestHeaders().getFirst("Authorization")
        + " "
        + new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    if (refusal != null) {
      answer(exchange, 400, refusal);
      return;
    }
    final var number = tokensIssued.incrementAndGet();
    final var body = (expiresIn == null
        ? "{\"access_token\":\"token-%d\",\"token_type\":\"Bearer\"}".formatted(number)
        : "{\"access_token\":\"token-%d\",\"token_type\":\"Bearer\",\"expires_in\":%d}"
            .formatted(number, expiresIn));
    answer(exchange, 200, body);

  }

  private static void answer(
      final HttpExchange exchange,
      final int status,
      final String json) throws IOException {

    final var body = json.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, body.length);
    try (var response = exchange.getResponseBody()) {
      response.write(body);
    }

  }

  private OauthAuthentication settings() {

    final var oauth = new OauthAuthentication();
    oauth.setBaseUrl("http://localhost:%d/token".formatted(tokenEndpoint.getAddress().getPort()));
    oauth.setClientId("cockpit");
    oauth.setClientSecret("secret");
    return oauth;

  }

  private static OauthBearerTokenHandler handler(
      final OauthAuthentication oauth) {

    final var authentication = new Authentication();
    authentication.setOauth(oauth);
    return new OauthBearerTokenHandler(authentication);

  }

  private OauthBearerTokenHandler handler() {

    return handler(settings());

  }

  private static String sentWith(
      final OauthBearerTokenHandler handler) {

    final var template = new RequestTemplate();
    handler.apply(template);
    return template.headers().get("Authorization").iterator().next();

  }

  @Test
  void aTokenIsUsedUntilShortlyBeforeItExpires() {

    expiresIn = 300;
    final var handler = handler();

    final var first = sentWith(handler);
    final var second = sentWith(handler);

    Assertions.assertThat(tokensIssued.get()).isEqualTo(1);
    Assertions.assertThat(first).isEqualTo("Bearer token-1");
    Assertions.assertThat(second).isEqualTo("Bearer token-1");

  }

  @Test
  void anExpiredTokenIsReplaced() {

    expiresIn = 0;
    final var handler = handler();

    sentWith(handler);
    final var second = sentWith(handler);

    Assertions.assertThat(tokensIssued.get()).isEqualTo(2);
    Assertions.assertThat(second).isEqualTo("Bearer token-2");

  }

  @Test
  void aTokenWithoutALifetimeIsKept() {

    expiresIn = null;
    final var handler = handler();

    sentWith(handler);
    sentWith(handler);

    Assertions.assertThat(tokensIssued.get()).isEqualTo(1);

  }

  @Test
  void theClientCredentialsGoIntoTheForm() {

    sentWith(handler());

    Assertions.assertThat(lastRequest).isEqualTo(
        "POST application/x-www-form-urlencoded null "
            + "grant_type=client_credentials&client_id=cockpit&client_secret=secret");

  }

  @Test
  void theClientCredentialsGoIntoABasicHeaderIfConfigured() {

    final var oauth = settings();
    oauth.setBasic(true);

    sentWith(handler(oauth));

    Assertions.assertThat(lastRequest).isEqualTo(
        "POST application/x-www-form-urlencoded Basic Y29ja3BpdDpzZWNyZXQ= "
            + "grant_type=client_credentials");

  }

  @Test
  void aRefusalNamesTheKeysToCheck() {

    refusal = "{\"error\":\"invalid_client\",\"error_description\":\"Bad credentials\"}";
    final var handler = handler();

    Assertions.assertThatThrownBy(() -> sentWith(handler))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("client 'cockpit' with status 400, error 'invalid_client': Bad credentials.")
        .hasMessageContaining("'authentication.oauth.client-id'")
        .hasMessageContaining("'authentication.oauth.client-secret'")
        .hasMessageContaining("'authentication.oauth.basic'");

  }

  @Test
  void anAnswerWhichIsNoOauthErrorPointsAtTheBaseUrl() {

    refusal = "<html>not found</html>";
    final var handler = handler();

    Assertions.assertThatThrownBy(() -> sentWith(handler))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("status 400. This is no OAuth error.")
        .hasMessageContaining("'authentication.oauth.base-url'");

  }

  @Test
  void anUnreachableEndpointPointsAtTheBaseUrl() throws IOException {

    final var oauth = settings();
    // a port which was free a moment ago, so nothing listens on it
    try (var socket = new ServerSocket(0)) {
      oauth.setBaseUrl("http://localhost:%d/token".formatted(socket.getLocalPort()));
    }
    final var handler = handler(oauth);

    Assertions.assertThatThrownBy(() -> sentWith(handler))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("could not be reached")
        .hasMessageContaining("'authentication.oauth.base-url'");

  }

  @Test
  void aMissingClientIdIsReportedWhenTheClientIsBuilt() {

    final var oauth = settings();
    oauth.setClientId(null);

    Assertions.assertThatThrownBy(() -> handler(oauth))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Add the property key 'authentication.oauth.client-id'");

  }

  @Test
  void aLifetimeSentAsAStringIsUnderstood() {

    expiresIn = 300;
    final var handler = handler();
    tokenEndpoint.removeContext("/token");
    tokenEndpoint.createContext("/token", exchange -> answer(exchange, 200,
        "{\"access_token\":\"token-%d\",\"expires_in\":\"300\"}"
            .formatted(tokensIssued.incrementAndGet())));

    sentWith(handler);
    sentWith(handler);

    Assertions.assertThat(tokensIssued.get()).isEqualTo(1);

  }

}
