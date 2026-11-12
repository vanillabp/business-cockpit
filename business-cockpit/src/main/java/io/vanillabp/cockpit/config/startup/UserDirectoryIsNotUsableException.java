package io.vanillabp.cockpit.config.startup;

import java.util.List;

/**
 * Raised before the first bean is built, when the application has no user directory, has two of
 * them, or has the one the cockpit brings with settings which cannot work. It carries the
 * description and the remedy as plain text, so {@link UserDirectoryIsNotUsableFailureAnalyzer} can
 * show them as Spring Boot's failure report instead of a stack trace.
 * <p>
 * Each of these stops the start. The directory is asked on every request of a logged-in user, so
 * without this check the first login would fail instead of the start, or the start would fail with
 * a message about a parameter of a bean the developer never wrote.
 */
public class UserDirectoryIsNotUsableException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private static final String WHAT_TO_DO_WITH_THE_SETTINGS = "Set or correct the values above. Or leave '"
      + UserDirectorySettings.TYPE
      + "' out and add a bean of type 'UserDirectoryService' of your own.";

  /**
   * Which values cannot be used and why, for the description of the failure report.
   */
  private final String whatIsWrong;

  /**
   * What the developer has to do, for the action of the failure report.
   */
  private final String whatToDo;

  /**
   * For settings of the directory the cockpit brings which cannot work.
   */
  UserDirectoryIsNotUsableException(
      final List<MissingConfiguration> unusable) {

    this("The Business Cockpit cannot use the user directory '"
        + UserDirectorySettings.TYPE
        + "' asks for:\n\n"
        + MissingConfiguration.asMessageBlocks(unusable), WHAT_TO_DO_WITH_THE_SETTINGS);

  }

  UserDirectoryIsNotUsableException(
      final String whatIsWrong,
      final String whatToDo) {

    super(whatIsWrong
        + "\n\n"
        + whatToDo);
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
