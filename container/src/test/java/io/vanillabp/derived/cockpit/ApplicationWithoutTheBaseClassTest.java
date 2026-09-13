package io.vanillabp.derived.cockpit;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.io.IOException;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.mongodb.MongoDBContainer;

/**
 * The point of the auto-configurations: an application which adds the dependency and writes nothing
 * else runs, serves the user interface, and its lists are empty only because nobody has reported
 * anything yet. The last part is why this test reports something and looks again.
 *
 * <p>{@link DerivedCockpitApplication} is that application, and its package is what makes the test
 * mean something. The delivered {@code container} lives in {@code io.vanillabp.cockpit} and finds the
 * library's beans through its own component scan, so its tests pass whether the auto-configurations
 * work or not.
 *
 * <p>Started like {@code StartupGuidanceTest} does, with a builder rather than a cached
 * {@code @SpringBootTest} context: the configuration here is the application's own and has nothing to
 * do with the {@code local} profile of the delivered one.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class ApplicationWithoutTheBaseClassTest {

    private static final String BPMS_API_USER = "reporting";

    private static final String BPMS_API_PASSWORD = "secret";

    private static final String GUI_PASSWORD = "test";

    private static final MongoDBContainer MONGODB =
            new MongoDBContainer("mongo:7.0").withReplicaSet();

    private static final HttpClient HTTP = HttpClient
            .newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static ConfigurableApplicationContext application;

    private static int port;

    private static String startupLog;

    @BeforeAll
    static void startTheApplication() {

        MONGODB.start();
        final var recorded = new ListAppender<ILoggingEvent>();
        recorded.start();
        application = new SpringApplicationBuilder(DerivedCockpitApplication.class)
                // Spring Boot configures the logging system while the environment is prepared and
                // throws away whatever was attached before, so the recorder waits for the event
                // fired after that and still before the first bean
                .listeners(event -> {
                    if (event instanceof ApplicationPreparedEvent) {
                        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("io.vanillabp.cockpit"))
                                .addAppender(recorded);
                    }
                })
                .run(
                        "--server.port=0",
                        "--spring.main.banner-mode=off",
                        "--spring.mongodb.uri=" + MONGODB.getReplicaSetUrl("derived-cockpit"),
                        "--business-cockpit.title-short=Derived",
                        "--business-cockpit.title-long=A cockpit of its own",
                        "--business-cockpit.application-version=1.2.3",
                        "--business-cockpit.application-uri=http://localhost",
                        "--business-cockpit.jwt.hmacSHA256-base64="
                                + Base64.getEncoder().encodeToString(new byte[32]),
                        "--bpms-api.realm-name=BPMS-API",
                        "--bpms-api.username=" + BPMS_API_USER,
                        "--bpms-api.password={noop}" + BPMS_API_PASSWORD);
        port = ((WebServerApplicationContext) application).getWebServer().getPort();
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("io.vanillabp.cockpit"))
                .detachAppender(recorded);
        startupLog = recorded
                .list
                .stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");

    }

    @AfterAll
    static void stopTheApplication() {

        if (application != null) {
            application.close();
        }
        MONGODB.stop();

    }

    /**
     * The shell of the single-page application, which the library carries in its own jar. The one
     * answered here is the stand-in in {@code src/test/resources/static/index.html}, because this
     * build does not run the webapp build.
     */
    @Test
    void theUserInterfaceIsServedAtTheRoot() {

        final var cookie = login(DerivedCockpitApplication.USER);

        final var root = get("/", cookie);
        assertThat(root.statusCode()).isEqualTo(200);
        assertThat(root.body()).contains("id=\"root\"");

        final var deepLink = get("/tasklist/some-task-id", cookie);
        assertThat(deepLink.statusCode()).isEqualTo(200);
        assertThat(deepLink.body()).contains("id=\"root\"");

    }

    /**
     * Both lists answer an empty page before anything is reported, and hold what is reported
     * afterwards. An empty answer alone would not say much: a list nobody wired would look the same
     * from the outside, with a 404 or a 500 instead of a page.
     */
    @Test
    void theListsAreEmptyUntilSomethingIsReported() {

        final var cookie = login(DerivedCockpitApplication.USER);
        final var token = unique("token");

        assertThat(userTasks(cookie, token).read("$.page.totalElements", Integer.class)).isZero();
        assertThat(workflows(cookie, token).read("$.page.totalElements", Integer.class)).isZero();

        final var moduleId = unique("module");
        registerWorkflowModule(moduleId);
        final var userTaskId = unique("task");
        reportUserTask(moduleId, userTaskId, token);
        final var workflowId = unique("workflow");
        reportWorkflow(moduleId, workflowId, token);

        assertThat(userTasks(cookie, token).read("$.userTasks[*].id", List.class))
                .containsExactly(userTaskId);
        assertThat(workflows(cookie, token).read("$.workflows[*].id", List.class))
                .containsExactly(workflowId);

        final var modules = send(HttpRequest
                .newBuilder(URI.create(url("/gui/api/v1/workflow-module")))
                .header("Cookie", cookie)
                .GET()
                .build());
        assertThat(modules.statusCode()).isEqualTo(200);
        assertThat(json(modules).read("$.modules[*].id", List.class)).contains(moduleId);

    }

    /**
     * Four annotations of the auto-configurations act on the whole application and not only on the
     * cockpit, so each of them says on every start what it does and what to write to decide it
     * instead. A developer who never reads the wiki has to meet them in the log.
     */
    @Test
    void everySwitchAffectingTheWholeApplicationIsReportedOnStartup() {

        assertThat(startupLog)
                .contains("switched @EnableWebSecurity on for the whole application")
                .contains("switched @EnableMethodSecurity(securedEnabled = true) on for the whole application")
                .contains("switched @EnableAsync on for the whole application")
                .contains("switched @EnableScheduling on for the whole application")
                .contains("business-cockpit.method-security.enabled")
                .contains("business-cockpit.async.enabled")
                .contains("business-cockpit.scheduling.enabled");

    }

    /**
     * The BPMS API is protected by a chain of its own, which is the one thing the reports above could
     * have gone through by accident had the cockpit's chains been replaced by Spring Boot's default.
     */
    @Test
    void theBpmsApiRejectsAClientWithoutTheConfiguredCredentials() {

        final var response = send(HttpRequest
                .newBuilder(URI.create(url("/bpms/api/v1_1/workflow-module/any-module")))
                .header("Authorization", basicAuth(BPMS_API_USER, "wrong"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{ \"id\": \"any-module\" }"))
                .build());

        assertThat(response.statusCode()).isEqualTo(401);

    }

    private static String url(
            final String path) {

        return "http://localhost:" + port + path;

    }

    private static String unique(
            final String prefix) {

        return prefix + "-" + UUID.randomUUID();

    }

    private static String basicAuth(
            final String user,
            final String password) {

        return "Basic " + Base64
                .getEncoder()
                .encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));

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

    private static final Configuration JSON_PATH = Configuration
            .builder()
            .options(Option.DEFAULT_PATH_LEAF_TO_NULL)
            .build();

    private static DocumentContext json(
            final HttpResponse<String> response) {

        return JsonPath.parse(response.body(), JSON_PATH);

    }

    private String login(
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
                        "Login of '" + userId + "' set no cookie; HTTP " + response.statusCode()));

    }

    private HttpResponse<String> get(
            final String path,
            final String cookie) {

        return send(HttpRequest
                .newBuilder(URI.create(url(path)))
                .header("Cookie", cookie)
                .header("Accept", "text/html")
                .GET()
                .build());

    }

    private DocumentContext listOf(
            final String path,
            final String cookie,
            final String fulltextToken,
            final String mode) {

        final var response = send(HttpRequest
                .newBuilder(URI.create(url("/gui/api/v1" + path)))
                .header("Cookie", cookie)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "pageNumber": 0,
                          "pageSize": 50,
                          "searchQueries": [ { "query": "%s" } ],
                          "sortAscending": true,
                          "mode": "%s"
                        }
                        """.formatted(fulltextToken, mode)))
                .build());
        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "Request to '" + path + "' failed: HTTP " + response.statusCode() + " "
                            + response.body());
        }
        return json(response);

    }

    private DocumentContext userTasks(
            final String cookie,
            final String fulltextToken) {

        return listOf("/usertask", cookie, fulltextToken, "OpenTasks");

    }

    private DocumentContext workflows(
            final String cookie,
            final String fulltextToken) {

        return listOf("/workflow", cookie, fulltextToken, "Active");

    }

    private HttpResponse<String> bpmsApi(
            final String path,
            final String body) {

        return send(HttpRequest
                .newBuilder(URI.create(url("/bpms/api/v1_1" + path)))
                .header("Authorization", basicAuth(BPMS_API_USER, BPMS_API_PASSWORD))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build());

    }

    private void registerWorkflowModule(
            final String moduleId) {

        final var response = bpmsApi("/workflow-module/" + moduleId, """
                {
                  "id": "%s",
                  "uri": "http://localhost:65000",
                  "taskProviderApiUriPath": "/task-provider/v1",
                  "workflowProviderApiUriPath": "/workflow-provider/v1"
                }
                """.formatted(moduleId));
        assertThat(response.statusCode()).isEqualTo(200);

    }

    private void reportUserTask(
            final String moduleId,
            final String userTaskId,
            final String fulltextToken) {

        final var response = bpmsApi("/usertask/created", """
                {
                  "id": "%s",
                  "userTaskId": "%s",
                  "timestamp": "%s",
                  "workflowModuleId": "%s",
                  "bpmnProcessId": "taxi-ride",
                  "bpmnProcessVersion": "1",
                  "businessId": "ride-4711",
                  "title": { "en": "Do ride 4711" },
                  "taskDefinition": "do-ride",
                  "taskDefinitionTitle": { "en": "Do ride" },
                  "uiUriPath": "/remoteEntry.js",
                  "uiUriType": "WEBPACK_MF_REACT",
                  "detailsFulltextSearch": "%s",
                  "assignee": "%s"
                }
                """.formatted(unique("event"), userTaskId, isoNow(), moduleId, fulltextToken,
                DerivedCockpitApplication.USER));
        assertThat(response.statusCode()).isEqualTo(200);

    }

    private void reportWorkflow(
            final String moduleId,
            final String workflowId,
            final String fulltextToken) {

        final var response = bpmsApi("/workflow/created", """
                {
                  "id": "%s",
                  "workflowId": "%s",
                  "timestamp": "%s",
                  "workflowModuleId": "%s",
                  "bpmnProcessId": "taxi-ride",
                  "bpmnProcessVersion": "1",
                  "businessId": "ride-4711",
                  "title": { "en": "Ride 4711" },
                  "uiUriPath": "/remoteEntry.js",
                  "uiUriType": "WEBPACK_MF_REACT",
                  "initiator": "%s",
                  "detailsFulltextSearch": "%s"
                }
                """.formatted(unique("event"), workflowId, isoNow(), moduleId,
                DerivedCockpitApplication.USER, fulltextToken));
        assertThat(response.statusCode()).isEqualTo(200);

    }

    private static String isoNow() {

        return OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

    }

}
