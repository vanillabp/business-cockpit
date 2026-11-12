package io.vanillabp.cockpit.config.startup;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Turns a user directory which cannot work into Spring Boot's "APPLICATION FAILED TO START"
 * report, the same way {@link CockpitIsNotConfiguredFailureAnalyzer} does it for a missing value.
 * <p>
 * Registered in {@code META-INF/spring.factories}, which is where Spring Boot looks for failure
 * analyzers.
 */
public class UserDirectoryIsNotUsableFailureAnalyzer extends AbstractFailureAnalyzer<UserDirectoryIsNotUsableException> {

  /**
   * Creates the analyzer. Spring Boot creates it from {@code META-INF/spring.factories}.
   */
  public UserDirectoryIsNotUsableFailureAnalyzer() {
  }

  @Override
  protected FailureAnalysis analyze(
      final Throwable rootFailure,
      final UserDirectoryIsNotUsableException cause) {

    return new FailureAnalysis(
        cause.describeWhatIsWrong(), cause.describeWhatToDo(), cause);

  }

}
