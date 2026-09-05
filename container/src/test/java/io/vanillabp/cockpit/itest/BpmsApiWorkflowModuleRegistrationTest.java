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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The registration endpoint of the BPMS API is an upsert: a workflow module calls it on every
 * start, so the second call for the same id has to update what the first one stored. This test
 * covers that update half - a module that moved to another host, and a module that changed the
 * paths of its provider APIs - while {@link ProxyGatewayTest} covers the first registration.
 *
 * <p>Two stub servers stand in for the module before and after the move, each one answering with
 * its own name so the tests can tell which of them the gateway actually reached.
 */
class BpmsApiWorkflowModuleRegistrationTest extends ItestBase {

    private static HttpServer serverA;
    private static HttpServer serverB;
    private static final AtomicReference<String> pathReceivedByA = new AtomicReference<>();
    private static final AtomicReference<String> pathReceivedByB = new AtomicReference<>();

    private String cookie;

    @BeforeAll
    static void startModuleStubs() throws IOException {

        serverA = startStub("A", pathReceivedByA);
        serverB = startStub("B", pathReceivedByB);

    }

    @AfterAll
    static void stopModuleStubs() {

        serverA.stop(0);
        serverB.stop(0);

    }

    private static HttpServer startStub(
            final String name,
            final AtomicReference<String> pathReceived) throws IOException {

        final var server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            pathReceived.set(exchange.getRequestURI().getPath());
            final var body = ("{ \"servedBy\": \"" + name + "\" }").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        return server;

    }

    private static String uriOf(
            final HttpServer server) {
        return "http://localhost:" + server.getAddress().getPort();
    }

    @BeforeEach
    void loginAsMartin() {

        cookie = loginToGui(USER_MARTIN);

    }

    /**
     * Sends the registration event the way a workflow module does on startup. Unlike
     * {@link ItestBase#registerWorkflowModule(String, String)} the provider paths are part of the
     * signature, because changing them is what one of the tests below is about.
     */
    private void register(
            final String workflowModuleId,
            final String uri,
            final String taskProviderApiUriPath,
            final String workflowProviderApiUriPath) {

        final var response = bpmsV1_1(
                "/workflow-module/" + workflowModuleId,
                """
                {
                  "id": "%s",
                  "uri": "%s",
                  "taskProviderApiUriPath": "%s",
                  "workflowProviderApiUriPath": "%s"
                }
                """.formatted(
                        workflowModuleId, uri, taskProviderApiUriPath, workflowProviderApiUriPath));
        assertThat(response.statusCode()).isEqualTo(200);

    }

    private int proxyCall(
            final String workflowModuleId) {

        final var response = send(HttpRequest
                .newBuilder(URI.create(url("/wm/" + workflowModuleId + "/some/path")))
                .GET()
                .build());
        return response.statusCode();

    }

    private String serverAnsweringTheProxy(
            final String workflowModuleId) {

        final var response = send(HttpRequest
                .newBuilder(URI.create(url("/wm/" + workflowModuleId + "/some/path")))
                .GET()
                .build());
        assertThat(response.statusCode()).isEqualTo(200);
        return json(response).read("$.servedBy", String.class);

    }

    private long storedVersionOf(
            final String workflowModuleId) {

        final var response = guiGet(cookie, "/workflow-module/" + workflowModuleId);
        assertThat(response.statusCode()).isEqualTo(200);
        return json(response).read("$.version", Long.class);

    }

    @Test
    void aRegisteredModuleIsReachableThroughItsProxyPath() {

        final var moduleId = unique("registered-module");
        register(moduleId, uriOf(serverA), "/task-provider/v1", "/workflow-provider/v1");

        // the route becomes effective on the refresh event the registration publishes
        await().untilAsserted(() -> assertThat(serverAnsweringTheProxy(moduleId)).isEqualTo("A"));
        assertThat(pathReceivedByA.get()).isEqualTo("/some/path");

    }

    /**
     * A module that moved to another host announces the move by registering again. The gateway has
     * to follow without a restart of the cockpit, otherwise every request to the module would run
     * into the abandoned address until someone notices.
     */
    @Test
    void reRegistrationWithAChangedUriMovesTheProxyToTheNewServer() {

        final var moduleId = unique("moving-module");
        register(moduleId, uriOf(serverA), "/task-provider/v1", "/workflow-provider/v1");
        await().untilAsserted(() -> assertThat(serverAnsweringTheProxy(moduleId)).isEqualTo("A"));

        final var versionBeforeTheMove = storedVersionOf(moduleId);
        register(moduleId, uriOf(serverB) + "/moved", "/task-provider/v1", "/workflow-provider/v1");
        assertThat(storedVersionOf(moduleId)).isGreaterThan(versionBeforeTheMove);

        await().untilAsserted(() -> assertThat(serverAnsweringTheProxy(moduleId)).isEqualTo("B"));
        assertThat(pathReceivedByB.get()).isEqualTo("/moved/some/path");

    }

    /**
     * The same move, but the module stayed on its server and only shifted the base path underneath
     * which it publishes itself. The target of the route is unchanged then, only the path rewrite
     * differs, which is the part of a route that is easiest to leave stale.
     */
    @Test
    void reRegistrationWithAChangedBasePathMovesTheProxyToTheNewPath() {

        final var moduleId = unique("re-based-module");
        register(moduleId, uriOf(serverA) + "/v1", "/task-provider/v1", "/workflow-provider/v1");
        await().untilAsserted(() -> {
            assertThat(serverAnsweringTheProxy(moduleId)).isEqualTo("A");
            assertThat(pathReceivedByA.get()).isEqualTo("/v1/some/path");
        });

        register(moduleId, uriOf(serverA) + "/v2", "/task-provider/v1", "/workflow-provider/v1");

        await().untilAsserted(() -> {
            assertThat(serverAnsweringTheProxy(moduleId)).isEqualTo("A");
            assertThat(pathReceivedByA.get()).isEqualTo("/v2/some/path");
        });

    }

    /**
     * The provider paths are stored on the module document but never handed to the GUI - the GUI
     * model exposes the proxied URI {@code /wm/{id}} only - so the change is observed through the
     * version counter of the document, which the repository raises on every actual write.
     */
    @Test
    void reRegistrationWithChangedProviderPathsIsStored() {

        final var moduleId = unique("re-pathed-module");
        register(moduleId, uriOf(serverA), "/task-provider/v1", "/workflow-provider/v1");
        final var versionAfterFirstRegistration = storedVersionOf(moduleId);

        register(moduleId, uriOf(serverA), "/tasks/v2", "/workflows/v2");
        final var versionAfterChangedPaths = storedVersionOf(moduleId);
        assertThat(versionAfterChangedPaths).isGreaterThan(versionAfterFirstRegistration);

        // an unchanged registration - what a restarted module sends - must not write anything
        register(moduleId, uriOf(serverA), "/tasks/v2", "/workflows/v2");
        assertThat(storedVersionOf(moduleId)).isEqualTo(versionAfterChangedPaths);

    }

    @Test
    void reRegistrationKeepsTheProxiedUriTheGuiWorksWith() {

        final var moduleId = unique("stable-uri-module");
        register(moduleId, uriOf(serverA), "/task-provider/v1", "/workflow-provider/v1");
        register(moduleId, uriOf(serverB), "/task-provider/v1", "/workflow-provider/v1");

        final var detail = json(guiGet(cookie, "/workflow-module/" + moduleId));
        assertThat(detail.read("$.id", String.class)).isEqualTo(moduleId);
        assertThat(detail.read("$.uri", String.class)).isEqualTo("/wm/" + moduleId);

    }

    @Test
    void registeringAnUnknownModuleIdCreatesIt() {

        final var moduleId = unique("brand-new-module");
        assertThat(guiGet(cookie, "/workflow-module/" + moduleId).statusCode()).isEqualTo(404);

        register(moduleId, uriOf(serverA), "/task-provider/v1", "/workflow-provider/v1");

        assertThat(proxyCall(moduleId)).isNotEqualTo(404);
        assertThat(guiGet(cookie, "/workflow-module/" + moduleId).statusCode()).isEqualTo(200);

    }

}
