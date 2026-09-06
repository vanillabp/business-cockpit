package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.sun.net.httpserver.HttpServer;
import io.vanillabp.cockpit.BusinessCockpitApplication;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.mongodb.MongoDBContainer;

/**
 * Workflow modules register themselves once, at their own startup, and are not asked again when
 * the cockpit restarts. The routes therefore have to survive a restart on their own: they are
 * rebuilt from the module documents in MongoDB while the application starts.
 *
 * <p>Proving that needs two application lifecycles in a row, which a cached
 * {@link org.springframework.boot.test.context.SpringBootTest} context cannot offer - so this test
 * starts the application itself, twice, against one database that outlives both runs. It also
 * brings its own MongoDB rather than sharing the one of {@link ItestBase}, which keeps the two
 * kinds of test independent of each other's lifecycle.
 *
 * <p>Kafka is left out: without the {@code bpms-api.kafka.topics.*} properties the consumer side
 * does not come up at all, and nothing in this scenario needs it.
 */
class ProxyAfterRestartTest {

    private static final String BPMS_API_USER = "abc";
    private static final String BPMS_API_PASSWORD = "123";

    private static final MongoDBContainer MONGODB =
            new MongoDBContainer("mongo:7.0").withReplicaSet();

    private static final HttpClient HTTP = HttpClient
            .newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static HttpServer usersStub;
    private static HttpServer moduleStub;
    private static final AtomicReference<String> pathReceivedByModule = new AtomicReference<>();

    @BeforeAll
    static void startInfrastructure() throws IOException {

        MONGODB.start();
        usersStub = startStub("/dev-shell/user/all", """
                [
                  { "id": "martin", "email": "martin@example.com", "firstName": "Martin",
                    "lastName": "Meier", "groups": [ "accounting", "bc-users" ] }
                ]
                """, null);
        moduleStub = startStub("/", "{ \"servedBy\": \"module\" }", pathReceivedByModule);

    }

    @AfterAll
    static void stopInfrastructure() {

        moduleStub.stop(0);
        usersStub.stop(0);
        MONGODB.stop();

    }

    private static HttpServer startStub(
            final String path,
            final String response,
            final AtomicReference<String> pathReceived) throws IOException {

        final var server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(path, exchange -> {
            if (pathReceived != null) {
                pathReceived.set(exchange.getRequestURI().getPath());
            }
            final var body = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        return server;

    }

    /**
     * Boots the application the way the executable jar does, on a free port and against the shared
     * database. The arguments are passed on the command line rather than as default properties so
     * that they outrank the settings the 'local' profile brings along.
     */
    private static ConfigurableApplicationContext startApplication(
            final String workerId) {

        return new SpringApplicationBuilder(BusinessCockpitApplication.class)
                .run(
                        "--server.port=0",
                        "--spring.main.banner-mode=off",
                        "--spring.profiles.active=local",
                        "--workerId=" + workerId,
                        "--spring.mongodb.uri=" + MONGODB.getReplicaSetUrl("business-cockpit"),
                        "--dev-shell-simulator.users-uri=http://localhost:"
                                + usersStub.getAddress().getPort() + "/dev-shell/user");

    }

    private static int portOf(
            final ConfigurableApplicationContext context) {

        return ((WebServerApplicationContext) context).getWebServer().getPort();

    }

    private static HttpResponse<String> send(
            final HttpRequest request) {

        try {
            return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new IllegalStateException("HTTP request failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("HTTP request interrupted", e);
        }

    }

    private static void registerWorkflowModule(
            final int port,
            final String workflowModuleId,
            final String uri) {

        final var credentials = Base64
                .getEncoder()
                .encodeToString((BPMS_API_USER + ":" + BPMS_API_PASSWORD).getBytes(StandardCharsets.UTF_8));
        final var response = send(HttpRequest
                .newBuilder(URI.create(
                        "http://localhost:" + port + "/bpms/api/v1_1/workflow-module/" + workflowModuleId))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "id": "%s",
                          "uri": "%s",
                          "taskProviderApiUriPath": "/task-provider/v1",
                          "workflowProviderApiUriPath": "/workflow-provider/v1"
                        }
                        """.formatted(workflowModuleId, uri)))
                .build());
        assertThat(response.statusCode()).isEqualTo(200);

    }

    private static void assertProxyReachesTheModule(
            final int port,
            final String workflowModuleId) {

        pathReceivedByModule.set(null);
        await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(250))
                .untilAsserted(() -> {
                    final var response = send(HttpRequest
                            .newBuilder(URI.create(
                                    "http://localhost:" + port + "/wm/" + workflowModuleId + "/some/path"))
                            .GET()
                            .build());
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(response.body()).contains("module");
                });
        assertThat(pathReceivedByModule.get()).isEqualTo("/some/path");

    }

    @Test
    void aModuleRegisteredBeforeARestartIsProxiedAgainAfterIt() {

        final var moduleId = "restarted-module-" + UUID.randomUUID();
        final var moduleUri = "http://localhost:" + moduleStub.getAddress().getPort();

        final var firstRun = startApplication("restart-itest-1");
        try {
            registerWorkflowModule(portOf(firstRun), moduleId, moduleUri);
            assertProxyReachesTheModule(portOf(firstRun), moduleId);
        } finally {
            firstRun.close();
        }

        // same database, no registration this time: the routes may only come from what the first
        // run stored, and they are rebuilt while the application starts
        final var secondRun = startApplication("restart-itest-2");
        try {
            assertProxyReachesTheModule(portOf(secondRun), moduleId);
        } finally {
            secondRun.close();
        }

    }

}
