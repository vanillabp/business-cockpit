package io.vanillabp.cockpit.config.startup;

import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.MONGODB_MAP_KEY_DOT_REPLACEMENT;

import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoConverter;

/**
 * What the cockpit writes instead of a dot in a key of the business data of a user task or a
 * workflow.
 * <p>
 * The business data, {@code details}, is a map the workflow module fills. MongoDB reads a dot in
 * a path as a step into a nested document, so Spring Data refuses a key like {@code order.id}
 * unless somebody tells it what to write instead. By default the cockpit tells it nothing, and a
 * report with such a key is given up (see decision 41 in the repository's DECISIONS.md).
 * <p>
 * With {@value CockpitConfiguration#MONGODB_MAP_KEY_DOT_REPLACEMENT} set, Spring Data writes the
 * replacement instead of each dot and turns it back into a dot when it reads the record. That has
 * two costs, and every text which offers the property names them:
 * <ul>
 * <li>Search and sorting see what is stored. A column or a filter has to name the key in its
 * stored form, like {@code details.order~id}. A path with the dot finds nothing.</li>
 * <li>The way back is not exact. A key which holds the replacement already comes back with a dot
 * in its place.</li>
 * </ul>
 */
public final class MapKeyDotReplacement {

    private static final Logger logger = LoggerFactory.getLogger(MapKeyDotReplacement.class);

    /** The value every text suggests. A tilde is rare in a key, and MongoDB takes it anywhere. */
    public static final String EXAMPLE = "~";

    /** The words Spring Data uses when it refuses a key with a dot, and the key it names. */
    private static final Pattern REFUSED_KEY = Pattern.compile("Map key (.+?) contains dots");

    private MapKeyDotReplacement() {
    }

    /**
     * Reads the replacement from the configuration.
     *
     * @param environment The configuration of the application
     * @return What is configured, or empty where the property is not set at all. An empty string
     *         is returned as it is, because it is a value somebody wrote and {@link #whatIsWrongWith}
     *         refuses it
     */
    public static Optional<String> configuredIn(
            final Environment environment) {

        return Binder
                .get(environment)
                .bind(ConfigurationPropertyName.adapt(MONGODB_MAP_KEY_DOT_REPLACEMENT, '.'), Bindable.of(String.class))
                .map(Optional::of)
                .orElseGet(() -> Optional.ofNullable(environment.getProperty(MONGODB_MAP_KEY_DOT_REPLACEMENT)));

    }

    /**
     * Says whether a configured value can work as the replacement of a dot.
     *
     * @param replacement A configured value
     * @return Why the value cannot be used, or empty if it can
     */
    public static Optional<String> whatIsWrongWith(
            final String replacement) {

        if (replacement.isBlank()) {
            return Optional.of("It is empty or holds nothing but spaces. Every dot would be dropped or turned into "
                    + "spaces, and 'order.id' would not come back as it was reported.");
        }
        if (replacement.contains(".")) {
            return Optional.of("It contains a dot. The stored key would have a dot again, and MongoDB would read it as a path.");
        }
        if (replacement.contains("$")) {
            return Optional.of("It contains a '$'. MongoDB reads a field name which starts with '$' as an operator.");
        }
        if (replacement.indexOf('\0') >= 0) {
            return Optional.of("It contains the character NUL, which MongoDB does not take in a field name.");
        }
        return Optional.empty();

    }

    /**
     * The block a message ends with: the property, what it does and costs, and a line to copy. It
     * is the layout {@code StartupConfigurationCheck} uses for a missing value.
     */
    static String whatToSet() {

        return "  " + MONGODB_MAP_KEY_DOT_REPLACEMENT + "\n"
                + "      What the cockpit writes instead of a dot in a key of the business data. "
                + "Pick a string no key of your business data contains. "
                + costs() + " Leave the property out to give up every report with such a key.\n"
                + "      Example: " + MONGODB_MAP_KEY_DOT_REPLACEMENT + ": \"" + EXAMPLE + "\"";

    }

    /**
     * Says what the property costs.
     *
     * @return What the property costs, in two sentences, for every text which offers it
     */
    public static String costs() {

        return "Search and sorting then find such a key only by its stored form, like 'order"
                + EXAMPLE + "id', and a key which holds the replacement already comes back with a dot "
                + "in its place.";

    }

    /**
     * Builds the hint which tells the sender of a refused report how such a key can be stored.
     *
     * @param message The message of the exception Spring Data threw while it mapped a record
     * @return A hint which names the property and what it costs, or empty if the message is not
     *         about a key with a dot
     */
    public static Optional<String> hintFor(
            final String message) {

        if (message == null) {
            return Optional.empty();
        }
        final var refused = REFUSED_KEY.matcher(message);
        if (!refused.find()) {
            return Optional.empty();
        }
        final var key = refused.group(1);
        return Optional.of("To store a key like '%s', set '%s' in the cockpit, for example to '%s'. "
                .formatted(key, MONGODB_MAP_KEY_DOT_REPLACEMENT, EXAMPLE)
                + "Search and sorting then find the key only by its stored form, '%s', "
                        .formatted(key.replace(".", EXAMPLE))
                + "and a key which holds the replacement already comes back with a dot in its place.");

    }

    /**
     * Hands the configured replacement to the converter, which writes and reads every record of
     * the cockpit. The value was checked at the start by {@code StartupConfigurationCheck}.
     *
     * @param environment The configuration of the application
     * @param converter The converter of the cockpit's MongoTemplate
     */
    public static void applyTo(
            final Environment environment,
            final MongoConverter converter) {

        final var replacement = configuredIn(environment);
        if (replacement.isEmpty()) {
            return;
        }
        if (converter instanceof MappingMongoConverter mapping) {
            mapping.setMapKeyDotReplacement(replacement.get());
            logger.info(
                    "Writing '{}' instead of a dot in a key of the business data, as '{}' says. {}",
                    replacement.get(),
                    MONGODB_MAP_KEY_DOT_REPLACEMENT,
                    costs());
            return;
        }
        logger.warn(
                "'{}' is set, but the MongoDB converter of this application is a {}, which cannot "
                        + "replace a dot. A report whose business data has a key with a dot is given up.",
                MONGODB_MAP_KEY_DOT_REPLACEMENT,
                converter.getClass().getName());

    }

}
