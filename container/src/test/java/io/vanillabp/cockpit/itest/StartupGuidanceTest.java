package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.sun.net.httpserver.HttpServer;
import io.vanillabp.cockpit.BusinessCockpitStandaloneApplication;
import io.vanillabp.cockpit.config.CockpitMongoTemplate;
import io.vanillabp.cockpit.config.startup.CockpitConfiguration;
import io.vanillabp.cockpit.config.startup.CockpitIsNotConfiguredException;
import io.vanillabp.cockpit.config.startup.WritesAreNotAcknowledgedException;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.mongodb.MongoDBContainer;

/**
 * What a derived application is told when it is started with a value missing. Every case here is a
 * real start of the reference application with one property emptied, because the whole point of
 * the feature is what happens during a start. A value which is only reported once some request
 * touches it has missed its purpose.
 *
 * <p>The assertions are on the two things a developer needs out of a message, the property name and
 * an example value, never on a stack trace. A start which is expected to fail costs almost
 * nothing, because the check runs before the first bean is built.
 *
 * <p>Like {@link ProxyAfterRestartTest} this starts the application itself and not through a
 * cached {@code @SpringBootTest} context. Each case needs its own configuration, and half of them
 * never reach a running context at all.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class StartupGuidanceTest {

    private static final MongoDBContainer MONGODB =
            new MongoDBContainer("mongo:7.0").withReplicaSet();

    private static final HttpClient HTTP = HttpClient
            .newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static HttpServer usersStub;

    private ListAppender<ILoggingEvent> recordedLog;

    @BeforeAll
    static void startInfrastructure() throws IOException {

        MONGODB.start();
        usersStub = startUsersStub();

    }

    @AfterAll
    static void stopInfrastructure() {

        usersStub.stop(0);
        MONGODB.stop();

    }

    private static HttpServer startUsersStub() throws IOException {

        final var users = """
                [
                  { "id": "martin", "email": "martin@example.com", "firstName": "Martin",
                    "lastName": "Meier", "groups": [ "accounting", "bc-users" ] }
                ]
                """.getBytes(StandardCharsets.UTF_8);
        final var server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/dev-shell/user/all", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, users.length);
            try (var out = exchange.getResponseBody()) {
                out.write(users);
            }
        });
        server.start();
        return server;

    }

    @BeforeEach
    void recordWhatIsLogged() {

        recordedLog = new ListAppender<>();
        recordedLog.start();

    }

    /**
     * Attaching the recorder has to wait for the application, not the other way round. Spring Boot
     * configures the logging system while the environment is being prepared, and that throws away
     * whatever was attached before. {@code ApplicationPreparedEvent} is fired after that and still
     * before the first bean, so it is the one moment at which the startup messages can be caught.
     */
    private void recordTheLogOf(
            final SpringApplicationBuilder application) {

        application.listeners(event -> {
            if (event instanceof ApplicationPreparedEvent) {
                ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("io.vanillabp.cockpit"))
                        .addAppender(recordedLog);
            }
        });

    }

    @AfterEach
    void stopRecording() {

        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("io.vanillabp.cockpit"))
                .detachAppender(recordedLog);

    }

    private String warnings() {

        return recordedLog
                .list
                .stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .map(ILoggingEvent::getFormattedMessage)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");

    }

    /**
     * Starts the reference application the way its executable jar does, with the given properties
     * replacing the ones the 'local' profile brings along. An override of an argument already given
     * here replaces it, so a test can hand in an empty value and really unset it.
     */
    private ConfigurableApplicationContext startWith(
            final String... overrides) {

        final var arguments = new ArrayList<>(List.of(
                "--server.port=0",
                "--spring.main.banner-mode=off",
                "--spring.profiles.active=local",
                "--workerId=startup-guidance-itest",
                "--dev-shell-simulator.users-uri=http://localhost:"
                        + usersStub.getAddress().getPort() + "/dev-shell/user",
                "--spring.mongodb.uri=" + MONGODB.getReplicaSetUrl("business-cockpit")));
        for (final var override : overrides) {
            final var key = override.substring(0, override.indexOf('=') + 1);
            arguments.removeIf(argument -> argument.startsWith(key));
            arguments.add(override);
        }
        final var application = new SpringApplicationBuilder(BusinessCockpitStandaloneApplication.class);
        recordTheLogOf(application);
        return application.run(arguments.toArray(String[]::new));

    }

    private static String unset(
            final String propertyName) {

        return "--" + propertyName + "=";

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

    private static String basicAuth(
            final String user,
            final String password) {

        return "Basic " + Base64
                .getEncoder()
                .encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));

    }

    @Test
    void startingWithoutADatabaseNamesThePropertyAndShowsAnExample() {

        assertThatThrownBy(() -> startWith(unset(CockpitConfiguration.MONGODB_URI)))
                .isInstanceOf(CockpitIsNotConfiguredException.class)
                .hasMessageContaining(CockpitConfiguration.MONGODB_URI)
                .hasMessageContaining("Example: " + CockpitConfiguration.MONGODB_URI + ": mongodb://");

    }

    @Test
    void startingWithoutAShortTitleNamesThePropertyAndShowsAnExample() {

        assertThatThrownBy(() -> startWith(unset(CockpitConfiguration.TITLE_SHORT)))
                .isInstanceOf(CockpitIsNotConfiguredException.class)
                .hasMessageContaining(CockpitConfiguration.TITLE_SHORT)
                .hasMessageContaining("Example: " + CockpitConfiguration.TITLE_SHORT + ": ");

    }

    @Test
    void startingWithNothingConfiguredListsEveryMissingValueAtOnce() {

        assertThatThrownBy(() -> startWith(
                unset(CockpitConfiguration.MONGODB_URI),
                unset(CockpitConfiguration.TITLE_SHORT)))
                .isInstanceOf(CockpitIsNotConfiguredException.class)
                .hasMessageContaining(CockpitConfiguration.MONGODB_URI)
                .hasMessageContaining(CockpitConfiguration.TITLE_SHORT);

    }

    /**
     * The BPMS API is what workflow modules and adapters report to. It used to end the start with
     * 'realmName must be specified'. Now the application runs without that value and says so.
     */
    @Test
    void startingWithoutBpmsApiCredentialsBootsWithTheBpmsApiRejectingRequests() {

        try (var application = startWith(
                unset(CockpitConfiguration.BPMS_API_REALM_NAME),
                unset(CockpitConfiguration.BPMS_API_USERNAME),
                unset(CockpitConfiguration.BPMS_API_PASSWORD))) {

            assertThat(warnings())
                    .contains(CockpitConfiguration.BPMS_API_REALM_NAME)
                    .contains(CockpitConfiguration.BPMS_API_USERNAME)
                    .contains(CockpitConfiguration.BPMS_API_PASSWORD)
                    .contains("Example: " + CockpitConfiguration.BPMS_API_USERNAME + ": ");

            final var response = send(HttpRequest
                    .newBuilder(URI.create("http://localhost:" + portOf(application)
                            + "/bpms/api/v1_1/workflow-module/any-module"))
                    .header("Authorization", basicAuth("abc", "123"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{ \"id\": \"any-module\" }"))
                    .build());
            assertThat(response.statusCode()).isEqualTo(401);

        }

    }

    /**
     * Part of the answer to "which write concern does the cockpit run with", and the only place it
     * can be read: the value the delivered configuration brings along, after the database migration
     * has set its own and given the template back.
     */
    @Test
    void theDeliveredConfigurationWritesWithAMajority() {

        try (var application = startWith()) {

            final var template = (CockpitMongoTemplate) application.getBean(MongoTemplate.class);
            assertThat(template.writeConcernApplied().getWObject()).isEqualTo("majority");
            assertThat(template.writeConcernApplied().getJournal()).isTrue();
            assertThat(warnings()).doesNotContain(CockpitConfiguration.MONGODB_WRITE_CONCERN);

        }

    }

    /**
     * A write only the primary has can be lost by a failover, and an installation may still know
     * that its database never fails over. So the cockpit runs and says what it writes with.
     */
    @Test
    void startingWithAWriteConcernWhichCanLoseReportsWarnsAndRuns() {

        try (var application = startWith(
                "--" + CockpitConfiguration.MONGODB_WRITE_CONCERN + "=1")) {

            assertThat(warnings())
                    .contains("'w: 1'")
                    .contains(CockpitConfiguration.MONGODB_WRITE_CONCERN)
                    .contains("Example: " + CockpitConfiguration.MONGODB_WRITE_CONCERN + ": majority");
            assertThat(portOf(application)).isPositive();

        }

    }

    /**
     * The one write concern the cockpit refuses. It reads the result of every write to notice a
     * concurrent change, and an unacknowledged write has no result.
     */
    @Test
    void startingWithUnacknowledgedWritesEndsTheStart() {

        assertThatThrownBy(() -> startWith(
                "--" + CockpitConfiguration.MONGODB_WRITE_CONCERN + "=0"))
                .rootCause()
                .isInstanceOf(WritesAreNotAcknowledgedException.class)
                .hasMessageContaining("'w: 0'")
                .hasMessageContaining(CockpitConfiguration.MONGODB_WRITE_CONCERN)
                .hasMessageContaining("Example: " + CockpitConfiguration.MONGODB_WRITE_CONCERN + ": majority");

    }

    /**
     * Without a signing key the cockpit could not hand out a single login cookie, so one is made up
     * for the run. The login below proves that the run is usable. The warning carries the key to
     * write down, so that the next start keeps everybody logged in.
     */
    @Test
    void startingWithoutASigningKeyBootsAndReportsTheGeneratedKey() {

        try (var application = startWith(unset(CockpitConfiguration.JWT_KEY))) {

            assertThat(warnings())
                    .contains(CockpitConfiguration.JWT_KEY)
                    .contains("Example: " + CockpitConfiguration.JWT_KEY + ": "
                            + application.getEnvironment().getProperty(CockpitConfiguration.JWT_KEY));

            final var response = send(HttpRequest
                    .newBuilder(URI.create("http://localhost:" + portOf(application)
                            + "/gui/api/v1/app/current-user"))
                    .header("Authorization", basicAuth("martin", "test"))
                    .GET()
                    .build());
            assertThat(response.headers().allValues("set-cookie"))
                    .anyMatch(cookie -> cookie.startsWith("bc="));

        }

    }

}
