package io.vanillabp.cockpit.extension.springboot.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepositoryJsonBuilder;
import org.springframework.boot.configurationmetadata.Deprecation;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.DataObjectPropertyName;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.util.ClassUtils;

import io.vanillabp.cockpit.extension.config.BusinessCockpitConfiguration;
import io.vanillabp.cockpit.extension.config.ConfigurationKeyCheck;
import io.vanillabp.cockpit.extension.config.ConfigurationKeys;
import io.vanillabp.cockpit.extension.springboot.CockpitOverlayProperties;
import io.vanillabp.integration.adapter.migration.config.MigrationAdapterProperties;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Holds the configuration metadata of this module against the keys the cockpit reads. An IDE
 * offers and checks the keys in <code>application.yaml</code> from that metadata. So a key which
 * the code reads but the metadata does not describe is shown as unknown, and a key described but
 * not read is offered for nothing.
 * <p>
 * Two sides are compared here. One is what Spring Boot binds: the properties of
 * {@link CockpitOverlayProperties}, walked like the binder walks them. A map becomes a level whose
 * key the application chooses, written as <code>*</code> here. The other is what the cockpit
 * checks a key against: the lists of {@link ConfigurationKeys} for each level, and the keys of
 * version 1 in {@link ConfigurationKeyCheck}. Both sides have to agree, and the metadata has to
 * describe them.
 * <p>
 * The metadata has two parts, see decision 66 in the repository's DECISIONS.md. The
 * configuration processor writes an entry for every key below <code>vanillabp.cockpit</code>,
 * with the Javadoc of its field, and one entry for the map of the workflow modules. The
 * hand-written part adds the defaults, the keys of the levels below a workflow module and the
 * keys of version 1.
 */
@ExtendWith(SuppressOutputExtension.class)
public class EveryKeyIsKnownToTheIdeTest {

  private static final String METADATA = "META-INF/spring-configuration-metadata.json";

  private static final String PREFIX = "vanillabp";

  private static final String GLOBAL_LEVEL = ConfigurationKeys.GLOBAL_PREFIX
      + ".";

  private static final String WORKFLOW_MODULES = PREFIX
      + ".workflow-modules";

  private static final String COCKPIT = "."
      + ConfigurationKeys.COCKPIT_SECTION
      + ".";

  /**
   * The levels below a workflow module, as the binder finds them, as the description of
   * <code>vanillabp.workflow-modules</code> names them, and with the keys the cockpit reads there.
   * Most specific last, because the description names them in that order.
   */
  private static final List<Level> LEVELS = List
      .of(
          new Level(
              WORKFLOW_MODULES
                  + ".*", "'<module>.cockpit'", ConfigurationKeys.KEYS_OF_A_WORKFLOW_MODULE),
          new Level(
              WORKFLOW_MODULES
                  + ".*.workflows.*", "'<module>.workflows.<process>.cockpit'", ConfigurationKeys.KEYS_OF_A_WORKFLOW),
          new Level(
              WORKFLOW_MODULES
                  + ".*.workflows.*.user-tasks.*", "'<module>.workflows.<process>.user-tasks.<task>.cockpit'", ConfigurationKeys.KEYS_OF_A_USER_TASK));

  /**
   * The key of the Process-Engine-API half. Its default lives in that repository, and its test
   * holds the default described here against it.
   */
  private static final String DEFAULT_OF_ANOTHER_REPOSITORY = ConfigurationKeys
      .globalKey(ConfigurationKeys.REMEMBERED_USER_TASKS);

  private static Map<String, ConfigurationMetadataProperty> metadata;

  private static Map<String, Type> keysBound;

  private record Level(
                       String path,
                       String named,
                       List<String> keysRead) {
  }

  @BeforeAll
  public static void readBothSides() throws IOException {

    metadata = metadataOfThisModule();
    keysBound = new TreeMap<>();
    collectKeys(PREFIX, CockpitOverlayProperties.class, keysBound);

  }

  @Test
  @DisplayName("the processor describes every map of the overlay with the type of its values")
  public void everyMapOfTheOverlayIsDescribedWithItsValueType() {

    keysBound.forEach((
        key,
        type) -> {
      if (key.contains("*") || !(type instanceof ParameterizedType)) {
        return;
      }
      final var entry = metadata.get(key);
      assertNotNull(entry, "the configuration processor wrote no entry '%s'".formatted(key));
      assertEquals(typeName(type), typeName(entry.getType()), "the type of '%s' in the metadata".formatted(key));
    });

  }

  @Test
  @DisplayName("every key below vanillabp.cockpit is bound, checked and described with a type and a description")
  public void everyGlobalKeyIsDescribed() {

    final var bound = new TreeSet<String>();
    keysBound.forEach((
        key,
        type) -> {
      // the producer properties of Kafka are a map the application fills, and the check lets
      // every key below it pass
      if (key.startsWith(GLOBAL_LEVEL) && !(key
          + ".").startsWith(ConfigurationKeys.globalKey(ConfigurationKeys.KAFKA_PROPERTIES_PREFIX))) {
        bound.add(key.substring(GLOBAL_LEVEL.length()));
      }
    });
    assertEquals(
        new TreeSet<>(ConfigurationKeys.GLOBAL_KEYS), bound,
        "the keys Spring Boot binds below 'vanillabp.cockpit' and the keys ConfigurationKeys.GLOBAL_KEYS lets pass");

    final var missing = new TreeSet<String>();
    keysBound.forEach((
        key,
        type) -> {
      if (!key.startsWith(GLOBAL_LEVEL)) {
        return;
      }
      final var entry = metadata.get(key);
      if ((entry == null) || isBlank(entry.getDescription())) {
        missing.add(key);
        return;
      }
      assertEquals(typeName(type), typeName(entry.getType()), "the type of '%s' in the metadata".formatted(key));
    });
    assertTrue(
        missing.isEmpty(),
        "give these fields of CockpitOverlayProperties a Javadoc, or add them to META-INF/additional-spring-configuration-metadata.json: "
            + missing);

  }

  @Test
  @DisplayName("every key read below a workflow module is bound there and named by the description of its level")
  public void everyKeyOfALowerLevelIsNamed() {

    final var description = String.valueOf(metadata
        .get(WORKFLOW_MODULES)
        .getDescription());
    for (var index = 0; index < LEVELS.size(); ++index) {
      final var level = LEVELS.get(index);
      final var prefix = level.path() + COCKPIT;
      final var bound = new TreeSet<String>();
      keysBound
          .keySet()
          .stream()
          .filter(key -> key.startsWith(prefix) && !key
              .substring(prefix.length())
              .contains("*"))
          .map(key -> key.substring(prefix.length()))
          .forEach(bound::add);
      assertEquals(
          new TreeSet<>(level.keysRead()), bound,
          "the keys Spring Boot binds below %s and the keys ConfigurationKeys lets pass there"
              .formatted(level.named()));

      final var startOfText = description.indexOf(level.named());
      assertTrue(
          startOfText >= 0,
          "the description of '%s' in the metadata names the level %s".formatted(WORKFLOW_MODULES, level.named()));
      final var end = index + 1 < LEVELS.size()
          ? description.indexOf(LEVELS
              .get(index + 1)
              .named())
          : description.length();
      final var ofTheLevel = description.substring(startOfText, end < 0
          ? description.length()
          : end);
      final var notNamed = level
          .keysRead()
          .stream()
          .filter(key -> !ofTheLevel.contains("'"
              + key
              + "'"))
          .toList();
      assertTrue(
          notNamed.isEmpty(),
          "name these keys in the description of '%s', where it talks about %s: %s"
              .formatted(WORKFLOW_MODULES, level.named(), notNamed));
    }

    // a key of the whole application which a workflow module may answer as well says where
    final var notNamed = new TreeSet<String>();
    ConfigurationKeys.GLOBAL_KEYS
        .stream()
        .filter(ConfigurationKeys.KEYS_OF_A_WORKFLOW_MODULE::contains)
        .map(ConfigurationKeys::globalKey)
        .filter(key -> !String
            .valueOf(metadata
                .get(key)
                .getDescription())
            .contains(WORKFLOW_MODULES
                + "."))
        .forEach(notNamed::add);
    assertTrue(
        notNamed.isEmpty(),
        "these keys are read below 'vanillabp.workflow-modules' as well, and their description has to say where: "
            + notNamed);

  }

  @Test
  @DisplayName("every key described is read, and every key of version 1 is described as removed")
  public void everyDescribedKeyIsReadAndEveryOldKeyIsMarked() {

    final var notRead = new TreeSet<String>();
    final var gone = ConfigurationKeyCheck.keysOfVersionOne();
    metadata.forEach((
        name,
        entry) -> {
      if (!name.startsWith(GLOBAL_LEVEL)) {
        return;
      }
      final var key = name.substring(GLOBAL_LEVEL.length());
      if (isNoLongerSupported(entry)) {
        if (gone
            .stream()
            .noneMatch(old -> key.equals(old) || key.startsWith(old
                + "."))) {
          notRead.add(name
              + " (removed, but no key of version 1 which ConfigurationKeyCheck knows)");
        }
        return;
      }
      if (!keysBound.containsKey(name)) {
        notRead.add(name);
      }
    });
    assertTrue(
        notRead.isEmpty(),
        "the metadata describes keys the cockpit does not read: "
            + notRead);

    final var description = String.valueOf(metadata
        .get(WORKFLOW_MODULES)
        .getDescription());
    final var notMarked = new TreeSet<String>();
    gone.forEach(old -> {
      final var global = ConfigurationKeys.globalKey(old);
      final var marked = metadata
          .entrySet()
          .stream()
          .anyMatch(entry -> isNoLongerSupported(entry.getValue()) && (entry
              .getKey()
              .equals(global) || entry
                  .getKey()
                  .startsWith(global
                      + ".")));
      // a key version 1 read below a workflow module cannot get an entry of its own
      if (!marked && !description.contains("'"
          + old
          + "'")) {
        notMarked.add(old);
      }
    });
    assertTrue(
        notMarked.isEmpty(),
        "describe these keys of version 1 with a deprecation of level 'error', or name them in the description of '%s': %s"
            .formatted(WORKFLOW_MODULES, notMarked));

  }

  @Test
  @DisplayName("a default in the metadata is the value the cockpit starts with")
  public void everyDefaultIsTheRealOne() {

    // the least an application writes to start with both connections configured
    final var least = Map
        .of(
            ConfigurationKeys.globalKey(ConfigurationKeys.REST_BASE_URL), "http://localhost:8080",
            ConfigurationKeys.globalKey(ConfigurationKeys.REST_OAUTH_BASE_URL), "http://localhost:8081/token",
            ConfigurationKeys.globalKey(ConfigurationKeys.REST_OAUTH_CLIENT_ID), "a-client",
            ConfigurationKeys.globalKey(ConfigurationKeys.REST_OAUTH_CLIENT_SECRET), "a-secret",
            ConfigurationKeys.globalKey(ConfigurationKeys.INITIATOR_SOURCE), "system");
    final var withoutTheKey = whatTheCockpitReads(least);
    final var checked = new TreeSet<String>();
    metadata.forEach((
        name,
        entry) -> {
      if (!name.startsWith(GLOBAL_LEVEL) || (entry.getDefaultValue() == null) || name
          .equals(DEFAULT_OF_ANOTHER_REPOSITORY)) {
        return;
      }
      final var withTheDefault = new HashMap<>(least);
      withTheDefault.put(name, String.valueOf(entry.getDefaultValue()));
      assertEquals(
          withoutTheKey, whatTheCockpitReads(withTheDefault),
          "the cockpit reads the same with '%s: %s' as without the key, or the default in the metadata is wrong"
              .formatted(name, entry.getDefaultValue()));
      checked.add(name);
    });
    assertTrue(!checked.isEmpty(), "the metadata names no default at all");

  }

  /**
   * What the cockpit reads from these keys, bound the way the application binds them.
   */
  private static List<Object> whatTheCockpitReads(
      final Map<String, String> keys) {

    final var overlay = new Binder(new MapConfigurationPropertySource(keys))
        .bind(PREFIX, CockpitOverlayProperties.class)
        .get();
    final var configuration = BusinessCockpitConfiguration
        .readAndValidate(new MigrationAdapterProperties(), overlay.toSettings(), false, false);
    return List
        .of(
            configuration.isUserTasksEnabled(), configuration.isWorkflowListEnabled(),
            configuration.getRest());

  }

  private static boolean isNoLongerSupported(
      final ConfigurationMetadataProperty entry) {

    return (entry.getDeprecation() != null) && (entry
        .getDeprecation()
        .getLevel() == Deprecation.Level.ERROR);

  }

  /**
   * The keys bound onto one class, as the JavaBean binder of Spring Boot finds them: every
   * property with a getter and a setter. A map is a key of its own, and where its values are a
   * class of VanillaBP, it adds a level whose key the application chooses. A class of VanillaBP
   * which is not in a map adds a level named after the property.
   */
  private static void collectKeys(
      final String prefix,
      final Class<?> type,
      final Map<String, Type> keys) {

    for (final var property : bindableProperties(type)) {
      final var key = prefix
          + "."
          + DataObjectPropertyName.toDashedForm(property.getName());
      final var genericType = property
          .getReadMethod()
          .getGenericReturnType();
      if (Map.class.isAssignableFrom(property.getPropertyType())) {
        keys.put(key, genericType);
        if (isNested(valueTypeOf(genericType))) {
          collectKeys(key
              + ".*", valueTypeOf(genericType), keys);
        }
      } else if (isNested(property.getPropertyType())) {
        collectKeys(key, property.getPropertyType(), keys);
      } else {
        keys.put(key, genericType instanceof ParameterizedType
            ? genericType
            : ClassUtils.resolvePrimitiveIfNecessary(property.getPropertyType()));
      }
    }

  }

  private static List<PropertyDescriptor> bindableProperties(
      final Class<?> type) {

    final var result = new ArrayList<PropertyDescriptor>();
    for (final var property : BeanUtils.getPropertyDescriptors(type)) {
      if ((property.getWriteMethod() != null) && (property.getReadMethod() != null)) {
        result.add(property);
      }
    }
    return result;

  }

  private static boolean isNested(
      final Class<?> type) {

    return !type.isPrimitive() && !type.isEnum() && !Collection.class.isAssignableFrom(type) && type
        .getPackageName()
        .startsWith("io.vanillabp");

  }

  private static Class<?> valueTypeOf(
      final Type mapType) {

    final var valueType = ((ParameterizedType) mapType).getActualTypeArguments()[1];
    return valueType instanceof ParameterizedType parameterized
        ? (Class<?>) parameterized.getRawType()
        : (Class<?>) valueType;

  }

  /**
   * The processor writes a nested class with a dollar sign and a type argument list without
   * blanks. Java's own type names have a dot and a blank there.
   */
  private static String typeName(
      final Object type) {

    final var name = type instanceof Type javaType
        ? javaType.getTypeName()
        : String.valueOf(type);
    return name
        .replace('$', '.')
        .replace(" ", "");

  }

  private static boolean isBlank(
      final Object text) {

    return (text == null) || text
        .toString()
        .isBlank();

  }

  /**
   * The metadata of THIS module, which is the file next to the overlay class. The class path
   * of the test holds the platform's file as well. It is read with Spring Boot's own reader of
   * the format, which tools use as well.
   */
  private static Map<String, ConfigurationMetadataProperty> metadataOfThisModule() throws IOException {

    final var location = CockpitOverlayProperties.class
        .getProtectionDomain()
        .getCodeSource()
        .getLocation()
        .toString();
    final var files = EveryKeyIsKnownToTheIdeTest.class
        .getClassLoader()
        .getResources(METADATA);
    while (files.hasMoreElements()) {
      final var file = files.nextElement();
      if (!file
          .toString()
          .startsWith(location)) {
        continue;
      }
      try (InputStream in = file.openStream()) {
        return new TreeMap<>(ConfigurationMetadataRepositoryJsonBuilder
            .create(StandardCharsets.UTF_8)
            .withJsonResource(in)
            .build()
            .getAllProperties());
      }
    }
    throw new AssertionError("no %s next to %s, is the configuration processor configured?"
        .formatted(METADATA, location));

  }

}
