package io.vanillabp.cockpit.support.userinterface;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Prints the problems of {@link UserInterfaceRoutesMisconfigured} as the reason why the application
 * did not start, without the stack trace in front of them. Registered in
 * {@code META-INF/spring.factories}, which is where Spring Boot looks for failure analyzers.
 */
public class UserInterfaceRoutesMisconfiguredFailureAnalyzer extends AbstractFailureAnalyzer<UserInterfaceRoutesMisconfigured> {

  /**
   * Creates the analyzer. Spring Boot creates it from {@code META-INF/spring.factories}.
   */
  public UserInterfaceRoutesMisconfiguredFailureAnalyzer() {
  }

  @Override
  protected FailureAnalysis analyze(
      final Throwable rootFailure,
      final UserInterfaceRoutesMisconfigured cause) {

    return new FailureAnalysis(
        cause.getMessage(), """
            Change the keys named above and start again. A workflow module gets a route to the \
            server of its user interface with the key '<id of the module>.user-interface.address', \
            for example 'loan-approval.user-interface.address=http://nginx:8080'.""", cause);

  }

}
