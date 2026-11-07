package io.vanillabp.cockpit.config.startup;

import java.util.List;

/**
 * Raised while the application context is still being prepared, when the configuration sets a
 * key the cockpit no longer binds. It carries the description and the remedy as plain text, so
 * {@link RemovedPropertyIsSetFailureAnalyzer} can show them as Spring Boot's failure report
 * instead of a stack trace.
 */
public class RemovedPropertyIsSetException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private static final String WHAT_TO_DO = "Remove these keys from the configuration. Setting "
      + "them changes nothing.";

  /**
   * Which keys are set and what became of them, for the description of the failure report.
   */
  private final String whatIsWrong;

  RemovedPropertyIsSetException(
      final List<String> removedKeys) {

    super(whatIsWrong(removedKeys)
        + "\n\n"
        + WHAT_TO_DO);
    this.whatIsWrong = whatIsWrong(removedKeys);

  }

  private static String whatIsWrong(
      final List<String> removedKeys) {

    return "The configuration sets keys the Business Cockpit no longer reads:\n\n  "
        + String.join("\n  ", removedKeys);

  }

  String describeWhatIsWrong() {
    return whatIsWrong;
  }

  String describeWhatToDo() {
    return WHAT_TO_DO;
  }

}
