package io.vanillabp.cockpit.config.startup;

import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.APPLICATION_URI;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.APPLICATION_VERSION;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.BPMS_API_PASSWORD;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.BPMS_API_REALM_NAME;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.BPMS_API_USERNAME;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.JWT_KEY;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.KAFKA_GROUP_ID_SUFFIX;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.KAFKA_TOPIC_USER_TASK;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.KAFKA_TOPIC_WORKFLOW;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.KAFKA_TOPIC_WORKFLOW_MODULE;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.MONGODB_HOST;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.MONGODB_URI;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.SMTP_ENABLED;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.SMTP_FROM;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.TITLE_LONG;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.TITLE_SHORT;
import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.WORKER_ID;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.crypto.KeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Component;

/**
 * Reports everything a cockpit application still has to configure, on every start and in as few
 * messages as possible.
 * <p>
 * It is a {@link BeanFactoryPostProcessor} because that is the last moment before the first bean is
 * built and therefore the earliest moment at which the whole configuration can be judged at once.
 * Every value checked here used to surface much later and much worse: as
 * {@code realmName must be specified} out of a security filter chain, as
 * {@code Cannot pass null or empty values to constructor} out of a user details manager, as a
 * connection timeout against a MongoDB nobody configured, or - for the signing key - only once
 * somebody tried to log in.
 * <p>
 * What is missing decides how it is reported:
 * <ul>
 * <li>Values without which no part of the cockpit works stop the start, collected into one
 * {@link CockpitIsNotConfiguredException} listing all of them.</li>
 * <li>Values that switch off one feature let the application boot and are reported as a warning
 * naming that feature, so the developer can fix them one start at a time.</li>
 * <li>The JWT signing key is generated on the spot and the generated value is handed to the
 * developer to copy, because guessing a key is the one thing this check can do for them.</li>
 * </ul>
 */
@Component
public class StartupConfigurationCheck implements BeanFactoryPostProcessor, EnvironmentAware {

    private static final Logger logger = LoggerFactory.getLogger(StartupConfigurationCheck.class);

    private static final String GENERATED_JWT_KEY_SOURCE = "business-cockpit-generated-jwt-key";

    private ConfigurableEnvironment environment;

    @Override
    public void setEnvironment(
            final Environment environment) {

        this.environment = (ConfigurableEnvironment) environment;

    }

    @Override
    public void postProcessBeanFactory(
            final ConfigurableListableBeanFactory beanFactory) {

        checkConfigurationOf(environment);

    }

    /**
     * Throws {@link CockpitIsNotConfiguredException} if the application cannot run at all, and logs
     * one warning per feature that stays switched off.
     */
    public void checkConfigurationOf(
            final ConfigurableEnvironment environment) {

        failOnMissingMandatoryValues(environment);
        warnAboutTheBpmsApi(environment);
        generateAJwtKeyIfNoneIsConfigured(environment);
        warnAboutKafkaIngestion(environment);
        warnAboutTheRemainingValues(environment);

    }

    private void failOnMissingMandatoryValues(
            final ConfigurableEnvironment environment) {

        final var missing = new ArrayList<MissingConfiguration>();

        // the driver falls back to an unauthenticated localhost:27017 when nothing is configured,
        // which ends in a connection timeout rather than in a hint about the missing URI
        if (CockpitConfiguration.isMissing(environment, MONGODB_URI)
                && CockpitConfiguration.isMissing(environment, MONGODB_HOST)) {
            missing.add(new MissingConfiguration(
                    MONGODB_URI,
                    "The MongoDB the cockpit stores user tasks, workflows and modules in. It has to "
                            + "be a replica set: the user interface updates from its change stream.",
                    "spring.mongodb.uri: mongodb://localhost:27017/business-cockpit?directConnection=true"));
        }

        if (CockpitConfiguration.isMissing(environment, TITLE_SHORT)) {
            missing.add(new MissingConfiguration(
                    TITLE_SHORT,
                    "The short application title. It names the application in the user interface and "
                            + "is the HTTP basic realm users log in to.",
                    "business-cockpit.title-short: MyCockpit"));
        }

        // Kafka ingestion is optional, but once topics are given the application is meant to consume
        // them, and it cannot without these two
        if (isKafkaIngestionFullyConfigured(environment)) {
            if (CockpitConfiguration.isWorkerIdMissing(environment)) {
                missing.add(new MissingConfiguration(
                        WORKER_ID,
                        "Tells the instances sharing a Kafka consumer group apart, which the BPMS "
                                + "API's Kafka ingestion needs to consume every event once.",
                        "start parameter '-DworkerId=local', or 'workerId: local' in the configuration"));
            }
            if (CockpitConfiguration.isMissing(environment, KAFKA_GROUP_ID_SUFFIX)) {
                missing.add(new MissingConfiguration(
                        KAFKA_GROUP_ID_SUFFIX,
                        "Identifies this application in the Kafka consumer group ids of the cockpit. "
                                + "Do not mix it up with 'workerId', which identifies the instance.",
                        "bpms-api.kafka.group-id-suffix: ${spring.application.name}"));
            }
        }

        if (!missing.isEmpty()) {
            throw new CockpitIsNotConfiguredException(List.copyOf(missing));
        }

    }

    private void warnAboutTheBpmsApi(
            final Environment environment) {

        if (CockpitConfiguration.isBpmsApiConfigured(environment)) {
            return;
        }

        final var missing = new ArrayList<MissingConfiguration>();
        if (CockpitConfiguration.isMissing(environment, BPMS_API_REALM_NAME)) {
            missing.add(new MissingConfiguration(
                    BPMS_API_REALM_NAME,
                    "The HTTP basic realm the BPMS API asks its clients to authenticate in.",
                    "bpms-api.realm-name: BPMS-API"));
        }
        if (CockpitConfiguration.isMissing(environment, BPMS_API_USERNAME)) {
            missing.add(new MissingConfiguration(
                    BPMS_API_USERNAME,
                    "The user name BPMS adapters authenticate at the BPMS API with.",
                    "bpms-api.username: bpms"));
        }
        if (CockpitConfiguration.isMissing(environment, BPMS_API_PASSWORD)) {
            missing.add(new MissingConfiguration(
                    BPMS_API_PASSWORD,
                    "The password of that user, prefixed by the encoding it is stored in.",
                    "bpms-api.password: \"{bcrypt}$2a$10$...\", or \"{noop}secret\" for local development"));
        }

        logger.warn("""
                The BPMS API is deactivated because it is not configured. Requests to '/bpms/api/**' \
                are rejected, so no workflow module and no BPMS adapter can report user tasks or \
                workflows to this cockpit. Missing:

                {}

                Add these values to activate the BPMS API on the next start.""",
                MissingConfiguration.asMessageBlocks(missing));

    }

    /**
     * A cockpit without a signing key could not hand out a single login cookie, so a key is made up
     * rather than refusing to start. It is only good for this run, which is what the warning is
     * about.
     */
    private void generateAJwtKeyIfNoneIsConfigured(
            final ConfigurableEnvironment environment) {

        if (!CockpitConfiguration.isMissing(environment, JWT_KEY)) {
            return;
        }

        final var generatedKey = generateJwtKey();
        // ahead of every other source: the key counts as unconfigured when it is set to nothing,
        // and a source holding an empty value would otherwise keep winning
        environment
                .getPropertySources()
                .addFirst(new MapPropertySource(GENERATED_JWT_KEY_SOURCE, Map.of(JWT_KEY, generatedKey)));

        logger.warn("""
                No JWT signing key is configured, so one was generated for this start. Every restart \
                and every further instance of this application signs with a different key, which \
                logs all users out. Missing:

                {}""",
                new MissingConfiguration(
                        JWT_KEY,
                        "The Base64 encoded HMAC-SHA256 key the login cookie is signed with.",
                        JWT_KEY + ": " + generatedKey).asMessageBlock());

    }

    private static String generateJwtKey() {

        try {
            return Base64
                    .getEncoder()
                    .encodeToString(KeyGenerator
                            .getInstance("HmacSha256")
                            .generateKey()
                            .getEncoded());
        } catch (Exception e) {
            throw new IllegalStateException(
                    "No property '" + JWT_KEY + "' is set and generating a key to run with failed.", e);
        }

    }

    private boolean isKafkaIngestionFullyConfigured(
            final Environment environment) {

        return missingKafkaTopics(environment).isEmpty();

    }

    private List<MissingConfiguration> missingKafkaTopics(
            final Environment environment) {

        final var missing = new ArrayList<MissingConfiguration>();
        if (CockpitConfiguration.isMissing(environment, KAFKA_TOPIC_USER_TASK)) {
            missing.add(new MissingConfiguration(
                    KAFKA_TOPIC_USER_TASK,
                    "The topic user task events are consumed from.",
                    "bpms-api.kafka.topics.user-task: business-cockpit-user-tasks"));
        }
        if (CockpitConfiguration.isMissing(environment, KAFKA_TOPIC_WORKFLOW)) {
            missing.add(new MissingConfiguration(
                    KAFKA_TOPIC_WORKFLOW,
                    "The topic workflow events are consumed from.",
                    "bpms-api.kafka.topics.workflow: business-cockpit-workflows"));
        }
        if (CockpitConfiguration.isMissing(environment, KAFKA_TOPIC_WORKFLOW_MODULE)) {
            missing.add(new MissingConfiguration(
                    KAFKA_TOPIC_WORKFLOW_MODULE,
                    "The topic workflow module registrations are consumed from.",
                    "bpms-api.kafka.topics.workflow-module: business-cockpit-workflow-modules"));
        }
        return missing;

    }

    /**
     * Kafka ingestion is switched on by the three topics together. Configuring only some of them
     * leaves it off, which is worth a word because it looks configured.
     */
    private void warnAboutKafkaIngestion(
            final Environment environment) {

        final var missingTopics = missingKafkaTopics(environment);
        if (missingTopics.isEmpty() || missingTopics.size() == 3) {
            return;
        }

        logger.warn("""
                The BPMS API's Kafka ingestion is deactivated because it is configured only in part. \
                It consumes all three topics or none. Missing:

                {}""",
                MissingConfiguration.asMessageBlocks(missingTopics));

    }

    private void warnAboutTheRemainingValues(
            final Environment environment) {

        final var missing = new ArrayList<MissingConfiguration>();

        if (CockpitConfiguration.isMissing(environment, TITLE_LONG)) {
            missing.add(new MissingConfiguration(
                    TITLE_LONG,
                    "The full application title, shown on the login screen and in the browser tab. "
                            + "Without it the user interface shows no title at all.",
                    "business-cockpit.title-long: My Business Cockpit"));
        }
        if (CockpitConfiguration.isMissing(environment, APPLICATION_VERSION)) {
            missing.add(new MissingConfiguration(
                    APPLICATION_VERSION,
                    "The version the user interface displays, usually filled by the build.",
                    "business-cockpit.application-version: \"@project.version@\""));
        }

        // both only matter once notification e-mails are actually sent
        if (isSmtpNotificationEnabled(environment)) {
            if (CockpitConfiguration.isMissing(environment, APPLICATION_URI)) {
                missing.add(new MissingConfiguration(
                        APPLICATION_URI,
                        "The URI this cockpit is reachable at. Notification e-mails link to their "
                                + "user task with it, and the links stay broken without it.",
                        "business-cockpit.application-uri: https://cockpit.example.com"));
            }
            if (CockpitConfiguration.isMissing(environment, SMTP_FROM)) {
                missing.add(new MissingConfiguration(
                        SMTP_FROM,
                        "The From address of notification e-mails. Many SMTP servers reject messages "
                                + "without one.",
                        "business-cockpit.notification.smtp.from: cockpit@example.com"));
            }
        }

        if (missing.isEmpty()) {
            return;
        }

        logger.warn("""
                The Business Cockpit is running with an incomplete configuration:

                {}""",
                MissingConfiguration.asMessageBlocks(missing));

    }

    private boolean isSmtpNotificationEnabled(
            final Environment environment) {

        return CockpitConfiguration
                .valueOf(environment, SMTP_ENABLED)
                .map(Boolean::parseBoolean)
                .orElse(Boolean.FALSE);

    }

}
