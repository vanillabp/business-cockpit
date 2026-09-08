package io.vanillabp.cockpit.extension.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vanillabp.integration.adapter.migration.config.MigrationAdapterProperties;
import io.vanillabp.integration.adapter.migration.config.WorkflowModuleAdapterProperties;

/**
 * Everything the Business Cockpit extension was configured with, read once at startup and
 * validated there rather than when the first event arrives.
 * <p>
 * The application is told about every gap it has in one boot: a transport which was not
 * chosen, a workflow module missing a setting, a value naming something which does not exist.
 * Each line names the property key to add, so the log is the documentation.
 */
public final class BusinessCockpitConfiguration {

  private static final Logger logger = LoggerFactory
      .getLogger(BusinessCockpitConfiguration.class);

  private final RestTransportConfiguration rest;

  private final KafkaTransportConfiguration kafka;

  private final String templateLoaderPath;

  private final Map<String, WorkflowModuleConfiguration> workflowModules;

  private BusinessCockpitConfiguration(
      final RestTransportConfiguration rest,
      final KafkaTransportConfiguration kafka,
      final String templateLoaderPath,
      final Map<String, WorkflowModuleConfiguration> workflowModules) {

    this.rest = rest;
    this.kafka = kafka;
    this.templateLoaderPath = templateLoaderPath;
    this.workflowModules = Map.copyOf(workflowModules);

  }

  /**
   * @return The REST transport's settings, or <code>null</code> where Kafka was chosen
   */
  public RestTransportConfiguration getRest() {

    return rest;

  }

  /**
   * @return The Kafka transport's settings, or <code>null</code> where REST was chosen
   */
  public KafkaTransportConfiguration getKafka() {

    return kafka;

  }

  /**
   * @return The directory templates are loaded from, or <code>null</code> where the
   *         application configured none and the cockpit reports the BPMN names instead
   */
  public String getTemplateLoaderPath() {

    return templateLoaderPath;

  }

  /**
   * @return Whether titles are rendered from templates rather than taken from the BPMN names
   */
  public boolean isTemplating() {

    return (templateLoaderPath != null) && !templateLoaderPath.isBlank();

  }

  /**
   * The settings of one workflow module.
   *
   * @param workflowModuleId The module
   * @return Its settings
   * @throws IllegalStateException If the module says nothing about the cockpit and therefore
   *           takes no part in it - the message names the module and the key which lets it in
   */
  public WorkflowModuleConfiguration workflowModule(
      final String workflowModuleId) {

    final var configuration = workflowModules.get(workflowModuleId);
    if (configuration == null) {
      throw new IllegalStateException(
          """
              The Business Cockpit extension has no settings for workflow module '%s', so the \
              module reports nothing to the cockpit. Reporting are: %s. To let this module report \
              as well, configure it below '%s', starting with '%s'."""
              .formatted(
                  workflowModuleId,
                  workflowModules.isEmpty() ? "none" : String.join(", ", workflowModules.keySet()),
                  ConfigurationKeys.workflowModuleKey(workflowModuleId, ""),
                  ConfigurationKeys
                      .workflowModuleKey(
                          workflowModuleId, ConfigurationKeys.WORKFLOW_MODULE_URI)));
    }
    return configuration;

  }

  /**
   * Whether a workflow module takes part in the Business Cockpit at all.
   *
   * @param workflowModuleId The module
   * @return Whether it configured the extension
   */
  public boolean reportsToTheCockpit(
      final String workflowModuleId) {

    return workflowModules.containsKey(workflowModuleId);

  }

  /**
   * Reads and validates the whole configuration.
   *
   * @param properties The core's resolved properties, which own the two locations an extension
   *          is configured in
   * @param templatingAvailable Whether a template engine is on the classpath at all - without
   *          one a template path is pointless and the BPMN language becomes mandatory
   * @return The configuration
   * @throws IllegalStateException If anything is missing or contradictory. The message lists
   *           every defect found, each with the key which fixes it
   */
  public static BusinessCockpitConfiguration readAndValidate(
      final MigrationAdapterProperties properties,
      final boolean templatingAvailable) {

    final var defects = new LinkedList<String>();
    final var global = properties.extensionProperties(null, ConfigurationKeys.EXTENSION_ID);

    final var rest = readRest(global);
    final var kafka = readKafka(global, defects);
    validateTransportChoice(rest, kafka, defects);

    final var templateLoaderPath = global.get(ConfigurationKeys.TEMPLATE_LOADER_PATH);
    final var templating = templatingAvailable && (templateLoaderPath != null) && !templateLoaderPath.isBlank();
    if (!templatingAvailable && (templateLoaderPath != null) && !templateLoaderPath.isBlank()) {
      defects.add(
          """
              '%s' is set to '%s' but no template engine is on the classpath, so nothing can be \
              rendered from it. Add the dependency 'org.freemarker:freemarker' to your workflow \
              module, or remove the property and let the cockpit report the names written in the \
              BPMN."""
              .formatted(
                  ConfigurationKeys.globalKey(ConfigurationKeys.TEMPLATE_LOADER_PATH),
                  templateLoaderPath));
    }

    final var modules = new LinkedHashMap<String, WorkflowModuleConfiguration>();
    properties
        .getWorkflowModules()
        .forEach((
            workflowModuleId,
            workflowModule) -> {
          if (saysNothingAboutTheCockpit(workflowModule)) {
            logger
                .info(
                    """
                        Workflow module '{}' reports nothing to the Business Cockpit: it configures \
                        none of the extension's settings. Set '{}' to let it report as well.""",
                    workflowModuleId,
                    ConfigurationKeys
                        .workflowModuleKey(
                            workflowModuleId, ConfigurationKeys.WORKFLOW_MODULE_URI));
            return;
          }
          modules
              .put(
                  workflowModuleId,
                  readWorkflowModule(
                      workflowModuleId,
                      properties
                          .extensionProperties(workflowModuleId, ConfigurationKeys.EXTENSION_ID),
                      templating,
                      defects));
        });

    if (!defects.isEmpty()) {
      throw new IllegalStateException(
          """
              The Business Cockpit extension is on the classpath but its configuration is \
              incomplete:
              %s"""
              .formatted(defects.stream().map("  - "::concat).reduce((
                  a,
                  b) -> a
                      + "\n"
                      + b)
                  .get()));
    }

    return new BusinessCockpitConfiguration(rest, kafka, templateLoaderPath, modules);

  }

  /**
   * Whether a workflow module wrote nothing at all below its own
   * <code>extensions.business-cockpit</code> section.
   * <p>
   * Only what the module itself says counts here, not what the application configured globally:
   * a module which reports to the cockpit says where it answers, and that address exists per
   * module and nowhere else. A module which says nothing is an application which uses the
   * cockpit for some of its modules and not for others, which is a choice rather than a defect.
   *
   * @param workflowModule The module's settings
   * @return Whether it opted out
   */
  private static boolean saysNothingAboutTheCockpit(
      final WorkflowModuleAdapterProperties workflowModule) {

    final var settings = workflowModule.getExtensions().get(ConfigurationKeys.EXTENSION_ID);
    return (settings == null) || settings.isEmpty();

  }

  private static RestTransportConfiguration readRest(
      final Map<String, String> global) {

    final var baseUrl = global.get(ConfigurationKeys.REST_BASE_URL);
    if ((baseUrl == null) || baseUrl.isBlank()) {
      return null;
    }
    return new RestTransportConfiguration(
        baseUrl, global.get(ConfigurationKeys.REST_USERNAME), global.get(ConfigurationKeys.REST_PASSWORD));

  }

  private static KafkaTransportConfiguration readKafka(
      final Map<String, String> global,
      final List<String> defects) {

    final var bootstrapServers = global.get(ConfigurationKeys.KAFKA_BOOTSTRAP_SERVERS);
    if ((bootstrapServers == null) || bootstrapServers.isBlank()) {
      return null;
    }
    final var missingTopics = new ArrayList<String>();
    final var userTask = requiredTopic(
        global, ConfigurationKeys.KAFKA_TOPIC_USER_TASK, missingTopics);
    final var workflow = requiredTopic(
        global, ConfigurationKeys.KAFKA_TOPIC_WORKFLOW, missingTopics);
    final var workflowModule = requiredTopic(
        global, ConfigurationKeys.KAFKA_TOPIC_WORKFLOW_MODULE, missingTopics);
    if (!missingTopics.isEmpty()) {
      defects.add(
          """
              The Kafka transport was chosen by '%s' but these topics are missing: %s. The cockpit \
              server reads one topic per kind of event, so all three are needed."""
              .formatted(
                  ConfigurationKeys.globalKey(ConfigurationKeys.KAFKA_BOOTSTRAP_SERVERS),
                  String.join(", ", missingTopics)));
    }
    final var producerProperties = new LinkedHashMap<String, String>();
    global
        .forEach((
            key,
            value) -> {
          if (key.startsWith(ConfigurationKeys.KAFKA_PROPERTIES_PREFIX)) {
            producerProperties
                .put(key.substring(ConfigurationKeys.KAFKA_PROPERTIES_PREFIX.length()), value);
          }
        });
    return new KafkaTransportConfiguration(
        bootstrapServers, userTask, workflow, workflowModule, producerProperties);

  }

  private static String requiredTopic(
      final Map<String, String> global,
      final String key,
      final List<String> missing) {

    final var value = global.get(key);
    if ((value == null) || value.isBlank()) {
      missing.add(ConfigurationKeys.globalKey(key));
      return null;
    }
    return value;

  }

  private static void validateTransportChoice(
      final RestTransportConfiguration rest,
      final KafkaTransportConfiguration kafka,
      final List<String> defects) {

    if ((rest == null) && (kafka == null)) {
      defects.add(
          """
              No transport to the cockpit server is configured. Set either
                  %s: http://localhost:8080
              to report events over REST, or
                  %s: localhost:9092
                  %s, %s, %s
              to report them over Kafka."""
              .formatted(
                  ConfigurationKeys.globalKey(ConfigurationKeys.REST_BASE_URL),
                  ConfigurationKeys.globalKey(ConfigurationKeys.KAFKA_BOOTSTRAP_SERVERS),
                  ConfigurationKeys.globalKey(ConfigurationKeys.KAFKA_TOPIC_USER_TASK),
                  ConfigurationKeys.globalKey(ConfigurationKeys.KAFKA_TOPIC_WORKFLOW),
                  ConfigurationKeys.globalKey(ConfigurationKeys.KAFKA_TOPIC_WORKFLOW_MODULE)));
      return;
    }
    if ((rest != null) && (kafka != null)) {
      defects.add(
          """
              Both transports to the cockpit server are configured, and events would be reported \
              twice. Remove either '%s' or '%s'."""
              .formatted(
                  ConfigurationKeys.globalKey(ConfigurationKeys.REST_BASE_URL),
                  ConfigurationKeys.globalKey(ConfigurationKeys.KAFKA_BOOTSTRAP_SERVERS)));
    }

  }

  private static WorkflowModuleConfiguration readWorkflowModule(
      final String workflowModuleId,
      final Map<String, String> settings,
      final boolean templating,
      final List<String> defects) {

    final var workflowModuleUri = required(
        workflowModuleId, settings, ConfigurationKeys.WORKFLOW_MODULE_URI, defects,
        "It is the address the cockpit server calls back for user-task forms and workflow pages.");
    final var uiUriPath = required(
        workflowModuleId, settings, ConfigurationKeys.UI_URI_PATH, defects,
        "It is the path below the module's URI the forms are served from.");
    final var i18nLanguages = required(
        workflowModuleId, settings, ConfigurationKeys.I18N_LANGUAGES, defects,
        "Write the languages titles are reported in, comma separated, e.g. 'en,de'.");
    final var bpmnDescriptionLanguage = templating
        ? settings.get(ConfigurationKeys.BPMN_DESCRIPTION_LANGUAGE)
        : required(
            workflowModuleId, settings, ConfigurationKeys.BPMN_DESCRIPTION_LANGUAGE, defects,
            """
                Without templates the cockpit reports the names written in the BPMN files, and it \
                has to know which language they are in. Configure '%s' instead to render titles \
                from templates."""
                .formatted(ConfigurationKeys.globalKey(ConfigurationKeys.TEMPLATE_LOADER_PATH)));

    final var uiUriTypeValue = required(
        workflowModuleId, settings, ConfigurationKeys.UI_URI_TYPE, defects,
        "Valid values are: %s.".formatted(String.join(", ", UiUriType.names())));
    UiUriType uiUriType = null;
    if (uiUriTypeValue != null) {
      try {
        uiUriType = UiUriType.of(uiUriTypeValue);
      } catch (final IllegalArgumentException e) {
        defects
            .add(
                "'%s' is '%s'. %s"
                    .formatted(
                        ConfigurationKeys
                            .workflowModuleKey(workflowModuleId, ConfigurationKeys.UI_URI_TYPE),
                        uiUriTypeValue,
                        e.getMessage()));
      }
    }

    final var groupHierarchy = new LinkedHashMap<String, Collection<String>>();
    settings
        .forEach((
            key,
            value) -> {
          if (key.startsWith(ConfigurationKeys.GROUP_HIERARCHY_PREFIX)) {
            groupHierarchy
                .put(
                    key.substring(ConfigurationKeys.GROUP_HIERARCHY_PREFIX.length()),
                    splitList(value));
          }
        });

    final var templatePath = settings.get(ConfigurationKeys.TEMPLATE_PATH);

    return new WorkflowModuleConfiguration(
        workflowModuleId, workflowModuleUri, uiUriType, uiUriPath, i18nLanguages == null ? List.of() : List.copyOf(
            splitList(i18nLanguages)), bpmnDescriptionLanguage, groupHierarchy, (templatePath == null) || templatePath
                .isBlank() ? workflowModuleId : templatePath);

  }

  private static String required(
      final String workflowModuleId,
      final Map<String, String> settings,
      final String key,
      final List<String> defects,
      final String why) {

    final var value = settings.get(key);
    if ((value != null) && !value.isBlank()) {
      return value;
    }
    defects
        .add(
            "'%s' is missing. %s".formatted(
                ConfigurationKeys.workflowModuleKey(workflowModuleId, key), why));
    return null;

  }

  /**
   * Reads a comma separated value the way both platforms write a list into one property.
   *
   * @param value The configured value
   * @return The entries, trimmed and without the empty ones
   */
  public static List<String> splitList(
      final String value) {

    if ((value == null) || value.isBlank()) {
      return List.of();
    }
    return Arrays
        .stream(value.split(","))
        .map(String::trim)
        .filter(entry -> !entry.isEmpty())
        .toList();

  }

}
