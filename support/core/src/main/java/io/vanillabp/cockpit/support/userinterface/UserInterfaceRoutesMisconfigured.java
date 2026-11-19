package io.vanillabp.cockpit.support.userinterface;

import java.util.List;

/**
 * Stops the start because one or more keys of the routes to the user interfaces are wrong. It lists
 * every problem found, so one restart is enough to see all of them.
 */
public class UserInterfaceRoutesMisconfigured extends IllegalStateException {

  private static final long serialVersionUID = 1L;

  /** The problems, one entry each. */
  private final List<String> problems;

  /**
   * Creates the exception.
   *
   * @param problems one entry per problem, each naming the key to set or to change
   */
  public UserInterfaceRoutesMisconfigured(
      final List<String> problems) {

    super("""
        The routes to the user interfaces of the workflow modules need a change:
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
