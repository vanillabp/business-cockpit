package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Registering a workflow module creates a gateway route forwarding {@code /wm/{moduleId}/**} to
 * the module's own HTTP server. The test runs such a server itself and checks both directions of
 * the proxying: the rewritten request reaching the module and the module's response reaching the
 * caller. The route is public (permitAll), so no authentication is involved.
 */
class ProxyGatewayTest extends ItestBase {

    private record RecordedRequest(String method, String path, String query, String testHeader, String body) {}

    private static HttpServer moduleStub;
    private static final AtomicReference<RecordedRequest> lastRequest = new AtomicReference<>();

    @BeforeAll
    static void startModuleStub() throws IOException {

        moduleStub = HttpServer.create(new InetSocketAddress(0), 0);
        moduleStub.createContext("/", exchange -> {
            lastRequest.set(new RecordedRequest(
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().getPath(),
                    exchange.getRequestURI().getQuery(),
                    exchange.getRequestHeaders().getFirst("X-Test-Header"),
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
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

    @AfterAll
    static void stopModuleStub() {
        moduleStub.stop(0);
    }

    private String moduleUri() {
        return "http://localhost:" + moduleStub.getAddress().getPort();
    }

    @Test
    void requestsAreForwardedToTheModuleAndResponsesPassedBack() {

        final var moduleId = unique("proxied-module");
        registerWorkflowModule(moduleId, moduleUri());

        // route registration is announced by an application event, so it becomes
        // effective shortly after the registration call returns
        await().untilAsserted(() -> {
            final var response = send(HttpRequest
                    .newBuilder(URI.create(url("/wm/" + moduleId + "/some/path?q=1")))
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
        registerWorkflowModule(moduleId, moduleUri() + "/base");

        await().untilAsserted(() -> {
            final var response = send(HttpRequest
                    .newBuilder(URI.create(url("/wm/" + moduleId + "/sub/resource")))
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
                    .newBuilder(URI.create(url("/wm/" + moduleId + "/api/action")))
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
     * Unregistered module ids fall through to the SPA handling instead of the gateway; asserting
     * on the stub (and not on the status code) keeps this independent of whether the SPA bundle
     * was part of the build.
     */
    @Test
    void unknownModuleRouteIsNotForwarded() {

        lastRequest.set(null);
        send(HttpRequest
                .newBuilder(URI.create(url("/wm/" + unique("never-registered") + "/anything")))
                .GET()
                .build());
        assertThat(lastRequest.get()).isNull();

    }

}
