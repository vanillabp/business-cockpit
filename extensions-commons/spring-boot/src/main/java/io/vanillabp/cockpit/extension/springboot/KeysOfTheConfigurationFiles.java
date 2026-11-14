package io.vanillabp.cockpit.extension.springboot;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.context.properties.bind.DataObjectPropertyName;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import io.vanillabp.cockpit.extension.config.ConfigurationKeyCheck;

/**
 * The keys of the Business Cockpit an application wrote, in the form {@link ConfigurationKeyCheck}
 * reads.
 * <p>
 * Spring Boot binds <code>restBaseUrl</code>, <code>rest_base_url</code> and
 * <code>rest-base-url</code> to the same setting. The check knows the dashed form only, so the
 * key is turned into it before it is checked. Only the key is turned, and not the workflow
 * module id or the process id in front of it, so a refusal names them as the application wrote
 * them.
 * <p>
 * Environment variables are left out. An environment variable spells a key in a form which
 * cannot be taken apart again without guessing, so a dash and a dot look the same there.
 */
final class KeysOfTheConfigurationFiles {

  private static final String VANILLABP = "vanillabp.";

  private static final String COCKPIT_SECTION = ".cockpit.";

  private KeysOfTheConfigurationFiles() {
  }

  /**
   * Collects the keys below <code>vanillabp</code> which the application wrote.
   *
   * @param environment The environment of the application
   * @return Every key below <code>vanillabp</code> the files, the command line and the system
   *         properties hold
   */
  static Set<String> of(
      final ConfigurableEnvironment environment) {

    final var keys = new LinkedHashSet<String>();
    for (final var source : environment.getPropertySources()) {
      if (!(source instanceof EnumerablePropertySource<?> enumerable) || (source instanceof SystemEnvironmentPropertySource)) {
        continue;
      }
      for (final var name : enumerable.getPropertyNames()) {
        if (name.startsWith(VANILLABP)) {
          keys.add(inTheDashedForm(name));
        }
      }
    }
    return keys;

  }

  /**
   * Turns the part of a key below a cockpit section into the dashed form.
   *
   * @param name A key as the application wrote it
   * @return The same key, with the part below the cockpit's section in the dashed form
   */
  static String inTheDashedForm(
      final String name) {

    final var section = name.lastIndexOf(COCKPIT_SECTION);
    if (section < 0) {
      return name;
    }
    final var start = section + COCKPIT_SECTION.length();
    return name.substring(0, start) + DataObjectPropertyName.toDashedForm(name.substring(start));

  }

}
