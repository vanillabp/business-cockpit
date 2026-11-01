package io.vanillabp.cockpit.config.startup;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Turns a missing user details provider into Spring Boot's "APPLICATION FAILED TO START" report, the
 * same way {@link CockpitIsNotConfiguredFailureAnalyzer} does it for a missing value. The first thing
 * a developer sees is the bean to write, and not a parameter of a library bean they never wrote.
 * <p>
 * Registered in {@code META-INF/spring.factories}, which is where Spring Boot looks for failure
 * analyzers.
 */
public class UserDetailsProviderIsMissingFailureAnalyzer extends AbstractFailureAnalyzer<UserDetailsProviderIsMissingException> {

  /**
   * Creates the analyzer. Spring Boot creates it from {@code META-INF/spring.factories}.
   */
  public UserDetailsProviderIsMissingFailureAnalyzer() {
  }

  @Override
  protected FailureAnalysis analyze(
      final Throwable rootFailure,
      final UserDetailsProviderIsMissingException cause) {

    return new FailureAnalysis(
        cause.describeWhatIsWrong(), cause.describeWhatToDo(), cause);

  }

}
