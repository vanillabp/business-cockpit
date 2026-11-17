package io.vanillabp.cockpit.support.rest.httpservice;

import java.util.List;

/**
 * Stops the start because the keys of one or more HTTP service groups are incomplete or wrong. It
 * lists every problem found, so one restart is enough to see all of them.
 * {@link HttpServiceClientsMisconfiguredFailureAnalyzer} prints the list on its own, without the
 * stack trace.
 */
public class HttpServiceClientsMisconfigured extends IllegalStateException {

  private static final long serialVersionUID = 1L;

  /** The problems, one entry each. */
  private final List<String> problems;

  /**
   * Creates the exception.
   *
   * @param problems one entry per problem, each naming the key to set or to change
   */
  public HttpServiceClientsMisconfigured(
      final List<String> problems) {

    super("""
        The keys of the HTTP service clients need a change:
        - %s"""
        .formatted(String.join("\n- ", problems)));
    this.problems = List.copyOf(problems);

  }

  /**
   * The problems found.
   *
   * @return one sentence or two per problem, each naming the key to set or to change
   */
  public List<String> getProblems() {
    return problems;
  }

}
