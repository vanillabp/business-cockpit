package io.vanillabp.cockpit.config.startup;

import java.util.List;

/**
 * Raised while the application context is still being prepared, listing every mandatory value that
 * is missing. It carries the description and the remedy as plain text so that
 * {@link CockpitIsNotConfiguredFailureAnalyzer} can present them as Spring Boot's failure report
 * instead of a stack trace.
 */
public class CockpitIsNotConfiguredException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private static final String REMEDY =
            "Add the values shown above to the application's configuration (e.g. 'application.yaml') "
            + "and start again.";

    private final String whatIsMissing;

    private final String whatToDo;

    CockpitIsNotConfiguredException(
            final List<MissingConfiguration> missing) {

        super("The Business Cockpit cannot start. It is missing " + countOf(missing) + ":\n\n"
                + MissingConfiguration.asMessageBlocks(missing)
                + "\n\n" + REMEDY);
        this.whatIsMissing = "The Business Cockpit is missing " + countOf(missing) + ":\n\n"
                + MissingConfiguration.asMessageBlocks(missing);
        this.whatToDo = REMEDY
                + " Every other setting the cockpit needs is reported as a warning once the "
                + "application boots.";

    }

    private static String countOf(
            final List<MissingConfiguration> missing) {

        return missing.size() == 1
                ? "one mandatory configuration value"
                : missing.size() + " mandatory configuration values";

    }

    String describeWhatIsMissing() {
        return whatIsMissing;
    }

    String describeWhatToDo() {
        return whatToDo;
    }

}
