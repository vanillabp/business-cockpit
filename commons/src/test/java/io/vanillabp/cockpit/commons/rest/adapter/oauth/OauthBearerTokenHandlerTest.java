package io.vanillabp.cockpit.commons.rest.adapter.oauth;

import java.io.IOException;
import java.net.InetSocketAddress;
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
import io.vanillabp.cockpit.commons.rest.adapter.Authentication;
import io.vanillabp.cockpit.commons.rest.adapter.OauthAuthentication;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The handler asks a token endpoint for a token by the client credentials grant. This test serves
 * that endpoint from a local HTTP server and counts how often it is asked. The endpoint is the
 * identity provider of a real installation, and every request of the cockpit used to ask it for a
 * new token.
 */
@ExtendWith(SuppressOutputExtension.class)
class OauthBearerTokenHandlerTest {

  private HttpServer tokenEndpoint;

  private final AtomicInteger tokensIssued = new AtomicInteger();

  /** The JSON field {@code expires_in} the endpoint answers with, or {@code null} for none. */
  private Integer expiresIn;

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

    final var number = tokensIssued.incrementAndGet();
    final var body = (expiresIn == null
        ? "{\"access_token\":\"token-%d\",\"token_type\":\"Bearer\"}".formatted(number)
        : "{\"access_token\":\"token-%d\",\"token_type\":\"Bearer\",\"expires_in\":%d}"
            .formatted(number, expiresIn))
        .getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(200, body.length);
    try (var response = exchange.getResponseBody()) {
      response.write(body);
    }

  }

  private OauthBearerTokenHandler handler() {

    final var oauth = new OauthAuthentication();
    oauth.setBaseUrl("http://localhost:%d/token".formatted(tokenEndpoint.getAddress().getPort()));
    oauth.setClientId("cockpit");
    oauth.setClientSecret("secret");
    final var authentication = new Authentication();
    authentication.setOauth(oauth);
    return new OauthBearerTokenHandler(authentication);

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

}
