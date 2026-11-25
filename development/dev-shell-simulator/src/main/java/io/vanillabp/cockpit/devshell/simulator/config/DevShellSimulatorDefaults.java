package io.vanillabp.cockpit.devshell.simulator.config;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.DefaultPropertiesPropertySource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.Ordered;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
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
 * This includes the values an application passes to
 * {@link org.springframework.boot.builder.SpringApplicationBuilder#properties(String...)}. Spring
 * Boot keeps those as {@value DefaultPropertiesPropertySource#NAME} and moves them to the end of all
 * property sources after every post-processor has run. A source added behind them would end up in
 * front of them again. So the defaults of the simulator join that source, behind the values of the
 * builder.
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
      final var propertySources = environment.getPropertySources();
      new YamlPropertySourceLoader()
          .load(DEFAULTS, new ClassPathResource(DEFAULTS, DevShellSimulatorDefaults.class.getClassLoader()))
          .forEach(defaults -> addBehindTheBuilderProperties(propertySources, defaults));
    } catch (IOException e) {
      throw new UncheckedIOException("The defaults of the simulator in "
          + DEFAULTS
          + " cannot be read", e);
    }

  }

  private static void addBehindTheBuilderProperties(
      final MutablePropertySources propertySources,
      final PropertySource<?> defaults) {

    final var builderProperties = propertySources.get(DefaultPropertiesPropertySource.NAME);
    if (builderProperties == null) {
      propertySources.addLast(defaults);
      return;
    }

    // The name stays the same, so Spring Boot still moves the source to the end.
    final var builderPropertiesFirst = new CompositePropertySource(DefaultPropertiesPropertySource.NAME);
    builderPropertiesFirst.addPropertySource(builderProperties);
    builderPropertiesFirst.addPropertySource(defaults);
    propertySources.replace(DefaultPropertiesPropertySource.NAME, builderPropertiesFirst);

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
