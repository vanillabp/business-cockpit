package io.vanillabp.cockpit.config.startup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.mock.env.MockEnvironment;

/**
 * What a cockpit application is told about its configuration, and which of it stops the start.
 *
 * <p>Every assertion is about the two things a message has to carry to be worth anything, the exact
 * property name to add and an example of a value. Nothing here asserts on wording beyond that, so
 * the texts stay free to improve.
 */
@ExtendWith(SuppressOutputExtension.class)
class StartupConfigurationCheckTest {

    /**
     * Everything a cockpit needs, as a starting point for taking exactly one value away.
     */
    private static final Map<String, String> COMPLETE_CONFIGURATION = Map.of(
            CockpitConfiguration.MONGODB_URI, "mongodb://localhost:27017/business-cockpit",
            CockpitConfiguration.TITLE_SHORT, "TestCockpit",
            CockpitConfiguration.TITLE_LONG, "Test Business Cockpit",
            CockpitConfiguration.APPLICATION_VERSION, "1.0",
            CockpitConfiguration.JWT_KEY, "0aH1oXQ4wZk5H0nB8mS1uQ8pQ0kZ1x2y3z4A5b6C7d8=",
            CockpitConfiguration.BPMS_API_REALM_NAME, "BPMS-API",
            CockpitConfiguration.BPMS_API_USERNAME, "bpms",
            CockpitConfiguration.BPMS_API_PASSWORD, "{noop}secret");

    private final StartupConfigurationCheck check = new StartupConfigurationCheck();

    private ListAppender<ILoggingEvent> recordedLog;

    @BeforeEach
    void recordWhatIsLogged() {

        recordedLog = new ListAppender<>();
        recordedLog.start();
        ((ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger(StartupConfigurationCheck.class))
                .addAppender(recordedLog);

    }

    @AfterEach
    void stopRecording() {

        ((ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger(StartupConfigurationCheck.class))
                .detachAppender(recordedLog);

    }

    private MockEnvironment configurationWithout(
            final String... propertyNames) {

        final var left = new HashMap<>(COMPLETE_CONFIGURATION);
        for (final var propertyName : propertyNames) {
            left.remove(propertyName);
        }
        final var environment = new MockEnvironment();
        left.forEach(environment::withProperty);
        return environment;

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

    @Test
    void aFullyConfiguredApplicationIsToldNothing() {

        check.checkConfigurationOf(configurationWithout());

        assertThat(warnings()).isEmpty();

    }

    @Test
    void aMissingDatabaseStopsTheStartAndNamesTheProperty() {

        assertThatThrownBy(() -> check.checkConfigurationOf(
                configurationWithout(CockpitConfiguration.MONGODB_URI)))
                .isInstanceOf(CockpitIsNotConfiguredException.class)
                .hasMessageContaining(CockpitConfiguration.MONGODB_URI)
                .hasMessageContaining("Example: " + CockpitConfiguration.MONGODB_URI + ": mongodb://");

    }

    @Test
    void aDatabaseGivenByHostInsteadOfUriIsAccepted() {

        final var environment = configurationWithout(CockpitConfiguration.MONGODB_URI);
        environment.withProperty(CockpitConfiguration.MONGODB_HOST, "localhost");

        assertThatCode(() -> check.checkConfigurationOf(environment))
                .doesNotThrowAnyException();

    }

    @Test
    void aMissingShortTitleStopsTheStartAndNamesTheProperty() {

        assertThatThrownBy(() -> check.checkConfigurationOf(
                configurationWithout(CockpitConfiguration.TITLE_SHORT)))
                .isInstanceOf(CockpitIsNotConfiguredException.class)
                .hasMessageContaining(CockpitConfiguration.TITLE_SHORT)
                .hasMessageContaining("Example: " + CockpitConfiguration.TITLE_SHORT + ": ");

    }

    /**
     * This is the point of collecting. A developer who starts from nothing reads one message and
     * adds everything at once, instead of restarting into the next single complaint.
     */
    @Test
    void allMandatoryValuesAreReportedInOneMessage() {

        assertThatThrownBy(() -> check.checkConfigurationOf(configurationWithout(
                CockpitConfiguration.MONGODB_URI, CockpitConfiguration.TITLE_SHORT)))
                .isInstanceOf(CockpitIsNotConfiguredException.class)
                .hasMessageContaining("2 mandatory configuration values")
                .hasMessageContaining(CockpitConfiguration.MONGODB_URI)
                .hasMessageContaining(CockpitConfiguration.TITLE_SHORT);

    }

    /**
     * A property set to an empty string is how somebody writes "I will fill this in later", and it
     * is how a start parameter unsets an inherited value. It has to count as missing, otherwise the
     * empty value reaches the code which used to throw.
     */
    @Test
    void anEmptyValueCountsAsAMissingOne() {

        final var environment = configurationWithout();
        environment.withProperty(CockpitConfiguration.TITLE_SHORT, "   ");

        assertThatThrownBy(() -> check.checkConfigurationOf(environment))
                .isInstanceOf(CockpitIsNotConfiguredException.class)
                .hasMessageContaining(CockpitConfiguration.TITLE_SHORT);

    }

    @Test
    void anUnconfiguredBpmsApiLetsTheApplicationBootAndNamesAllThreeProperties() {

        check.checkConfigurationOf(configurationWithout(
                CockpitConfiguration.BPMS_API_REALM_NAME,
                CockpitConfiguration.BPMS_API_USERNAME,
                CockpitConfiguration.BPMS_API_PASSWORD));

        assertThat(warnings())
                .contains("BPMS API")
                .contains(CockpitConfiguration.BPMS_API_REALM_NAME)
                .contains(CockpitConfiguration.BPMS_API_USERNAME)
                .contains(CockpitConfiguration.BPMS_API_PASSWORD)
                .contains("Example: " + CockpitConfiguration.BPMS_API_USERNAME + ": ");

    }

    @Test
    void anIncompleteBpmsApiIsReportedWithTheMissingPropertyOnly() {

        check.checkConfigurationOf(configurationWithout(CockpitConfiguration.BPMS_API_PASSWORD));

        assertThat(warnings())
                .contains(CockpitConfiguration.BPMS_API_PASSWORD)
                .doesNotContain(CockpitConfiguration.BPMS_API_USERNAME);

    }

    /**
     * The key is the one value the check can supply itself, so it does. It hands the generated one
     * over to be written down.
     */
    @Test
    void aMissingSigningKeyIsGeneratedAndHandedToTheDeveloper() {

        final var environment = configurationWithout(CockpitConfiguration.JWT_KEY);

        check.checkConfigurationOf(environment);

        assertThat(warnings()).contains(CockpitConfiguration.JWT_KEY);
        final var generated = environment.getProperty(CockpitConfiguration.JWT_KEY);
        assertThat(generated).isNotBlank();
        assertThat(warnings()).contains("Example: " + CockpitConfiguration.JWT_KEY + ": " + generated);

    }

    @Test
    void valuesTheUserInterfaceOnlyDisplaysAreAWarning() {

        check.checkConfigurationOf(configurationWithout(
                CockpitConfiguration.TITLE_LONG, CockpitConfiguration.APPLICATION_VERSION));

        assertThat(warnings())
                .contains(CockpitConfiguration.TITLE_LONG)
                .contains(CockpitConfiguration.APPLICATION_VERSION)
                .contains("Example: " + CockpitConfiguration.TITLE_LONG + ": ");

    }

    /**
     * Both values only matter once e-mails are actually sent, so an installation without
     * notification is not nagged about them.
     */
    @Test
    void notificationValuesAreOnlyAskedForOnceNotificationIsSwitchedOn() {

        check.checkConfigurationOf(configurationWithout());
        assertThat(warnings()).doesNotContain(CockpitConfiguration.APPLICATION_URI);

        final var withNotification = configurationWithout();
        withNotification.withProperty(CockpitConfiguration.SMTP_ENABLED, "true");
        check.checkConfigurationOf(withNotification);

        assertThat(warnings())
                .contains(CockpitConfiguration.APPLICATION_URI)
                .contains(CockpitConfiguration.SMTP_FROM);

    }

    /**
     * Kafka ingestion switches itself on by the three topics together, so half of them looks
     * configured and consumes nothing.
     */
    @Test
    void kafkaTopicsConfiguredOnlyInPartAreAWarning() {

        final var environment = configurationWithout();
        environment.withProperty(CockpitConfiguration.KAFKA_TOPIC_USER_TASK, "user-tasks");

        check.checkConfigurationOf(environment);

        assertThat(warnings())
                .contains("Kafka")
                .contains(CockpitConfiguration.KAFKA_TOPIC_WORKFLOW)
                .contains(CockpitConfiguration.KAFKA_TOPIC_WORKFLOW_MODULE);

    }

    @Test
    void kafkaIngestionWithoutAWorkerIdStopsTheStart() {

        final var environment = configurationWithout();
        environment.withProperty(CockpitConfiguration.KAFKA_TOPIC_USER_TASK, "user-tasks");
        environment.withProperty(CockpitConfiguration.KAFKA_TOPIC_WORKFLOW, "workflows");
        environment.withProperty(CockpitConfiguration.KAFKA_TOPIC_WORKFLOW_MODULE, "modules");

        assertThatThrownBy(() -> check.checkConfigurationOf(environment))
                .isInstanceOf(CockpitIsNotConfiguredException.class)
                .hasMessageContaining(CockpitConfiguration.WORKER_ID)
                .hasMessageContaining(CockpitConfiguration.KAFKA_GROUP_ID_SUFFIX);

    }

    /**
     * The signing key is written in camel case, which is not a valid configuration property name.
     * It is bound by Spring's relaxed matching all the same, and the check has to find it under
     * exactly the spelling the reference application writes into its yaml.
     */
    @Test
    void theSigningKeyIsFoundUnderTheSpellingUsedInYaml() {

        final var environment = new MockEnvironment()
                .withProperty("business-cockpit.jwt.hmacSHA256-base64", "a-key");

        assertThat(CockpitConfiguration.valueOf(environment, CockpitConfiguration.JWT_KEY))
                .contains("a-key");

    }

    @Test
    void noReplacementOfADotIsAccepted() {

        assertThatCode(() -> check.checkConfigurationOf(configurationWithout()))
                .doesNotThrowAnyException();

    }

    @Test
    void aReplacementOfADotWhichCanWorkIsAccepted() {

        final var environment = configurationWithout()
                .withProperty(CockpitConfiguration.MONGODB_MAP_KEY_DOT_REPLACEMENT, "~");

        assertThatCode(() -> check.checkConfigurationOf(environment))
                .doesNotThrowAnyException();
        assertThat(MapKeyDotReplacement.configuredIn(environment)).contains("~");

    }

    /**
     * A value which cannot work stops the start, and the message says why, which property to
     * change, what it costs and which value to copy.
     */
    @ParameterizedTest
    @ValueSource(strings = { "", " ", ".", "_._", "$", "a$", "\u0000" })
    void aReplacementOfADotWhichCannotWorkStopsTheStart(
            final String replacement) {

        final var environment = configurationWithout()
                .withProperty(CockpitConfiguration.MONGODB_MAP_KEY_DOT_REPLACEMENT, replacement);

        assertThatThrownBy(() -> check.checkConfigurationOf(environment))
                .isInstanceOf(MapKeyDotReplacementIsNotUsableException.class)
                .hasMessageContaining("cannot use '" + replacement + "'")
                .hasMessageContaining(CockpitConfiguration.MONGODB_MAP_KEY_DOT_REPLACEMENT)
                .hasMessageContaining("Search and sorting then find such a key only by its stored form")
                .hasMessageContaining("Example: " + CockpitConfiguration.MONGODB_MAP_KEY_DOT_REPLACEMENT + ": \"~\"");

    }

    @Test
    void aReplacementOfADotWhichCannotWorkIsShownAsTheFailureReport() {

        final var environment = configurationWithout()
                .withProperty(CockpitConfiguration.MONGODB_MAP_KEY_DOT_REPLACEMENT, ".");
        final var failure = catchThrowableOfType(
                MapKeyDotReplacementIsNotUsableException.class,
                () -> check.checkConfigurationOf(environment));

        final var analysis = new MapKeyDotReplacementIsNotUsableFailureAnalyzer().analyze(failure);

        assertThat(analysis.getDescription()).contains("It contains a dot.");
        assertThat(analysis.getAction())
                .contains("leave the property out")
                .contains(CockpitConfiguration.MONGODB_MAP_KEY_DOT_REPLACEMENT);

    }


    @ParameterizedTest
    @ValueSource(strings = { "0", "30", " 45 ", "60" })
    void aLimitOfSortIndexesWhichCanWorkIsAccepted(
            final String limit) {

        final var environment = configurationWithout()
                .withProperty(CockpitConfiguration.MONGODB_SORT_INDEXES_PER_COLLECTION, limit);

        assertThatCode(() -> check.checkConfigurationOf(environment))
                .doesNotThrowAnyException();
        assertThat(SortIndexLimit.configuredIn(environment)).isEqualTo(Integer.parseInt(limit.trim()));

    }

    @Test
    void withoutALimitOfSortIndexesTheDefaultHolds() {

        assertThat(SortIndexLimit.configuredIn(configurationWithout())).isEqualTo(30);

    }

    /**
     * The limit is read only when a list is sorted by a new path. A value which cannot work stops
     * the start instead, and the message names the property and a value to copy.
     */
    @ParameterizedTest
    @ValueSource(strings = { "-1", "61", "thirty", "1.5" })
    void aLimitOfSortIndexesWhichCannotWorkStopsTheStart(
            final String limit) {

        final var environment = configurationWithout()
                .withProperty(CockpitConfiguration.MONGODB_SORT_INDEXES_PER_COLLECTION, limit);

        assertThatThrownBy(() -> check.checkConfigurationOf(environment))
                .isInstanceOf(SortIndexLimitIsNotUsableException.class)
                .hasMessageContaining("The value '" + limit + "'")
                .hasMessageContaining("Example: " + CockpitConfiguration.MONGODB_SORT_INDEXES_PER_COLLECTION + ": 30");

    }

    @Test
    void aLimitOfSortIndexesWhichCannotWorkIsShownAsTheFailureReport() {

        final var environment = configurationWithout()
                .withProperty(CockpitConfiguration.MONGODB_SORT_INDEXES_PER_COLLECTION, "100");
        final var failure = catchThrowableOfType(
                SortIndexLimitIsNotUsableException.class,
                () -> check.checkConfigurationOf(environment));

        final var analysis = new SortIndexLimitIsNotUsableFailureAnalyzer().analyze(failure);

        assertThat(analysis.getDescription())
                .contains(CockpitConfiguration.MONGODB_SORT_INDEXES_PER_COLLECTION)
                .contains("not between 0 and 60");
        assertThat(analysis.getAction()).contains("leave the property out");

    }

    /**
     * A lifetime of the login token is read only when somebody logs in. One which cannot be read
     * stops the start instead, and the message names the property and a value to copy.
     */
    @ParameterizedTest
    @ValueSource(strings = { "12h", "PT0S", "-PT1H", "P1X" })
    void aTokenLifetimeWhichIsNoDurationStopsTheStart(
            final String lifetime) {

        final var environment = configurationWithout()
                .withProperty(CockpitConfiguration.JWT_EXPIRES_DURATION, lifetime);

        assertThatThrownBy(() -> check.checkConfigurationOf(environment))
                .isInstanceOf(JwtLifetimeIsNotUsableException.class)
                .hasMessageContaining("The value '" + lifetime + "' is no duration greater than zero")
                .hasMessageContaining("Example: " + CockpitConfiguration.JWT_EXPIRES_DURATION + ": PT12H")
                .hasMessageNotContaining(CockpitConfiguration.JWT_MAX_LOGIN_DURATION);

    }

    @Test
    void bothTokenLifetimesAreReportedInOneMessage() {

        final var environment = configurationWithout()
                .withProperty(CockpitConfiguration.JWT_EXPIRES_DURATION, "twelve hours")
                .withProperty(CockpitConfiguration.JWT_MAX_LOGIN_DURATION, "a week");

        assertThatThrownBy(() -> check.checkConfigurationOf(environment))
                .isInstanceOf(JwtLifetimeIsNotUsableException.class)
                .hasMessageContaining("Example: " + CockpitConfiguration.JWT_EXPIRES_DURATION + ": PT12H")
                .hasMessageContaining("Example: " + CockpitConfiguration.JWT_MAX_LOGIN_DURATION + ": P7D");

    }

    @Test
    void aTokenLifetimeWhichIsNoDurationIsShownAsTheFailureReport() {

        final var environment = configurationWithout()
                .withProperty(CockpitConfiguration.JWT_MAX_LOGIN_DURATION, "7d");
        final var failure = catchThrowableOfType(
                JwtLifetimeIsNotUsableException.class,
                () -> check.checkConfigurationOf(environment));

        final var analysis = new JwtLifetimeIsNotUsableFailureAnalyzer().analyze(failure);

        assertThat(analysis.getDescription()).contains(CockpitConfiguration.JWT_MAX_LOGIN_DURATION);
        assertThat(analysis.getAction())
                .contains("ISO-8601 duration")
                .contains("leave the property out");

    }

    /**
     * Configured token lifetimes which work are accepted under the spelling used in YAML, too.
     */
    @Test
    void tokenLifetimesWhichWorkAreAccepted() {

        final var environment = configurationWithout()
                .withProperty("business-cockpit.jwt.cookie.expiresDuration", "PT8H")
                .withProperty("business-cockpit.jwt.cookie.maxLoginDuration", "P1D");

        check.checkConfigurationOf(environment);

        assertThat(warnings()).isEmpty();

    }

    /**
     * A maximum shorter than one token switches the renewal off. That boots, with a word about it.
     * {@link #aFullyConfiguredApplicationIsToldNothing()} and
     * {@link #tokenLifetimesWhichWorkAreAccepted()} hold that the default and a longer maximum say
     * nothing.
     */
    @Test
    void aMaximumShorterThanOneTokenIsAWarning() {

        final var environment = configurationWithout()
                .withProperty(CockpitConfiguration.JWT_MAX_LOGIN_DURATION, "PT8H");

        check.checkConfigurationOf(environment);

        assertThat(warnings())
                .contains("Logins are never renewed")
                .contains(CockpitConfiguration.JWT_MAX_LOGIN_DURATION + ": P7D");

    }

}
