package io.vanillabp.cockpit.itest;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.errors.TopicExistsException;
import org.awaitility.Awaitility;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.mongodb.MongoDBContainer;

/**
 * Base for black-box integration tests: boots the complete application on a random port and talks
 * to it exclusively over HTTP, the way real clients do. Nothing in here (nor in the tests built on
 * it) touches types of the web stack, which is what let the suite survive the move from WebFlux to
 * Spring MVC unchanged.
 *
 * <p>Infrastructure is shared across all test classes: one MongoDB replica set container (change
 * streams and the changeset migration require a replica set), one Apache Kafka container and one
 * stub HTTP server playing the role of the dev-shell simulator the 'local' profile loads its users
 * from. The containers live in static fields initialized once per JVM, and every test class uses
 * the identical context configuration, so Spring caches a single application context.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "workerId=itest-worker",
                "bpms-api.kafka.group-id-suffix=itest",
                "bpms-api.kafka.topics.user-task=" + ItestBase.KAFKA_USER_TASK_TOPIC,
                "bpms-api.kafka.topics.workflow=" + ItestBase.KAFKA_WORKFLOW_TOPIC,
                "bpms-api.kafka.topics.workflow-module=" + ItestBase.KAFKA_WORKFLOW_MODULE_TOPIC,
                // the consumer group is created while the tests already produce, so do not skip
                // records published before the group's first assignment
                "spring.kafka.consumer.auto-offset-reset=earliest"
        })
@ActiveProfiles("local")
public abstract class ItestBase {

    public static final String KAFKA_USER_TASK_TOPIC = "itest-user-tasks";
    public static final String KAFKA_WORKFLOW_TOPIC = "itest-workflows";
    public static final String KAFKA_WORKFLOW_MODULE_TOPIC = "itest-modules";

    /** Credentials of the BPMS API basic-auth client as configured in application-local.yaml. */
    protected static final String BPMS_API_USER = "abc";
    protected static final String BPMS_API_PASSWORD = "123";

    /** Users served by the dev-shell simulator stub; the GUI password is fixed to "test". */
    protected static final String USER_MARTIN = "martin";
    protected static final String USER_PETRA = "petra";
    protected static final String GUI_PASSWORD = "test";
    protected static final String GROUP_OF_MARTIN = "accounting";
    protected static final String GROUP_OF_PETRA = "sales";

    private static final MongoDBContainer MONGODB =
            new MongoDBContainer("mongo:7.0").withReplicaSet();
    private static final KafkaContainer KAFKA =
            new KafkaContainer("apache/kafka:3.9.1");
    private static final HttpServer USERS_STUB;

    protected static final HttpClient HTTP = HttpClient
            .newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    static {
        Startables.deepStart(MONGODB, KAFKA).join();
        USERS_STUB = startUsersStub();
        createKafkaTopics();
        Awaitility.setDefaultTimeout(Duration.ofSeconds(30));
        Awaitility.setDefaultPollInterval(Duration.ofMillis(250));
    }

    @DynamicPropertySource
    static void wireSharedInfrastructure(
            final DynamicPropertyRegistry registry) {

        registry.add("spring.mongodb.uri", () -> MONGODB.getReplicaSetUrl("business-cockpit"));
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("dev-shell-simulator.users-uri",
                () -> "http://localhost:" + USERS_STUB.getAddress().getPort() + "/dev-shell/user");

    }

    private static HttpServer startUsersStub() {

        final var users = """
                [
                  { "id": "martin", "email": "martin@example.com", "firstName": "Martin",
                    "lastName": "Meier", "groups": [ "accounting", "bc-users" ] },
                  { "id": "petra", "email": "petra@example.com", "firstName": "Petra",
                    "lastName": "Huber", "groups": [ "sales", "bc-users" ] }
                ]
                """;
        try {
            final var server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/dev-shell/user/all", exchange -> {
                final var body = users.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (var out = exchange.getResponseBody()) {
                    out.write(body);
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException("Could not start users stub", e);
        }

    }

    private static void createKafkaTopics() {

        try (var admin = Admin.create(java.util.Map.of(
                "bootstrap.servers", KAFKA.getBootstrapServers()))) {
            final var topics = List.of(
                    new NewTopic(KAFKA_USER_TASK_TOPIC, 1, (short) 1),
                    new NewTopic(KAFKA_WORKFLOW_TOPIC, 1, (short) 1),
                    new NewTopic(KAFKA_WORKFLOW_MODULE_TOPIC, 1, (short) 1));
            admin.createTopics(topics).all().get();
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof TopicExistsException)) {
                throw new IllegalStateException("Could not create Kafka topics", e);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while creating Kafka topics", e);
        }

    }

    protected static String kafkaBootstrapServers() {
        return KAFKA.getBootstrapServers();
    }

    @LocalServerPort
    protected int port;

    protected String url(
            final String path) {
        return "http://localhost:" + port + path;
    }

    protected static String unique(
            final String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    protected static String isoNow() {
        return OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    protected static String basicAuth(
            final String user,
            final String password) {
        return "Basic " + Base64
                .getEncoder()
                .encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    protected HttpResponse<String> send(
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

    /**
     * The server omits null properties, so a missing leaf reads as null instead of failing - that
     * way tests can assert absent fields (e.g. an unset assignee) the same as null ones.
     */
    private static final Configuration JSON_PATH_CONFIGURATION = Configuration
            .builder()
            .options(Option.DEFAULT_PATH_LEAF_TO_NULL)
            .build();

    protected static DocumentContext json(
            final HttpResponse<String> response) {
        return JsonPath.parse(response.body(), JSON_PATH_CONFIGURATION);
    }

    // BPMS API (basic auth on every request, like a real reporting system)

    protected HttpResponse<String> bpmsV1_1(
            final String path,
            final String jsonBody) {

        return send(HttpRequest
                .newBuilder(URI.create(url("/bpms/api/v1_1" + path)))
                .header("Authorization", basicAuth(BPMS_API_USER, BPMS_API_PASSWORD))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build());

    }

    protected void registerWorkflowModule(
            final String workflowModuleId,
            final String uri) {

        final var response = bpmsV1_1(
                "/workflow-module/" + workflowModuleId,
                """
                {
                  "id": "%s",
                  "uri": "%s",
                  "taskProviderApiUriPath": "/task-provider/v1",
                  "workflowProviderApiUriPath": "/workflow-provider/v1"
                }
                """.formatted(workflowModuleId, uri));
        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "Could not register workflow module '" + workflowModuleId + "': HTTP "
                            + response.statusCode() + " " + response.body());
        }

    }

    // GUI API (login by basic auth once, then the JWT cookie like the SPA does)

    /**
     * Authenticates like the web app: the first request carries basic auth, the response sets the
     * JWT cookie which authenticates all subsequent requests.
     *
     * @return the cookie pair "bc=..." to be sent as Cookie header
     */
    protected String loginToGui(
            final String userId) {

        final var response = send(HttpRequest
                .newBuilder(URI.create(url("/gui/api/v1/app/current-user")))
                .header("Authorization", basicAuth(userId, GUI_PASSWORD))
                .GET()
                .build());
        return response
                .headers()
                .allValues("set-cookie")
                .stream()
                .filter(cookie -> cookie.startsWith("bc="))
                .map(cookie -> cookie.substring(0, cookie.indexOf(';')))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Login of '" + userId + "' did not set the JWT cookie; HTTP "
                                + response.statusCode()));

    }

    protected HttpResponse<String> guiGet(
            final String cookie,
            final String path) {

        return send(HttpRequest
                .newBuilder(URI.create(url("/gui/api/v1" + path)))
                .header("Cookie", cookie)
                .GET()
                .build());

    }

    protected HttpResponse<String> guiPost(
            final String cookie,
            final String path,
            final String jsonBody) {

        return send(HttpRequest
                .newBuilder(URI.create(url("/gui/api/v1" + path)))
                .header("Cookie", cookie)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build());

    }

    protected HttpResponse<String> guiPatch(
            final String cookie,
            final String path,
            final String jsonBody) {

        final var builder = HttpRequest
                .newBuilder(URI.create(url("/gui/api/v1" + path)))
                .header("Cookie", cookie);
        if (jsonBody == null) {
            builder.method("PATCH", HttpRequest.BodyPublishers.noBody());
        } else {
            builder
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody));
        }
        return send(builder.build());

    }

    /**
     * Fetches the current user's task list filtered by a fulltext token, which isolates the test's
     * own tasks from those created by other tests sharing the application context.
     */
    protected DocumentContext userTaskList(
            final String cookie,
            final String fulltextToken,
            final String mode) {

        final var response = guiPost(cookie, "/usertask", """
                {
                  "pageNumber": 0,
                  "pageSize": 50,
                  "searchQueries": [ { "query": "%s" } ],
                  "sortAscending": true,
                  "mode": "%s"
                }
                """.formatted(fulltextToken, mode));
        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "Task list request failed: HTTP " + response.statusCode() + " " + response.body());
        }
        return json(response);

    }

    protected DocumentContext workflowList(
            final String cookie,
            final String fulltextToken,
            final String mode) {

        final var response = guiPost(cookie, "/workflow", """
                {
                  "pageNumber": 0,
                  "pageSize": 50,
                  "searchQueries": [ { "query": "%s" } ],
                  "sortAscending": true,
                  "mode": "%s"
                }
                """.formatted(fulltextToken, mode));
        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "Workflow list request failed: HTTP " + response.statusCode() + " " + response.body());
        }
        return json(response);

    }

}
