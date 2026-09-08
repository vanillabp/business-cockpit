package io.vanillabp.cockpit.extension.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.sun.net.httpserver.HttpServer;

import io.vanillabp.cockpit.extension.config.RestTransportConfiguration;
import io.vanillabp.cockpit.extension.spi.UserTaskEventKind;
import io.vanillabp.cockpit.extension.transport.RestTransport;
import io.vanillabp.integration.spi.PhaseTwoPermanentFailure;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * What a deployment configures about the connection to the cockpit server, asserted against a
 * server of the test's own.
 * <p>
 * Every one of these settings existed in version 1 of the Business Cockpit, and each of them is
 * something a deployment cannot arrange any other way: the proxy in front of it, the
 * authorization server issuing its tokens, and how long it is willing to wait. What is asserted
 * is what arrives on the wire, because a header which is built but never sent looks exactly
 * like one which is.
 */
@ExtendWith(SuppressOutputExtension.class)
public class RestClientSettingsTest {

  /** One request the test's server received. */
  record Request(
                 String method,
                 String uri,
                 String authorization,
                 String proxyAuthorization,
                 String body) {
  }

  private HttpServer server;

  private final List<Request> received = new LinkedList<>();

  private final AtomicInteger tokensIssued = new AtomicInteger();

  /** How long the cockpit server takes to answer, which one test makes longer than the timeout. */
  private volatile Duration answeringTakes = Duration.ZERO;

  @BeforeEach
  public void startTheServer() throws IOException {

    server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    server
        .createContext(
            "/",
            exchange -> {
              final var body = new String(
                  exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
              received
                  .add(
                      new Request(
                          exchange.getRequestMethod(), exchange.getRequestURI().toString(), exchange.getRequestHeaders()
                              .getFirst("Authorization"), exchange.getRequestHeaders()
                                  .getFirst("Proxy-Authorization"), body));
              if (exchange.getRequestURI().getPath().contains("/token")) {
                final var token = answerOf(exchange.getRequestURI().getPath())
                    .formatted(tokensIssued.incrementAndGet())
                    .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, token.length);
                exchange.getResponseBody().write(token);
                exchange.close();
                return;
              }
              if (!answeringTakes.isZero()) {
                try {
                  Thread.sleep(answeringTakes.toMillis());
                } catch (final InterruptedException e) {
                  Thread.currentThread().interrupt();
                }
              }
              exchange.sendResponseHeaders(200, -1);
              exchange.close();
            });
    server.start();

  }

  /**
   * @param path What the token was asked for under
   * @return What an authorization server answers - the usual answer, and the two answers a
   *         server may give which the extension has to survive
   */
  private static String answerOf(
      final String path) {

    if (path.endsWith("/token-without-an-expiry")) {
      return """
          {"access_token":"token-%d","token_type":"Bearer"}""";
    }
    if (path.endsWith("/token-without-a-token")) {
      return """
          {"token_type":"Bearer","expires_in":300}%.0s""";
    }
    return """
        {"access_token":"token-%d","token_type":"Bearer","expires_in":300}""";

  }

  @AfterEach
  public void stopTheServer() {

    server.stop(0);

  }

  private int port() {

    return server.getAddress().getPort();

  }

  private String at(
      final String path) {

    return "http://localhost:%d%s".formatted(port(), path);

  }

  private static RestTransportConfiguration configuration(
      final String baseUrl,
      final String username,
      final String password,
      final Duration connectTimeout,
      final Duration readTimeout,
      final RestTransportConfiguration.Proxy proxy,
      final RestTransportConfiguration.OAuth oauth) {

    return new RestTransportConfiguration(
        baseUrl, username, password, connectTimeout, readTimeout, proxy, true, null, null, oauth);

  }

  private void report(
      final RestTransportConfiguration configuration) {

    new RestTransport(configuration)
        .publishUserTaskEvent(EventFixture.userTask(UserTaskEventKind.CREATED));

  }

  @Test
  @DisplayName("A configured user name is sent as a basic authentication")
  public void aUserNameBecomesABasicAuthentication() {

    report(configuration(at(""), "cockpit", "s3cret", null, null, null, null));

    assertEquals(1, received.size());
    assertEquals(
        "Basic Y29ja3BpdDpzM2NyZXQ=", received.getFirst().authorization(),
        "the cockpit server was called without the user it was configured with");

  }

  @Test
  @DisplayName("A client-credentials flow puts a bearer token on every report")
  public void aClientCredentialsFlowFetchesABearerToken() {

    final var configuration = configuration(
        at(""), null, null, null, null, null,
        new RestTransportConfiguration.OAuth(
            at("/token"), "taxi-ride", "s3cret", false));
    final var transport = new RestTransport(configuration);

    transport.publishUserTaskEvent(EventFixture.userTask(UserTaskEventKind.CREATED));
    transport.publishUserTaskEvent(EventFixture.userTask(UserTaskEventKind.UPDATED));

    final var tokenRequest = received.getFirst();
    assertTrue(tokenRequest.uri().contains("/token"), tokenRequest.uri());
    assertTrue(
        tokenRequest.body().contains("grant_type=client_credentials"), tokenRequest.body());
    assertTrue(tokenRequest.body().contains("client_id=taxi-ride"), tokenRequest.body());
    assertEquals(
        List.of("Bearer token-1", "Bearer token-1"),
        received
            .stream()
            .filter(request -> !request.uri().contains("/token"))
            .map(Request::authorization)
            .toList(),
        "a token which is still valid was fetched twice");

  }

  @Test
  @DisplayName("A client which identifies itself in a header keeps its secret out of the form")
  public void aClientMayIdentifyItselfInAHeader() {

    report(
        configuration(
            at(""), null, null, null, null, null,
            new RestTransportConfiguration.OAuth(
                at("/token"), "taxi-ride", "s3cret", true)));

    final var tokenRequest = received.getFirst();
    assertEquals("Basic dGF4aS1yaWRlOnMzY3JldA==", tokenRequest.authorization());
    assertEquals("grant_type=client_credentials", tokenRequest.body());

  }

  @Test
  @DisplayName("A token without an expiry is used, and one without a token is refused")
  public void aTokenWithoutAnExpiryIsUsed() {

    report(
        configuration(
            at(""), null, null, null, null, null,
            new RestTransportConfiguration.OAuth(
                at("/token-without-an-expiry"), "taxi-ride", "s3cret", false)));

    assertEquals(
        "Bearer token-1",
        received
            .stream()
            .filter(request -> !request.uri().contains("/token"))
            .findFirst()
            .orElseThrow()
            .authorization());

    final var withoutAToken = assertThrows(
        RuntimeException.class,
        () -> report(
            configuration(
                at(""), null, null, null, null, null,
                new RestTransportConfiguration.OAuth(
                    at("/token-without-a-token"), "taxi-ride", "s3cret", false))));
    assertTrue(withoutAToken.getMessage().contains("access_token"), withoutAToken.getMessage());

  }

  @Test
  @DisplayName("An authorization server which refuses the token has the report repeated")
  public void aRefusedTokenHasTheReportRepeated() {

    final var failure = assertThrows(
        RuntimeException.class,
        () -> report(
            configuration(
                at(""), null, null, null, null, null,
                new RestTransportConfiguration.OAuth(
                    at("/nowhere"), "taxi-ride", "s3cret", false))));

    assertFalse(
        PhaseTwoPermanentFailure.isPermanent(failure),
        "a token which could not be fetched ended the report for good");
    assertTrue(failure.getMessage().contains("taxi-ride"), failure.getMessage());

  }

  @Test
  @DisplayName("A server which takes longer than the read timeout has the report repeated")
  public void aSlowServerRunsIntoTheReadTimeout() {

    answeringTakes = Duration.ofSeconds(2);

    final var failure = assertThrows(
        RuntimeException.class,
        () -> report(
            configuration(at(""), null, null, null, Duration.ofMillis(200), null, null)));

    assertFalse(
        PhaseTwoPermanentFailure.isPermanent(failure),
        "a server which was merely slow ended the report for good");
    assertNotNull(failure.getMessage());

  }

  @Test
  @DisplayName("Every request passes the proxy, carrying what the proxy expects")
  public void requestsPassTheConfiguredProxy() {

    report(
        configuration(
            "http://cockpit.invalid:8080", null, null, null, null,
            new RestTransportConfiguration.Proxy(
                "localhost", port(), "walter", "s3cret"),
            null));

    final var request = received.getFirst();
    assertTrue(
        request.uri().startsWith("http://cockpit.invalid:8080/bpms/api/"),
        "the proxy was asked for something other than the cockpit server: "
            + request.uri());
    assertEquals("Basic d2FsdGVyOnMzY3JldA==", request.proxyAuthorization());

  }

}
