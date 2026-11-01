package io.vanillabp.cockpit.config.startup;

/**
 * Raised before the first bean is built, when the application has no user details provider. The
 * library brings none since the module {@code container} holds the login. It carries the description and the remedy as plain text,
 * so {@link UserDetailsProviderIsMissingFailureAnalyzer} can show them as Spring Boot's failure report
 * instead of a stack trace.
 */
public class UserDetailsProviderIsMissingException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Which value cannot be used and why, for the description of the failure report.
   */
  private final String whatIsWrong;

  /**
   * What the developer has to do, for the action of the failure report.
   */
  private final String whatToDo;

  UserDetailsProviderIsMissingException(
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
