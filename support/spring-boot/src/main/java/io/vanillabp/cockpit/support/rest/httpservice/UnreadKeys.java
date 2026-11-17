package io.vanillabp.cockpit.support.rest.httpservice;

import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.context.properties.source.IterableConfigurationPropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

/**
 * Finds the keys below the prefix of a group which no setting reads, for example a misspelled
 * {@code read-timout}. A key which is read by nobody would change nothing, and the developer would
 * not learn why.
 * <p>
 * The known keys are taken from the properties class itself, so a key a later Spring Boot adds to
 * {@code HttpClientProperties} is known here without a change.
 * <p>
 * Environment variables are not looked at. Their names cannot be told apart from those of other
 * programs which happen to start the same way.
 */
final class UnreadKeys {

  /** A setting with sub-keys of any name, like {@code default-header.<name>}. */
  private static final Map<String, Object> ANY_NAME = Map.of();

  /** The known keys, as a tree in Spring Boot's uniform form: lower case, without dashes. */
  private static final Map<String, Object> KNOWN = keysOf(HttpServiceGroupProperties.class);

  private UnreadKeys() {
  }

  /** The keys below {@code prefix} which no setting of {@link HttpServiceGroupProperties} reads. */
  static Collection<String> below(
      final Environment environment,
      final String prefix) {

    final var root = ConfigurationPropertyName.of(prefix);
    final var unread = new TreeSet<String>();
    for (final var source : ConfigurationPropertySources.get(environment)) {
      if (source.getUnderlyingSource() instanceof SystemEnvironmentPropertySource) {
        continue;
      }
      if (source instanceof IterableConfigurationPropertySource iterable) {
        iterable.stream()
            .filter(root::isAncestorOf)
            .filter(name -> !isKnown(name, root.getNumberOfElements()))
            .forEach(name -> unread.add(name.toString()));
      }
    }
    return unread;

  }

  /** Whether any key is set below {@code prefix}, environment variables not counted. */
  static boolean anyBelow(
      final Environment environment,
      final String prefix) {

    final var root = ConfigurationPropertyName.of(prefix);
    for (final var source : ConfigurationPropertySources.get(environment)) {
      if (source.getUnderlyingSource() instanceof SystemEnvironmentPropertySource) {
        continue;
      }
      if ((source instanceof IterableConfigurationPropertySource iterable) && iterable.stream()
          .anyMatch(root::isAncestorOf)) {
        return true;
      }
    }
    return false;

  }

  @SuppressWarnings("unchecked")
  private static boolean isKnown(
      final ConfigurationPropertyName name,
      final int firstElement) {

    var level = KNOWN;
    for (var index = firstElement; index < name.getNumberOfElements(); ++index) {
      if (level == ANY_NAME) {
        return true;
      }
      if (name.isNumericIndex(index)) {
        // the index of a list, like 'oauth.scope[0]'
        continue;
      }
      final var element = name.getElement(index, ConfigurationPropertyName.Form.UNIFORM);
      if (!level.containsKey(element)) {
        return false;
      }
      final var next = level.get(element);
      if (next == null) {
        // a single value: only list indexes may follow
        for (var rest = index + 1; rest < name.getNumberOfElements(); ++rest) {
          if (!name.isNumericIndex(rest)) {
            return false;
          }
        }
        return true;
      }
      level = (Map<String, Object>) next;
    }
    return true;

  }

  /**
   * The keys a properties class reads: a single value is a leaf ({@code null}), a map takes any
   * sub-key, and any other type is a group of keys of its own.
   */
  private static Map<String, Object> keysOf(
      final Class<?> type) {

    final var keys = new HashMap<String, Object>();
    for (final var property : BeanUtils.getPropertyDescriptors(type)) {
      if (property.getReadMethod() == null || property.getName().equals("class")) {
        continue;
      }
      final var propertyType = property.getPropertyType();
      final var uniformName = property.getName().toLowerCase();
      if (BeanUtils.isSimpleValueType(propertyType) || TemporalAmount.class.isAssignableFrom(
          propertyType) || Collection.class.isAssignableFrom(propertyType) || propertyType.isArray()) {
        keys.put(uniformName, null);
      } else if (Map.class.isAssignableFrom(propertyType)) {
        keys.put(uniformName, ANY_NAME);
      } else {
        keys.put(uniformName, keysOf(propertyType));
      }
    }
    return keys;

  }

}
