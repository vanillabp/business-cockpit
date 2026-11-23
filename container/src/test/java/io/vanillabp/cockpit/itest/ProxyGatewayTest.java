package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Registering a workflow module creates a gateway route forwarding {@code /wm/{moduleId}/**} to
 * the module's own HTTP server. The test runs such a server itself and checks both directions of
 * the proxying, the rewritten request which reaches the module and the module's response which
 * reaches the caller. The route is public (permitAll), so no authentication is involved.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class ProxyGatewayTest extends ItestBase {

  private record RecordedRequest(String method, String path, String query, String testHeader, String body,
                                 String upgrade) {
  }

  private static final String MANIFEST = "{ \"name\": \"a manifest worth compressing\" }";

  private static HttpServer moduleStub;
  private static final AtomicReference<RecordedRequest> lastRequest = new AtomicReference<>();

  @BeforeAll
  static void startModuleStub() throws IOException {

    moduleStub = HttpServer.create(new InetSocketAddress(0), 0);
    moduleStub.createContext("/", exchange -> {
      lastRequest.set(new RecordedRequest(
          exchange.getRequestMethod(), exchange.getRequestURI().getPath(), exchange.getRequestURI().getQuery(), exchange
              .getRequestHeaders().getFirst(
                  "X-Test-Header"), new String(exchange.getRequestBody()
                      .readAllBytes(), StandardCharsets.UTF_8), exchange.getRequestHeaders().getFirst("Upgrade")));
      if (exchange.getRequestURI().getPath().endsWith("/manifest.json")) {
        answerCompressed(exchange);
        return;
      }
      final var body = "{ \"answer\": \"from module\" }".getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.getResponseHeaders().add("X-Module-Header", "module-value");
      exchange.sendResponseHeaders(200, body.length);
      try (var out = exchange.getResponseBody()) {
        out.write(body);
      }
    });
    moduleStub.start();

  }

  /** Answers like a web server which compresses what it serves, if the client accepts gzip. */
  private static void answerCompressed(
      final HttpExchange exchange) throws IOException {

    final var acceptsGzip = String.valueOf(exchange.getRequestHeaders().getFirst("Accept-Encoding")).contains("gzip");
    final var plain = MANIFEST.getBytes(StandardCharsets.UTF_8);
    final byte[] body;
    if (acceptsGzip) {
      final var compressed = new ByteArrayOutputStream();
      try (var gzip = new GZIPOutputStream(compressed)) {
        gzip.write(plain);
      }
      body = compressed.toByteArray();
      exchange.getResponseHeaders().add("Content-Encoding", "gzip");
    } else {
      body = plain;
    }
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(200, body.length);
    try (var out = exchange.getResponseBody()) {
      out.write(body);
    }

  }

  @AfterAll
  static void stopModuleStub() {
    moduleStub.stop(0);
  }

  private String moduleUri() {
    return "http://localhost:"
        + moduleStub.getAddress().getPort();
  }

  @Test
  void requestsAreForwardedToTheModuleAndResponsesPassedBack() {

    final var moduleId = unique("proxied-module");
    registerWorkflowModule(moduleId, moduleUri());

    // route registration is announced by an application event, so it becomes
    // effective shortly after the registration call returns
    await().untilAsserted(() -> {
      final var response = send(HttpRequest
          .newBuilder(URI.create(url("/wm/"
              + moduleId
              + "/some/path?q=1")))
          .header("X-Test-Header", "test-value")
          .GET()
          .build());
      assertThat(response.statusCode()).isEqualTo(200);
      assertThat(response.body()).contains("from module");
      assertThat(response.headers().firstValue("X-Module-Header")).hasValue("module-value");
    });

    final var recorded = lastRequest.get();
    assertThat(recorded.method()).isEqualTo("GET");
    assertThat(recorded.path()).isEqualTo("/some/path");
    assertThat(recorded.query()).isEqualTo("q=1");
    assertThat(recorded.testHeader()).isEqualTo("test-value");

  }

  @Test
  void moduleUriPathIsPrependedWhenForwarding() {

    final var moduleId = unique("proxied-module");
    registerWorkflowModule(moduleId, moduleUri()
        + "/base");

    await().untilAsserted(() -> {
      final var response = send(HttpRequest
          .newBuilder(URI.create(url("/wm/"
              + moduleId
              + "/sub/resource")))
          .GET()
          .build());
      assertThat(response.statusCode()).isEqualTo(200);
    });

    assertThat(lastRequest.get().path()).isEqualTo("/base/sub/resource");

  }

  @Test
  void requestBodiesReachTheModule() {

    final var moduleId = unique("proxied-module");
    registerWorkflowModule(moduleId, moduleUri());

    await().untilAsserted(() -> {
      final var response = send(HttpRequest
          .newBuilder(URI.create(url("/wm/"
              + moduleId
              + "/api/action")))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString("{ \"command\": \"go\" }"))
          .build());
      assertThat(response.statusCode()).isEqualTo(200);
    });

    final var recorded = lastRequest.get();
    assertThat(recorded.method()).isEqualTo("POST");
    assertThat(recorded.path()).isEqualTo("/api/action");
    assertThat(recorded.body()).isEqualTo("{ \"command\": \"go\" }");

  }

  /**
   * A module which compresses its answer hands the browser the compressed bytes and says so in
   * {@code Content-Encoding}. The proxy passes both on as they are. A proxy which unpacked the
   * body but kept the header, or dropped the header, would break the cache of the browser and
   * send more bytes than the module did.
   */
  @Test
  void aCompressedAnswerOfTheModuleReachesTheBrowserAsItIs() throws Exception {

    final var moduleId = unique("proxied-module");
    registerWorkflowModule(moduleId, moduleUri());
    final var request = HttpRequest
        .newBuilder(URI.create(url("/wm/"
            + moduleId
            + "/manifest.json")))
        .header("Accept-Encoding", "gzip")
        .GET()
        .build();

    await().untilAsserted(() -> assertThat(send(request).statusCode()).isEqualTo(200));
    final var response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());

    assertThat(response.headers().firstValue("Content-Encoding")).hasValue("gzip");
    try (var unpacked = new GZIPInputStream(new ByteArrayInputStream(response.body()))) {
      assertThat(new String(unpacked.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo(MANIFEST);
    }

  }

  /**
   * The proxy speaks HTTP/1.1 with the module. It asks for no upgrade to HTTP/2 over a plain
   * connection ({@code Upgrade: h2c}), which some development servers, Vite among them, do not
   * answer the way the client expects.
   */
  @Test
  void theProxyAsksTheModuleForNoUpgrade() {

    final var moduleId = unique("proxied-module");
    registerWorkflowModule(moduleId, moduleUri());

    await().untilAsserted(() -> assertThat(send(HttpRequest
        .newBuilder(URI.create(url("/wm/"
            + moduleId
            + "/plain")))
        .GET()
        .build()).statusCode()).isEqualTo(200));

    assertThat(lastRequest.get().path()).isEqualTo("/plain");
    assertThat(lastRequest.get().upgrade()).isNull();

  }

  /**
   * An unregistered module id falls through to the single-page application instead of the
   * gateway. The assertion is on the stub and not on the status code, which keeps the test
   * independent of whether the bundle of that application was part of the build.
   */
  @Test
  void unknownModuleRouteIsNotForwarded() {

    lastRequest.set(null);
    send(HttpRequest
        .newBuilder(URI.create(url("/wm/"
            + unique("never-registered")
            + "/anything")))
        .GET()
        .build());
    assertThat(lastRequest.get()).isNull();

  }

}
