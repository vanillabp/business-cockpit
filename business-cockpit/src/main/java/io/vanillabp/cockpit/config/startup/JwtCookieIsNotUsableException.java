package io.vanillabp.cockpit.config.startup;

import java.util.List;

/**
 * Raised while the application context is still being prepared, when the attributes of the login
 * cookie cannot work together. It carries the description and the remedy as plain text, so
 * {@link JwtCookieIsNotUsableFailureAnalyzer} can show them as Spring Boot's failure report instead
 * of a stack trace.
 * <p>
 * Such a cookie stops the start. A browser drops it, so nobody could log in, and nothing on the
 * server side would show why.
 */
public class JwtCookieIsNotUsableException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private static final String WHAT_TO_DO = "Change the values as the examples show, or leave "
      + "'business-cockpit.jwt.cookie.same-site' out to use its default 'lax'.";

  /**
   * Which value cannot be used and why, for the description of the failure report.
   */
  private final String whatIsWrong;

  JwtCookieIsNotUsableException(
      final List<MissingConfiguration> unusable) {

    super(whatIsWrong(unusable)
        + "\n\n"
        + WHAT_TO_DO);
    this.whatIsWrong = whatIsWrong(unusable);

  }

  private static String whatIsWrong(
      final List<MissingConfiguration> unusable) {

    return "A browser would drop the login cookie of the Business Cockpit:\n\n"
        + MissingConfiguration.asMessageBlocks(unusable);

  }

  String describeWhatIsWrong() {
    return whatIsWrong;
  }

  String describeWhatToDo() {
    return WHAT_TO_DO;
  }

}
