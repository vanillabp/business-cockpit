package io.vanillabp.cockpit.support.rest.restclient;

import java.util.List;

/**
 * The keys of REST clients cannot work: a missing or broken address, an incomplete proxy, an OAuth
 * client without credentials, a key nobody reads. The message lists every problem of the prefix,
 * so the developer sees all of them after one start.
 */
public class RestClientsMisconfigured extends IllegalStateException {

  private static final long serialVersionUID = 1L;

  /** The problems, each one a sentence which says what to do. */
  private final List<String> problems;

  /**
   * Lists the problems.
   *
   * @param prefix   the key all keys of the clients start with
   * @param problems what is wrong, each one a sentence which says what to do
   */
  public RestClientsMisconfigured(
      final String prefix,
      final List<String> problems) {

    super("""
        The REST clients at '%s' cannot be built, so the application does not start:
        %s
        The keys of REST clients and their defaults are listed in the support README."""
        .formatted(prefix, String.join("\n", problems.stream().map(problem -> "  - "
            + problem).toList())));
    this.problems = List.copyOf(problems);

  }

  /**
   * What is wrong.
   *
   * @return the problems, each one a sentence which says what to do
   */
  public List<String> getProblems() {
    return problems;
  }

}
