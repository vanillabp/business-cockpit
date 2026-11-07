package io.vanillabp.cockpit.config.startup;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Turns a removed key which is still set into Spring Boot's "APPLICATION FAILED TO START" report,
 * the same way {@link CockpitIsNotConfiguredFailureAnalyzer} does it for a missing value.
 * <p>
 * Registered in {@code META-INF/spring.factories}, which is where Spring Boot looks for failure
 * analyzers.
 */
public class RemovedPropertyIsSetFailureAnalyzer extends AbstractFailureAnalyzer<RemovedPropertyIsSetException> {

  /**
   * Creates the analyzer. Spring Boot creates it from {@code META-INF/spring.factories}.
   */
  public RemovedPropertyIsSetFailureAnalyzer() {
  }

  @Override
  protected FailureAnalysis analyze(
      final Throwable rootFailure,
      final RemovedPropertyIsSetException cause) {

    return new FailureAnalysis(
        cause.describeWhatIsWrong(), cause.describeWhatToDo(), cause);

  }

}
