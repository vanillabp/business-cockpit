package io.vanillabp.cockpit.config.startup;

import java.util.List;

/**
 * Raised while the application context is still being prepared, when a lifetime of the login token
 * cannot be read as a duration. It carries the description and the remedy as plain text, so
 * {@link JwtLifetimeIsNotUsableFailureAnalyzer} can show them as Spring Boot's failure report
 * instead of a stack trace.
 * <p>
 * Such a value stops the start. Without it no login token can be built, so nobody could log in,
 * and before this check the only sign of it was an error in the log at the first login.
 */
public class JwtLifetimeIsNotUsableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private static final String WHAT_TO_DO = "Write each value as an ISO-8601 duration greater than zero, "
            + "for example 'PT12H' for twelve hours or 'P7D' for seven days, or leave the property out "
            + "to use its default.";

    private final String whatIsWrong;

    JwtLifetimeIsNotUsableException(
            final List<MissingConfiguration> unusable) {

        super(whatIsWrong(unusable) + "\n\n" + WHAT_TO_DO);
        this.whatIsWrong = whatIsWrong(unusable);

    }

    private static String whatIsWrong(
            final List<MissingConfiguration> unusable) {

        return "The Business Cockpit cannot use the lifetime of its login token:\n\n"
                + MissingConfiguration.asMessageBlocks(unusable);

    }

    String describeWhatIsWrong() {
        return whatIsWrong;
    }

    String describeWhatToDo() {
        return WHAT_TO_DO;
    }

}
