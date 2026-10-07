package io.vanillabp.cockpit.config.startup;

import static io.vanillabp.cockpit.config.startup.CockpitConfiguration.MONGODB_SORT_INDEXES_PER_COLLECTION;

import java.util.Optional;
import org.springframework.core.env.Environment;

/**
 * How many indexes for sorting a list the cockpit creates per collection at most. The lists of user
 * tasks and of workflows are sorted by paths a client names, and each new combination of paths gets
 * an index of its own until this limit is reached. After that a new combination is sorted without
 * an index, and the cockpit warns once per combination.
 * <p>
 * MongoDB takes 64 indexes per collection, and the cockpit needs a few of them for itself. So the
 * limit is a number from 0 to {@value #HIGHEST}. 0 means: never create an index for sorting.
 */
public final class SortIndexLimit {

    public static final int DEFAULT = 30;

    public static final int HIGHEST = 60;

    private SortIndexLimit() {
    }

    /**
     * @param environment The configuration of the application
     * @return The configured limit, or {@link #DEFAULT} if none is configured. A value
     *         {@link #whatIsWrongWith(Environment)} refuses stops the start before this is asked.
     */
    public static int configuredIn(
            final Environment environment) {

        return CockpitConfiguration
                .valueOf(environment, MONGODB_SORT_INDEXES_PER_COLLECTION)
                .map(value -> Integer.parseInt(value.trim()))
                .orElse(DEFAULT);

    }

    /**
     * @param environment The configuration of the application
     * @return Why the configured value cannot be used, or empty if it can or none is configured
     */
    public static Optional<String> whatIsWrongWith(
            final Environment environment) {

        final var configured = CockpitConfiguration.valueOf(environment, MONGODB_SORT_INDEXES_PER_COLLECTION);
        if (configured.isEmpty()) {
            return Optional.empty();
        }
        final int limit;
        try {
            limit = Integer.parseInt(configured.get().trim());
        } catch (NumberFormatException e) {
            return Optional.of("The value '%s' is no whole number.".formatted(configured.get()));
        }
        if ((limit < 0) || (limit > HIGHEST)) {
            return Optional.of(("The value '%s' is not between 0 and %d. MongoDB takes 64 indexes per "
                    + "collection, and the cockpit needs a few of them for itself.").formatted(limit, HIGHEST));
        }
        return Optional.empty();

    }

    /**
     * The block a message ends with: the property, what it does, and a line to copy.
     */
    static String whatToSet() {

        return new MissingConfiguration(
                MONGODB_SORT_INDEXES_PER_COLLECTION,
                "How many indexes for sorting a list the cockpit creates per collection at most, "
                        + "from 0 to " + HIGHEST + ". Leave it out to use " + DEFAULT + ".",
                MONGODB_SORT_INDEXES_PER_COLLECTION + ": " + DEFAULT).asMessageBlock();

    }

}
