package io.vanillabp.cockpit.support.rest.httpservice;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Prints the problems of {@link HttpServiceClientsMisconfigured} as the reason why the application
 * did not start, without the stack trace in front of them. Registered in
 * {@code META-INF/spring.factories}, which is where Spring Boot looks for failure analyzers.
 */
public class HttpServiceClientsMisconfiguredFailureAnalyzer extends AbstractFailureAnalyzer<HttpServiceClientsMisconfigured> {

  /**
   * Creates the analyzer. Spring Boot creates it from {@code META-INF/spring.factories}.
   */
  public HttpServiceClientsMisconfiguredFailureAnalyzer() {
  }

  @Override
  protected FailureAnalysis analyze(
      final Throwable rootFailure,
      final HttpServiceClientsMisconfigured cause) {

    return new FailureAnalysis(
        cause.getMessage(), """
            Change the keys named above and start again. The keys of a group live below its prefix: \
            the one its HttpServiceGroupPrefix bean names, or 'spring.http.serviceclient.<group>' \
            for a group without one.""", cause);

  }

}
