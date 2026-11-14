package io.vanillabp.cockpit.extension.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.vanillabp.cockpit.extension.spi.AdapterCockpitKey;
import io.vanillabp.cockpit.extension.spi.AdapterCockpitKeys;
import io.vanillabp.integration.extension.spi.settings.MisplacedSettings;

/**
 * Tells a key the Business Cockpit reads from one which merely looks like it.
 * <p>
 * A misspelled key is configuration a developer stares at for an afternoon. It stands in the
 * file, nobody reads it, and what it was meant to do simply does not happen. Quarkus refuses
 * such a key below <code>vanillabp</code> by itself and names it. This check runs before that
 * and says which key was meant, which is the part a framework cannot know.
 * <p>
 * Spring Boot binds around a key no binding declares. So on Spring Boot this check is the only
 * thing which ends the start, and it runs there while the cockpit's settings are read. Both
 * platforms refuse the same keys with the same message.
 * <p>
 * The check also looks at the place per adapter:
 * <code>vanillabp.adapters.&lt;adapter-id&gt;.cockpit.&lt;key&gt;</code> and the same position
 * below a workflow module, a workflow and a task. Only a BPMS half writes keys there, and it
 * announces them through {@link AdapterCockpitKeys}. Every other key there gets one of three
 * answers:
 * <ul>
 * <li>it is a neutral key, so it belongs to <code>vanillabp.cockpit</code> or to the
 * <code>cockpit</code> section of a workflow module, a workflow or a user task;</li>
 * <li>the BPMS half of this adapter type does not know it;</li>
 * <li>its BPMS half reads it, but at another level. This message has the words the VanillaBP core
 * uses for its own misplaced keys, see {@link MisplacedSettings}.</li>
 * </ul>
 * A neutral key is not read per adapter. During a migration two BPMS run the same workflow, and
 * the cockpit has to show it the same way for both.
 */
public final class ConfigurationKeyCheck {

  /**
   * How far a key may be from one the cockpit reads and still be named as the one meant. Three
   * edits catch a swapped letter, a missing one and a plural nobody asked for. Beyond three, two
   * unrelated keys start to look like each other.
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
   * One segment of a property name: an id as it is written, or an id with dots in it, which
   * Quarkus writes in quotes.
   */
  private static final String SEGMENT = "(\"[^\"]*\"|[^.\"]+)";

  /** <code>vanillabp.adapters.&lt;adapter-id&gt;.cockpit.&lt;key&gt;</code> */
  private static final Pattern AT_THE_ADAPTER = Pattern
      .compile("vanillabp\\.adapters\\.%s\\.cockpit\\.(.+)".formatted(SEGMENT));

  /**
   * The place per adapter below a workflow module, a workflow and a task. A task is written below
   * <code>tasks</code>, which is the platform's word, or below <code>user-tasks</code>, which is
   * the word version 1 of the cockpit used.
   */
  private static final Pattern BELOW_A_WORKFLOW_MODULE = Pattern
      .compile(
          "vanillabp\\.workflow-modules\\.%1$s(?:\\.workflows\\.%1$s(?:\\.(?:tasks|user-tasks)\\.%1$s)?)?\\.adapters\\.%1$s\\.cockpit\\.(.+)"
              .formatted(SEGMENT));

  /**
   * Keys which a snapshot of version 2 read at one place and which moved to another one before
   * the release. They are not keys of version 1, so no upgrade guide names them. A developer who
   * used the snapshot learns the new place from the message.
   */
  private static final Map<String, String> MOVED = Map
      .of(
          ConfigurationKeys.REMEMBERED_USER_TASKS_BEFORE,
          "it is read for each adapter id of the Process Engine API now, so each engine remembers its own user tasks. "
              + "Write it at 'vanillabp.adapters.<adapter-id>.cockpit.remembered-user-tasks'");

  /**
   * The keys version 1 of the Business Cockpit read and version 2 does not, with what an
   * application does instead. A key of an application which is being upgraded is far more likely
   * to be one of these than a typo. A developer who wrote it once wants to know what became of
   * it, not which key it resembles.
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
            "it made a consumer group unique, and the adapter only produces");
    gone
        .put(
            "jwt",
            "version 1 bound these keys and read them nowhere, and the cockpit server configures its own tokens");
    gone
        .put(
            "group-hierarchy-bean-name",
            "write a fixed hierarchy below 'group-hierarchy'. For a hierarchy which changes while the application runs, add a bean of type "
                + "io.vanillabp.spi.cockpit.workflowmodules.WorkflowModuleGroupHierarchy instead. Which groups may see a workflow module "
                + "is answered by the WorkflowModuleDetailsProvider bean");
    return Map.copyOf(gone);

  }

  private ConfigurationKeyCheck() {
  }

  /**
   * The keys of version 1 which version 2 does not read. A key which stood for a whole section
   * in version 1 is the name of that section. The configuration metadata of the Spring Boot
   * module describes them as removed, and its test reads them from here.
   *
   * @return The keys, below <code>vanillabp.cockpit</code> or below the <code>cockpit</code>
   *         section of a workflow module
   */
  public static Set<String> keysOfVersionOne() {

    return GONE.keySet();

  }

  /**
   * The keys the BPMS halves on the class path announced, see {@link AdapterCockpitKeys}.
   *
   * @return What each BPMS half reads below the adapter ids of its type
   */
  public static List<AdapterCockpitKeys> keysOfTheBpmsHalves() {

    final var classLoader = Thread.currentThread().getContextClassLoader() != null
        ? Thread.currentThread().getContextClassLoader()
        : ConfigurationKeyCheck.class.getClassLoader();
    return ServiceLoader
        .load(AdapterCockpitKeys.class, classLoader)
        .stream()
        .map(ServiceLoader.Provider::get)
        .toList();

  }

  /**
   * Looks at every key of an application which stands in one of the cockpit's sections. It takes
   * each adapter id as the name of its adapter type, and it asks the service loader for the keys
   * of the BPMS halves.
   *
   * @param propertyNames The property names of the application
   * @throws IllegalStateException If one of them is no setting of the Business Cockpit. The
   *           message names it and the one it is nearest to
   */
  public static void refuseKeysNobodyReads(
      final Iterable<String> propertyNames) {

    refuseKeysNobodyReads(propertyNames, Function.identity(), keysOfTheBpmsHalves());

  }

  /**
   * Looks at every key of an application which stands in one of the cockpit's sections,
   * including the place per adapter.
   *
   * @param propertyNames The property names of the application
   * @param adapterTypeOf The type of an adapter id, as the platform resolves it. An id without a
   *          type of its own is its type
   * @param keysOfTheBpmsHalves What each BPMS half reads below the adapter ids of its type
   * @throws IllegalStateException If one of them is no setting of the Business Cockpit, or one
   *           written at a level where nobody reads it. The message names each key and what to
   *           do with it
   */
  public static void refuseKeysNobodyReads(
      final Iterable<String> propertyNames,
      final Function<String, String> adapterTypeOf,
      final Collection<AdapterCockpitKeys> keysOfTheBpmsHalves) {

    final var defects = new ArrayList<String>();
    final var misplaced = new TreeMap<Misplaced, List<String>>(Comparator
        .comparing(Misplaced::adapterType)
        .thenComparing(one -> one.key().name()));
    for (final var propertyName : propertyNames) {
      final var name = withoutAnIndex(withoutAProfile(propertyName));
      final var position = AdapterPosition.of(name);
      if (position != null) {
        final var defect = defectAtTheAdapter(
            name, position, adapterTypeOf.apply(position.adapterId()), keysOfTheBpmsHalves, misplaced);
        if (defect != null) {
          defects.add(defect);
        }
        continue;
      }
      final var defect = defectOf(propertyName);
      if (defect != null) {
        defects.add(defect);
      }
    }
    if (defects.isEmpty() && misplaced.isEmpty()) {
      return;
    }
    final var answers = new ArrayList<String>();
    if (!defects.isEmpty()) {
      answers
          .add(
              """
                  The Business Cockpit adapter is configured with a key it does not read:
                  %s"""
                  .formatted(String.join("\n", defects)));
    }
    misplaced
        .forEach((
            what,
            writtenAt) -> answers.add(refusalOf(what, writtenAt).getMessage()));
    throw new IllegalStateException(String.join("\n\n", answers));

  }

  /**
   * A key of a BPMS half, written at a level where its half does not read it.
   *
   * @param adapterType The type of the adapter whose id carries the key
   * @param key What the BPMS half announced
   */
  private record Misplaced(
                           String adapterType,
                           AdapterCockpitKey key) {
  }

  /**
   * Where a key at the place per adapter stands.
   *
   * @param workflowModuleId The workflow module, or <code>null</code> at the adapter
   * @param bpmnProcessId The workflow, or <code>null</code> above it
   * @param taskId The task, or <code>null</code> above it
   * @param adapterId The adapter id
   * @param key The key below the <code>cockpit</code> section
   */
  private record AdapterPosition(
                                 String workflowModuleId,
                                 String bpmnProcessId,
                                 String taskId,
                                 String adapterId,
                                 String key) {

    static AdapterPosition of(
        final String name) {

      final Matcher atTheAdapter = AT_THE_ADAPTER.matcher(name);
      if (atTheAdapter.matches()) {
        return new AdapterPosition(null, null, null, unquoted(atTheAdapter.group(1)), atTheAdapter.group(2));
      }
      final Matcher below = BELOW_A_WORKFLOW_MODULE.matcher(name);
      if (below.matches()) {
        return new AdapterPosition(
            unquoted(below.group(1)), unquoted(below.group(2)), unquoted(below.group(3)), unquoted(
                below.group(4)), below
                    .group(5));
      }
      return null;

    }

    AdapterCockpitKey.Level level() {

      if (taskId != null) {
        return AdapterCockpitKey.Level.TASK;
      }
      if (bpmnProcessId != null) {
        return AdapterCockpitKey.Level.WORKFLOW;
      }
      if (workflowModuleId != null) {
        return AdapterCockpitKey.Level.WORKFLOW_MODULE;
      }
      return AdapterCockpitKey.Level.ADAPTER;

    }

    private static String unquoted(
        final String segment) {

      return (segment != null) && segment.startsWith("\"") && segment.endsWith("\"") && (segment.length() > 1)
          ? segment.substring(1, segment.length() - 1)
          : segment;

    }

  }

  private static String defectAtTheAdapter(
      final String propertyName,
      final AdapterPosition position,
      final String adapterType,
      final Collection<AdapterCockpitKeys> keysOfTheBpmsHalves,
      final Map<Misplaced, List<String>> misplaced) {

    final var keysOfTheType = keysOfTheBpmsHalves
        .stream()
        .filter(half -> adapterType.equals(half.adapterType()))
        .flatMap(half -> half.keys().stream())
        .toList();
    final var announced = keysOfTheType
        .stream()
        .filter(key -> sameKey(key.name(), position.key()))
        .findFirst()
        .orElse(null);
    if (announced != null) {
      if (!announced.readAt().contains(position.level())) {
        misplaced
            .computeIfAbsent(new Misplaced(adapterType, announced), what -> new ArrayList<>())
            .add(propertyName);
      }
      return null;
    }
    final var neutralPlace = whereANeutralKeyBelongs(position);
    if (neutralPlace != null) {
      return ("  - '%s' is a setting for every BPMS, so the Business Cockpit does not read it for one adapter. "
          + "Write it at '%s'. During a migration two BPMS run the same workflow, and the cockpit shows it the same way for both.")
          .formatted(propertyName, neutralPlace);
    }
    if (keysOfTheType.isEmpty()) {
      return "  - '%s' is no setting of the Business Cockpit. Its adapter for the adapter type '%s' reads no key below an adapter id."
          .formatted(propertyName, adapterType);
    }
    final var names = keysOfTheType
        .stream()
        .map(AdapterCockpitKey::name)
        .sorted()
        .toList();
    final var nearest = nearest(position.key(), names);
    if (nearest != null) {
      return "  - '%s' is no setting of the Business Cockpit adapter for '%s'. Did you mean '%s'?"
          .formatted(propertyName, adapterType, propertyName.substring(
              0, propertyName.length() - position.key().length()) + nearest);
    }
    return "  - '%s' is no setting the Business Cockpit adapter for '%s' reads below an adapter id. Those are: %s."
        .formatted(propertyName, adapterType, String.join(", ", names));

  }

  /**
   * Where a neutral key written at the place per adapter belongs: at the same level without the
   * adapter, or at the level where the key is read.
   *
   * @return The full key to write instead, or <code>null</code> where the key is no neutral key
   */
  private static String whereANeutralKeyBelongs(
      final AdapterPosition position) {

    final var key = position.key();
    final var module = position.workflowModuleId() == null
        ? "<workflow-module>"
        : position.workflowModuleId();
    final var workflow = position.bpmnProcessId() == null
        ? "<bpmn-process-id>"
        : position.bpmnProcessId();
    final var task = position.taskId() == null
        ? "<task>"
        : position.taskId();
    final var userTaskKey = "vanillabp.workflow-modules.%s.%s.%s.%s.%s.%s"
        .formatted(
            module, ConfigurationKeys.WORKFLOWS_SECTION, workflow, ConfigurationKeys.USER_TASKS_SECTION, task,
            ConfigurationKeys.COCKPIT_SECTION);
    final var ofTheLevel = switch (position.level()) {
      case TASK -> spelledAsIn(ConfigurationKeys.KEYS_OF_A_USER_TASK, key, k -> userTaskKey
          + "."
          + k);
      case WORKFLOW -> spelledAsIn(ConfigurationKeys.KEYS_OF_A_WORKFLOW, key, k -> ConfigurationKeys
          .workflowKey(module, workflow, k));
      case WORKFLOW_MODULE -> spelledAsIn(
          ConfigurationKeys.KEYS_OF_A_WORKFLOW_MODULE, key, k -> ConfigurationKeys.workflowModuleKey(module, k));
      case ADAPTER -> spelledAsIn(ConfigurationKeys.GLOBAL_KEYS, key, ConfigurationKeys::globalKey);
    };
    if (ofTheLevel != null) {
      return ofTheLevel;
    }
    if (isInTheGroupHierarchy(key)) {
      return ConfigurationKeys.workflowModuleKey(module, key);
    }
    final var global = spelledAsIn(ConfigurationKeys.GLOBAL_KEYS, key, ConfigurationKeys::globalKey);
    if (global != null) {
      return global;
    }
    final var ofAModule = spelledAsIn(
        ConfigurationKeys.KEYS_OF_A_WORKFLOW_MODULE, key, k -> ConfigurationKeys.workflowModuleKey(module, k));
    if (ofAModule != null) {
      return ofAModule;
    }
    final var ofAWorkflow = spelledAsIn(ConfigurationKeys.KEYS_OF_A_WORKFLOW, key, k -> ConfigurationKeys
        .workflowKey(module, workflow, k));
    if (ofAWorkflow != null) {
      return ofAWorkflow;
    }
    return spelledAsIn(ConfigurationKeys.KEYS_OF_A_USER_TASK, key, k -> userTaskKey
        + "."
        + k);

  }

  private static boolean isInTheGroupHierarchy(
      final String key) {

    return key.startsWith(ConfigurationKeys.GROUP_HIERARCHY
        + ".");

  }

  /**
   * @param keys The keys of one level
   * @param key What an application wrote
   * @param fullKey How the full key of that level is spelled
   * @return The full key, spelled as the cockpit spells it, or <code>null</code> where the
   *         level has no such key
   */
  private static String spelledAsIn(
      final List<String> keys,
      final String key,
      final Function<String, String> fullKey) {

    return keys
        .stream()
        .filter(candidate -> sameKey(candidate, key))
        .findFirst()
        .map(fullKey)
        .orElse(null);

  }

  /**
   * The refusal of a key of a BPMS half which was written at a level where the half does not
   * read it, in the words of the VanillaBP core.
   */
  private static IllegalStateException refusalOf(
      final Misplaced what,
      final List<String> writtenAt) {

    final var levels = what
        .key()
        .readAt()
        .stream()
        .sorted()
        .toList();
    return MisplacedSettings
        .refuse(
            "The Business Cockpit adapter for '%s' reads '%s' %s"
                .formatted(what.adapterType(), what.key().name(), levels
                    .stream()
                    .map(level -> "at the "
                        + nameOf(level))
                    .collect(Collectors.joining(" and "))),
            writtenAt, levels.size() == 1
                ? "the %s it is meant for".formatted(nameOf(levels.get(0)))
                : "one of these keys",
            levels
                .stream()
                .map(level -> "%s: %s".formatted(keyAt(level, what.key().name()), what.key().example()))
                .toList(),
            what.key().whyOnlyThere());

  }

  private static String nameOf(
      final AdapterCockpitKey.Level level) {

    return switch (level) {
      case ADAPTER -> "adapter";
      case WORKFLOW_MODULE -> "workflow module";
      case WORKFLOW -> "workflow";
      case TASK -> "task";
    };

  }

  private static String keyAt(
      final AdapterCockpitKey.Level level,
      final String key) {

    final var below = ".adapters.<adapter-id>.%s.%s".formatted(ConfigurationKeys.COCKPIT_SECTION, key);
    return switch (level) {
      case ADAPTER -> "vanillabp"
          + below;
      case WORKFLOW_MODULE -> "vanillabp.workflow-modules.<workflow-module>"
          + below;
      case WORKFLOW -> "vanillabp.workflow-modules.<workflow-module>.workflows.<bpmn-process-id>"
          + below;
      case TASK -> "vanillabp.workflow-modules.<workflow-module>.workflows.<bpmn-process-id>.tasks.<task>"
          + below;
    };

  }

  /**
   * Spring Boot binds a key in several spellings: <code>base-url</code>, <code>baseUrl</code>
   * and, from an environment variable, <code>baseurl</code>. They are the same key. Quarkus
   * binds only the first one, and it refuses the others by itself.
   *
   * @return Whether the two keys are the same key for Spring Boot
   */
  private static boolean sameKey(
      final String one,
      final String other) {

    return uniform(one).equals(uniform(other));

  }

  private static String uniform(
      final String key) {

    return key
        .replace("-", "")
        .toLowerCase(Locale.ROOT);

  }

  private static String defectOf(
      final String propertyName) {

    final var name = withoutAnIndex(withoutAProfile(propertyName));
    if (name.startsWith(ConfigurationKeys.GLOBAL_PREFIX
        + ".")) {
      final var key = name.substring(ConfigurationKeys.GLOBAL_PREFIX.length() + 1);
      if (key.startsWith(ConfigurationKeys.KAFKA_PROPERTIES_PREFIX)) {
        return null;
      }
      final var movedTo = whereItMoved(key);
      if (movedTo != null) {
        return "  - '%s' is no longer read there: %s.".formatted(name, movedTo);
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
    if (isInTheGroupHierarchy(key)) {
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

    if (keysOfTheLevel.stream().anyMatch(candidate -> sameKey(candidate, key))) {
      return null;
    }
    final var whatBecameOfIt = whatBecameOf(key);
    if (whatBecameOfIt != null) {
      return "  - '%s' is a key of version 1 of the Business Cockpit which version 2 does not read: %s."
          .formatted(propertyName, whatBecameOfIt);
    }
    final var movedTo = whereItMoved(key);
    if (movedTo != null) {
      return "  - '%s' is no longer read there: %s.".formatted(propertyName, movedTo);
    }
    // a key the cockpit reads for the whole application, written for one module or workflow. The
    // developer knows the key and has put it in the wrong place, so the place is what they need.
    // At the top this cannot be true, because a key of the top was accepted above
    if (ConfigurationKeys.GLOBAL_KEYS.contains(key)) {
      return "  - '%s' is read for the whole application only, not for %s. Write it once as '%s'."
          .formatted(propertyName, whatTheLevelIs, ConfigurationKeys.globalKey(key));
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
   * @param key What an application wrote, at any level of the neutral tree
   * @return Where it is read now, where a snapshot of version 2 read it somewhere else, else
   *         <code>null</code>
   */
  private static String whereItMoved(
      final String key) {

    return MOVED
        .entrySet()
        .stream()
        .filter(entry -> sameKey(entry.getKey(), key))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElse(null);

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
   * A key which holds for one profile is the same key.
   * <code>%prod.vanillabp.cockpit.rest.log</code> is as gone as
   * <code>vanillabp.cockpit.rest.log</code>, and a developer who misspelled it in a profile wants
   * to hear about it there too. SmallRye usually hands the name over without the prefix. Where it
   * does not, this is what takes it off.
   *
   * @param propertyName A property name as a platform spells it
   * @return The name without the profile it holds for
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
