package io.vanillabp.cockpit.config.startup;

/**
 * Raised while the application context is still being prepared, when the string configured to
 * replace a dot in a key of the business data cannot be used. It carries the description and the
 * remedy as plain text, so {@link MapKeyDotReplacementIsNotUsableFailureAnalyzer} can show them as
 * Spring Boot's failure report instead of a stack trace.
 * <p>
 * Such a value stops the start rather than being left out. Somebody set it because reports with
 * such keys are to be stored, and a cockpit which ignored it would give those reports up without
 * a word at the start.
 */
public class MapKeyDotReplacementIsNotUsableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private static final String WHAT_TO_DO =
            "Change the value, or leave the property out:\n\n" + MapKeyDotReplacement.whatToSet();

    private final String whatIsWrong;

    private final String whatToDo;

    MapKeyDotReplacementIsNotUsableException(
            final String replacement,
            final String reason) {

        super(whatIsWrong(replacement, reason) + "\n\n" + WHAT_TO_DO);
        this.whatIsWrong = whatIsWrong(replacement, reason);
        this.whatToDo = WHAT_TO_DO;

    }

    private static String whatIsWrong(
            final String replacement,
            final String reason) {

        return "The Business Cockpit cannot use '" + replacement + "' as the replacement of a dot in a key of "
                + "the business data. " + reason;

    }

    String describeWhatIsWrong() {
        return whatIsWrong;
    }

    String describeWhatToDo() {
        return whatToDo;
    }

}
