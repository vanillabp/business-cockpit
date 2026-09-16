package io.vanillabp.cockpit.config.startup;

/**
 * Raised while the MongoDB template is being built, when the write concern it would write with does
 * not wait for a single acknowledgement. It carries the description and the remedy as plain text,
 * so {@link WritesAreNotAcknowledgedFailureAnalyzer} can show them as Spring Boot's failure report
 * instead of a stack trace.
 */
public class WritesAreNotAcknowledgedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String whatIsWrong;

    private final String whatToDo;

    WritesAreNotAcknowledgedException(
            final String whatIsWrong,
            final String whatToDo) {

        super(whatIsWrong + "\n\n" + whatToDo);
        this.whatIsWrong = whatIsWrong;
        this.whatToDo = whatToDo;

    }

    String describeWhatIsWrong() {
        return whatIsWrong;
    }

    String describeWhatToDo() {
        return whatToDo;
    }

}
