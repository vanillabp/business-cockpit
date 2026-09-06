package io.vanillabp.cockpit.config.startup;

import java.util.List;

/**
 * One configuration value that is missing, described the way every startup message reports it: the
 * exact property name, what the cockpit needs it for, and a line to copy into the application's
 * configuration.
 */
record MissingConfiguration(
        String propertyName,
        String purpose,
        String example) {

    /**
     * The block every message is built from: the property name on its own line, what it is for and
     * the line to copy indented underneath it.
     */
    String asMessageBlock() {

        return "  " + propertyName + "\n"
                + "      " + purpose + "\n"
                + "      Example: " + example;

    }

    static String asMessageBlocks(
            final List<MissingConfiguration> missing) {

        return missing
                .stream()
                .map(MissingConfiguration::asMessageBlock)
                .reduce((left, right) -> left + "\n\n" + right)
                .orElse("");

    }

}
