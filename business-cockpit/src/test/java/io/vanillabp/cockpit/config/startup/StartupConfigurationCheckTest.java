package io.vanillabp.cockpit.config.startup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.env.MockEnvironment;

/**
 * What a cockpit application is told about its configuration, and which of it stops the start.
 *
 * <p>Every assertion is about the two things a message has to carry to be worth anything: the exact
 * property name to add and an example of a value. Nothing here asserts on wording beyond that, so
 * the texts stay free to improve.
 */
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
     * The point of collecting: a developer starting from nothing reads one message and adds
     * everything at once instead of restarting into the next single complaint.
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
     * A property set to an empty string is how somebody writes "I will fill this in later" - and how
     * a start parameter unsets an inherited value. It has to count as missing, otherwise the empty
     * value reaches the code that used to throw.
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
     * The key is the one value the check can supply itself, so it does - and hands the generated one
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

}
