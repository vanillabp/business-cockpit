package io.vanillabp.cockpit.extension.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Tells a key the Business Cockpit reads from one which merely looks like it.
 * <p>
 * A misspelled key is configuration a developer stares at for an afternoon: it stands in the
 * file, it is read by nobody, and what it was meant to do simply does not happen. Quarkus
 * refuses such a key below <code>vanillabp</code> by itself, naming it; this check runs before
 * that and says which key was meant, which is the part a framework cannot know.
 * <p>
 * Spring Boot ignores a key no binding declares, and this repository leaves that as it is: the
 * asymmetry between the two platforms is the platform integration's own, documented decision.
 */
public final class ConfigurationKeyCheck {

  /**
   * How far a key may be from one the cockpit reads and still be named as the one meant. Three
   * edits catch a swapped letter, a missing one and a plural nobody asked for; beyond that two
   * unrelated keys start looking like each other.
   */
  private static final int NEAREST = 3;

  private static final String WORKFLOW_MODULES = "vanillabp.workflow-modules.";

  private static final String WORKFLOWS = "."
      + ConfigurationKeys.WORKFLOWS_SECTION
      + ".";

  private static final String USER_TASKS = "."
      + ConfigurationKeys.USER_TASKS_SECTION
      + ".";

  private static final String COCKPIT_SECTION = "."
      + ConfigurationKeys.COCKPIT_SECTION
      + ".";

  /**
   * The keys version 1 of the Business Cockpit read and version 2 does not, with what an
   * application does instead. A key of an application which is being upgraded is far more likely
   * to be one of these than a typo, and a developer who wrote it once wants to know what became of
   * it rather than which key it resembles.
   */
  private static final Map<String, String> GONE = goneKeys();

  private static Map<String, String> goneKeys() {

    final var gone = new LinkedHashMap<String, String>();
    gone
        .put(
            "rest.log",
            "the client logs through SLF4J, so set the logger 'io.vanillabp.cockpit.bpms.api.v1_1.BpmsApi' to DEBUG");
    gone
        .put(
            "rest.additional-get-parameters",
            "it appended query parameters to GET requests, and every report is a POST");
    gone
        .put(
            "rest.retry",
            "a failed report waits in the outbox and is repeated from there, which 'vanillabp.outbox.*' configures");
    gone
        .put(
            "kafka.group-id-suffix",
            "it made a consumer group unique, and the extension only produces");
    gone
        .put(
            "jwt",
            "version 1 bound these keys and read them nowhere, and the cockpit server configures its own tokens");
    gone
        .put(
            "group-hierarchy-bean-name",
            "the hierarchy is configuration, and which groups may see a workflow module is answered by the WorkflowModuleDetailsProvider bean");
    return Map.copyOf(gone);

  }

  private ConfigurationKeyCheck() {
  }

  /**
   * Looks at every key of an application which stands in one of the cockpit's sections.
   *
   * @param propertyNames The property names of the application
   * @throws IllegalStateException If one of them is no setting of the Business Cockpit. The
   *           message names it and the one it is nearest to
   */
  public static void refuseKeysNobodyReads(
      final Iterable<String> propertyNames) {

    final var defects = new ArrayList<String>();
    for (final var propertyName : propertyNames) {
      final var defect = defectOf(propertyName);
      if (defect != null) {
        defects.add(defect);
      }
    }
    if (defects.isEmpty()) {
      return;
    }
    throw new IllegalStateException(
        """
            The Business Cockpit extension is configured with a key it does not read:
            %s"""
            .formatted(String.join("\n", defects)));

  }

  private static String defectOf(
      final String propertyName) {

    final var name = withoutAnIndex(propertyName);
    if (name.startsWith(ConfigurationKeys.GLOBAL_PREFIX
        + ".")) {
      final var key = name.substring(ConfigurationKeys.GLOBAL_PREFIX.length() + 1);
      if (key.startsWith(ConfigurationKeys.KAFKA_PROPERTIES_PREFIX)) {
        return null;
      }
      return defectOf(name, key, ConfigurationKeys.GLOBAL_KEYS, "the whole application");
    }
    if (!name.startsWith(WORKFLOW_MODULES)) {
      return null;
    }
    final var section = name.lastIndexOf(COCKPIT_SECTION);
    if (section < 0) {
      return null;
    }
    final var key = name.substring(section + COCKPIT_SECTION.length());
    final var above = name.substring(0, section);
    if (above.contains(WORKFLOWS)) {
      return above.contains(USER_TASKS)
          ? defectOf(name, key, ConfigurationKeys.KEYS_OF_A_USER_TASK, "a single user task")
          : defectOf(name, key, ConfigurationKeys.KEYS_OF_A_WORKFLOW, "a single workflow");
    }
    if (key.startsWith(ConfigurationKeys.GROUP_HIERARCHY
        + ".")) {
      return null;
    }
    return defectOf(
        name, key, ConfigurationKeys.KEYS_OF_A_WORKFLOW_MODULE, "a workflow module");

  }

  private static String defectOf(
      final String propertyName,
      final String key,
      final List<String> keysOfTheLevel,
      final String whatTheLevelIs) {

    if (keysOfTheLevel.contains(key)) {
      return null;
    }
    final var whatBecameOfIt = whatBecameOf(key);
    if (whatBecameOfIt != null) {
      return "  - '%s' is a key of version 1 of the Business Cockpit which version 2 does not read: %s."
          .formatted(propertyName, whatBecameOfIt);
    }
    final var nearest = nearest(key, keysOfTheLevel);
    if (nearest != null) {
      return "  - '%s' is no setting of the Business Cockpit. Did you mean '%s'?"
          .formatted(propertyName, propertyName.substring(
              0, propertyName.length() - key.length()) + nearest);
    }
    return "  - '%s' is no setting the Business Cockpit reads for %s. Those are: %s."
        .formatted(propertyName, whatTheLevelIs, String.join(", ", keysOfTheLevel));

  }

  /**
   * @param key What an application wrote
   * @return What became of it where it is a key of version 1, else <code>null</code>
   */
  private static String whatBecameOf(
      final String key) {

    return GONE
        .entrySet()
        .stream()
        .filter(gone -> key.equals(gone.getKey()) || key.startsWith(gone.getKey()
            + "."))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElse(null);

  }

  /**
   * @param key What an application wrote
   * @param keysOfTheLevel What it could have meant
   * @return The nearest of them, or <code>null</code> where none is near enough to guess
   */
  private static String nearest(
      final String key,
      final List<String> keysOfTheLevel) {

    var nearest = (String) null;
    var distance = NEAREST + 1;
    for (final var candidate : keysOfTheLevel) {
      final var edits = edits(key.toLowerCase(Locale.ROOT), candidate);
      if (edits < distance) {
        distance = edits;
        nearest = candidate;
      }
    }
    return nearest;

  }

  /**
   * @return How many single-character edits turn one key into the other
   */
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
        current[column] = Math
            .min(
                Math.min(current[column - 1] + 1, previous[column] + 1),
                previous[column - 1] + (one.charAt(row - 1) == other.charAt(column - 1) ? 0 : 1));
      }
      previous = current;
    }
    return previous[other.length()];

  }

  /**
   * @param propertyName A property name as a platform spells it
   * @return The name without the index a list entry carries, which is the same setting
   */
  private static String withoutAnIndex(
      final String propertyName) {

    return propertyName.endsWith("]")
        ? propertyName.substring(0, propertyName.lastIndexOf('['))
        : propertyName;

  }

}
