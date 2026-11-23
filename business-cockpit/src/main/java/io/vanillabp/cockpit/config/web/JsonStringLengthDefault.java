package io.vanillabp.cockpit.config.web;

import java.util.Map;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.DefaultPropertiesPropertySource;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Gives the longest string Jackson reads from a JSON body a default of 10,000,000 characters,
 * through Spring Boot's key {@value #MAX_STRING_LENGTH_PROPERTY}. Jackson's own default is ten
 * times as long, and Spring Boot passes it on unchanged.
 * <p>
 * The GUI API has a smaller limit for the whole body, see {@link GuiApiRequestBodyLimit}. This one
 * holds for every JSON body, the reports of the BPMS API included, whose business data can be
 * large. A MongoDB document holds 16 MB, so no string far beyond this limit could be stored
 * anyway.
 * <p>
 * The default goes in at the lowest precedence, below every property source of the application.
 * An application which sets the key keeps its own value.
 */
public class JsonStringLengthDefault implements EnvironmentPostProcessor {

  /**
   * Spring Boot's key for the longest string Jackson reads.
   */
  public static final String MAX_STRING_LENGTH_PROPERTY = "spring.jackson.factory.constraints.read.max-string-length";

  /**
   * The value the cockpit gives the key when the application does not set it.
   */
  public static final int DEFAULT_MAX_STRING_LENGTH = 10_000_000;

  /**
   * Creates the post processor. Spring Boot creates it because {@code META-INF/spring.factories}
   * names it.
   */
  public JsonStringLengthDefault() {
  }

  @Override
  public void postProcessEnvironment(
      final ConfigurableEnvironment environment,
      final SpringApplication application) {

    if (environment.containsProperty(MAX_STRING_LENGTH_PROPERTY)) {
      return;
    }
    DefaultPropertiesPropertySource.addOrMerge(
        Map.of(MAX_STRING_LENGTH_PROPERTY, DEFAULT_MAX_STRING_LENGTH), environment.getPropertySources());

  }

}
