package io.vanillabp.cockpit.config.startup;

/**
 * Raised while the application context is still being prepared, when the limit of indexes for
 * sorting cannot be used. It carries the description and the remedy as plain text, so
 * {@link SortIndexLimitIsNotUsableFailureAnalyzer} can show them as Spring Boot's failure report
 * instead of a stack trace.
 * <p>
 * Such a value stops the start. The limit is read only when a list is sorted by a new path, which
 * can be days after the start.
 */
public class SortIndexLimitIsNotUsableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private static final String WHAT_TO_DO =
            "Change the value, or leave the property out:\n\n" + SortIndexLimit.whatToSet();

    private final String whatIsWrong;

    SortIndexLimitIsNotUsableException(
            final String reason) {

        super(whatIsWrong(reason) + "\n\n" + WHAT_TO_DO);
        this.whatIsWrong = whatIsWrong(reason);

    }

    private static String whatIsWrong(
            final String reason) {

        return "The Business Cockpit cannot use the limit of indexes for sorting, '"
                + CockpitConfiguration.MONGODB_SORT_INDEXES_PER_COLLECTION + "'. " + reason;

    }

    String describeWhatIsWrong() {
        return whatIsWrong;
    }

    String describeWhatToDo() {
        return WHAT_TO_DO;
    }

}
