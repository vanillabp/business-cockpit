package io.vanillabp.cockpit.support.rest.restclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.Config;

/**
 * Finds the keys at the prefix of REST clients which nobody reads, for example a misspelled
 * {@code read-timout}. Such a key would change nothing, and the developer would not learn why.
 * <p>
 * A key which holds for one profile only, like {@code %dev.loan-approval.cockpit.base-url}, is
 * checked the same way. Environment variables are not looked at: their names cannot be told apart
 * from those of other programs which happen to start the same way.
 */
final class UnreadKeys {

  /** Every key {@link RestClientKeys} reads, without the prefix. */
  static final List<String> KNOWN = List.of(
      "base-url",
      "connect-timeout",
      "read-timeout",
      "redirects",
      "verify-ssl",
      "tls-configuration-name",
      "log",
      "connection-pool-size",
      "proxy.host",
      "proxy.port",
      "proxy.username",
      "proxy.password",
      "retry.enabled",
      "retry.max-attempts",
      "retry.period",
      "retry.max-period",
      "oauth.token-uri",
      "oauth.client-id",
      "oauth.client-secret",
      "oauth.client-authentication-method",
      "oauth.scope",
      "oauth.renew-before",
      "oauth.lifetime-without-expires-in");

  /** A setting with sub-keys of any name. */
  private static final String DEFAULT_HEADER = "default-header.";

  /** The index of a list, like {@code oauth.scope[0]}. */
  private static final Pattern LIST_INDEX = Pattern.compile("\\[\\d+]$");

  /**
   * Keys of the Spring Boot module which have no counterpart on Quarkus, with what to do instead.
   */
  private static final Map<String, String> SPRING_BOOT_ONLY = Map.of(
      "ssl.bundle", """
          Spring Boot's SSL bundles do not exist on Quarkus. Put the certificates into a \
          configuration of the Quarkus TLS registry, 'quarkus.tls.<name>.*', and name it in '%s'.""",
      "cookie-handling", "Quarkus' REST client keeps no cookies between requests. Remove the key.",
      "apiversion", "Quarkus' REST client has no API versions. Remove the keys, or send the version with "
          + "'%s.default-header.<name>'.");

  /** Up to this many single-character edits, a key counts as a misspelling of a known key. */
  private static final int NEAREST = 4;

  private UnreadKeys() {
  }

  /**
   * One problem for every key at {@code prefix} which nobody reads.
   *
   * @param config where the keys are read from
   * @param prefix the key all keys of the clients start with
   * @return the problems, empty if every key is read
   */
  static List<String> below(
      final Config config,
      final String prefix) {

    final var start = prefix
        + ".";
    final var problems = new ArrayList<String>();
    final var propertyNames = new TreeSet<String>();
    config.getPropertyNames().forEach(propertyNames::add);
    for (final var propertyName : propertyNames) {
      final var key = withoutAProfile(propertyName);
      if (!key.startsWith(start)) {
        continue;
      }
      final var name = LIST_INDEX.matcher(key.substring(start.length())).replaceAll("");
      if (KNOWN.contains(name) || (name.startsWith(DEFAULT_HEADER) && (name.length() > DEFAULT_HEADER.length()))) {
        continue;
      }
      problems.add(problemOf(propertyName, prefix, name));
    }
    return problems;

  }

  private static String problemOf(
      final String propertyName,
      final String prefix,
      final String name) {

    final var nobodyReadsIt = "The key '%s' of the REST clients at '%s' is read by nobody."
        .formatted(propertyName, prefix);
    final var springBootOnly = SPRING_BOOT_ONLY
        .entrySet()
        .stream()
        .filter(entry -> name.equals(entry.getKey()) || name.startsWith(entry.getKey()
            + "."))
        .findFirst();
    if (springBootOnly.isPresent()) {
      return nobodyReadsIt
          + " "
          + springBootOnly.get().getValue()
              .formatted(prefix + (name.startsWith("ssl") ? ".tls-configuration-name" : ""));
    }
    final var nearest = nearest(name);
    if (nearest != null) {
      return nobodyReadsIt + " Did you mean '%s.%s'?".formatted(prefix, nearest);
    }
    return nobodyReadsIt
        + " The support README lists every key.";

  }

  /**
   * A key which holds for one profile is the same key: {@code %dev.inventory.base-urll} is as wrong
   * as {@code inventory.base-urll}.
   */
  private static String withoutAProfile(
      final String propertyName) {

    if (!propertyName.startsWith("%")) {
      return propertyName;
    }
    final var name = propertyName.indexOf('.');
    return name < 0
        ? propertyName
        : propertyName.substring(name + 1);

  }

  private static String nearest(
      final String name) {

    var nearest = (String) null;
    var distance = NEAREST + 1;
    for (final var candidate : KNOWN) {
      final var edits = edits(name.toLowerCase(Locale.ROOT), candidate);
      if (edits < distance) {
        distance = edits;
        nearest = candidate;
      }
    }
    return nearest;

  }

  /** How many single-character edits turn one key into the other. */
  private static int edits(
      final String one,
      final String other) {

    var previous = new int[other.length() + 1];
    for (var column = 0; column <= other.length(); column++) {
      previous[column] = column;
    }
    for (var row = 1; row <= one.length(); row++) {
      final var current = new int[other.length() + 1];
      current[0] = row;
      for (var column = 1; column <= other.length(); column++) {
        current[column] = Math.min(
            Math.min(current[column - 1] + 1, previous[column] + 1),
            previous[column - 1] + (one.charAt(row - 1) == other.charAt(column - 1) ? 0 : 1));
      }
      previous = current;
    }
    return previous[other.length()];

  }

}
