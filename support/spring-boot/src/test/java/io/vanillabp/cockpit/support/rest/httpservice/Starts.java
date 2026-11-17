package io.vanillabp.cockpit.support.rest.httpservice;

import java.util.List;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

/** Starts an application which is expected not to start, and answers why. */
final class Starts {

  private Starts() {
  }

  /**
   * The problems the conventions found while the application started.
   *
   * @param configurations the test application and the configurations with the groups
   * @param properties     the keys, as {@code key=value}
   */
  static List<String> problemsOf(
      final Class<?>[] configurations,
      final String... properties) {

    try (var context = new SpringApplicationBuilder(configurations)
        .web(WebApplicationType.NONE)
        .properties(properties)
        .run()) {
      throw new AssertionError("The application started, but it should have stopped");
    } catch (AssertionError e) {
      throw e;
    } catch (RuntimeException e) {
      for (Throwable cause = e; cause != null; cause = cause.getCause()) {
        if (cause instanceof HttpServiceClientsMisconfigured misconfigured) {
          return misconfigured.getProblems();
        }
      }
      throw new AssertionError("The application stopped for another reason", e);
    }

  }

}
