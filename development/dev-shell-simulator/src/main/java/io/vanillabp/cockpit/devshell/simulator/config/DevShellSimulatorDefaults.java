package io.vanillabp.cockpit.devshell.simulator.config;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;

import io.vanillabp.cockpit.devshell.simulator.DevShellSimulator;

/**
 * Adds the defaults of the simulator, {@value #DEFAULTS}, as the configuration with the lowest
 * priority.
 * <p>
 * The defaults are not in an {@code application.yaml}. An application which extends
 * {@link DevShellSimulator} has an {@code application.yaml} of its own, and Spring Boot reads only
 * the first file of that name on the class path. So the application lost the data source, the
 * schema update and the key of the login token, and had to repeat them. With a file of their own
 * the defaults always count, and every value the application configures wins over them.
 * <p>
 * The defaults are added only to an application which is the simulator or extends it. Another
 * application which has the jar on its class path keeps its configuration as it is.
 */
public class DevShellSimulatorDefaults implements EnvironmentPostProcessor, Ordered {

  /** The file on the class path which holds the defaults. */
  public static final String DEFAULTS = "dev-shell-simulator-defaults.yaml";

  /**
   * Creates the post-processor. Spring Boot creates it, nobody else does.
   */
  public DevShellSimulatorDefaults() {
  }

  @Override
  public void postProcessEnvironment(
      final ConfigurableEnvironment environment,
      final SpringApplication application) {

    final var isTheSimulator = application
        .getAllSources()
        .stream()
        .anyMatch(source -> (source instanceof Class<?> type) && DevShellSimulator.class.isAssignableFrom(type));
    if (!isTheSimulator) {
      return;
    }

    try {
      new YamlPropertySourceLoader()
          .load(DEFAULTS, new ClassPathResource(DEFAULTS, DevShellSimulatorDefaults.class.getClassLoader()))
          .forEach(environment.getPropertySources()::addLast);
    } catch (IOException e) {
      throw new UncheckedIOException("The defaults of the simulator in "
          + DEFAULTS
          + " cannot be read", e);
    }

  }

  /**
   * Runs after Spring Boot has read the configuration files of the application. Otherwise those
   * files would be added after the defaults, and the defaults would win over them.
   */
  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }

}
