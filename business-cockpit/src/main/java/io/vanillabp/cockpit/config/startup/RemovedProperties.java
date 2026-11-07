package io.vanillabp.cockpit.config.startup;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.context.properties.source.ConfigurationPropertyState;
import org.springframework.core.env.Environment;

/**
 * The keys the cockpit used to bind and does not bind any more. Somebody who still sets one of
 * them believes it changes something, so {@link StartupConfigurationCheck} stops the start and
 * says what became of it.
 */
final class RemovedProperties {

  /** Each removed key, with what to know instead of it. */
  private static final Map<String, String> REMOVED = removed();

  private static Map<String, String> removed() {

    final var removed = new LinkedHashMap<String, String>();
    removed.put(
        "mongodb.change-stream-executor",
        "Nothing ever read it. The change streams run on the threads of Spring's message listener "
            + "container.");
    return removed;

  }

  private RemovedProperties() {
  }

  /**
   * The removed keys an application sets, in any spelling which relaxed binding accepts.
   *
   * @param environment The configuration of the application
   * @return One line per removed key which is set, naming the key and what became of it
   */
  static List<String> setIn(
      final Environment environment) {

    final var sources = ConfigurationPropertySources.get(environment);
    return REMOVED
        .entrySet()
        .stream()
        .filter(removed -> isSet(sources, ConfigurationPropertyName.of(removed.getKey())))
        .map(removed -> "'%s': %s".formatted(removed.getKey(), removed.getValue()))
        .toList();

  }

  private static boolean isSet(
      final Iterable<ConfigurationPropertySource> sources,
      final ConfigurationPropertyName key) {

    for (final var source : sources) {
      if ((source.getConfigurationProperty(key) != null) || (source
          .containsDescendantOf(key) == ConfigurationPropertyState.PRESENT)) {
        return true;
      }
    }
    return false;

  }

}
