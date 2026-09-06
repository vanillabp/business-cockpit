package io.vanillabp.cockpit.config.startup;

import java.util.Optional;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * The names of the configuration values a cockpit application has to provide, and a way to read
 * them straight from the environment.
 * <p>
 * Reading is deliberately not done through the {@code @ConfigurationProperties} beans: the startup
 * check runs before any bean is instantiated, and {@link BpmsApiIsConfigured} runs even earlier,
 * while configuration classes are still being parsed. Binding by
 * {@link ConfigurationPropertyName#adapt(CharSequence, char) adapted} name keeps Spring's relaxed
 * matching, so a value written as {@code hmacSHA256-base64} in yaml is found under exactly the name
 * spelled here - and under every other spelling Spring would accept for it.
 */
public final class CockpitConfiguration {

    /**
     * Where the database lives. Spring Boot 4 moved these keys out of {@code spring.data.mongodb}.
     */
    public static final String MONGODB_URI = "spring.mongodb.uri";

    public static final String MONGODB_HOST = "spring.mongodb.host";

    public static final String TITLE_SHORT = "business-cockpit.title-short";

    public static final String TITLE_LONG = "business-cockpit.title-long";

    public static final String APPLICATION_VERSION = "business-cockpit.application-version";

    public static final String APPLICATION_URI = "business-cockpit.application-uri";

    public static final String JWT_KEY = "business-cockpit.jwt.hmacSHA256-base64";

    public static final String BPMS_API_REALM_NAME = "bpms-api.realm-name";

    public static final String BPMS_API_USERNAME = "bpms-api.username";

    public static final String BPMS_API_PASSWORD = "bpms-api.password";

    public static final String KAFKA_TOPIC_USER_TASK = "bpms-api.kafka.topics.user-task";

    public static final String KAFKA_TOPIC_WORKFLOW = "bpms-api.kafka.topics.workflow";

    public static final String KAFKA_TOPIC_WORKFLOW_MODULE = "bpms-api.kafka.topics.workflow-module";

    public static final String KAFKA_GROUP_ID_SUFFIX = "bpms-api.kafka.group-id-suffix";

    /**
     * Not a relaxed-bound property but a plain placeholder read by {@code @Value}, so it is spelled
     * in camel case and has to be looked up exactly like that.
     */
    public static final String WORKER_ID = "workerId";

    public static final String SMTP_ENABLED = "business-cockpit.notification.smtp.enabled";

    public static final String SMTP_FROM = "business-cockpit.notification.smtp.from";

    private CockpitConfiguration() {
    }

    /**
     * The configured value, or empty when the property is absent or holds nothing but whitespace.
     * A property set to an empty string is treated as unset on purpose: an empty realm name or an
     * empty password is never what somebody meant to configure.
     */
    public static Optional<String> valueOf(
            final Environment environment,
            final String propertyName) {

        final var value = Binder
                .get(environment)
                .bind(ConfigurationPropertyName.adapt(propertyName, '.'), Bindable.of(String.class))
                .orElse(null);
        return StringUtils.hasText(value) ? Optional.of(value) : Optional.empty();

    }

    public static boolean isMissing(
            final Environment environment,
            final String propertyName) {

        return valueOf(environment, propertyName).isEmpty();

    }

    /**
     * Whether the BPMS API can be served: it authenticates every request by HTTP basic, which needs
     * all three of realm, user and password.
     */
    public static boolean isBpmsApiConfigured(
            final Environment environment) {

        return !isMissing(environment, BPMS_API_REALM_NAME)
                && !isMissing(environment, BPMS_API_USERNAME)
                && !isMissing(environment, BPMS_API_PASSWORD);

    }

    /**
     * The worker id is read as a plain placeholder, so relaxed binding does not apply to it.
     */
    public static boolean isWorkerIdMissing(
            final Environment environment) {

        return !StringUtils.hasText(environment.getProperty(WORKER_ID));

    }

}
