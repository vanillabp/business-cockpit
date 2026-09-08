package io.vanillabp.cockpit.extension.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

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

  private final Collection<String> configuredAdapterIds;

  private BusinessCockpitConfiguration(
      final RestTransportConfiguration rest,
      final KafkaTransportConfiguration kafka,
      final String templateLoaderPath,
      final Map<String, WorkflowModuleConfiguration> workflowModules,
      final Collection<String> configuredAdapterIds) {

    this.rest = rest;
    this.kafka = kafka;
    this.templateLoaderPath = templateLoaderPath;
    this.workflowModules = Map.copyOf(workflowModules);
    this.configuredAdapterIds = List.copyOf(configuredAdapterIds);

  }

  /**
   * The adapters the application configured, which is what a report can come from. Kept
   * because a dispatch naming an adapter no BPMS half serves is told which adapters exist at
   * all - a half missing from the classpath and an adapter id misspelled in the configuration
   * look the same otherwise.
   *
   * @return The configured adapter ids
   */
  public Collection<String> getConfiguredAdapterIds() {

    return configuredAdapterIds;

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

    final var rest = readRest(global, defects);
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

    return new BusinessCockpitConfiguration(
        rest, kafka, templateLoaderPath, modules, properties.adapterTypes().keySet());

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
      final Map<String, String> global,
      final List<String> defects) {

    final var baseUrl = global.get(ConfigurationKeys.REST_BASE_URL);
    if ((baseUrl == null) || baseUrl.isBlank()) {
      return null;
    }
    final var connectTimeout = duration(global, ConfigurationKeys.REST_CONNECT_TIMEOUT, defects);
    final var readTimeout = duration(global, ConfigurationKeys.REST_READ_TIMEOUT, defects);
    final var proxy = readProxy(global, defects);
    final var verifySsl = flag(global, ConfigurationKeys.REST_VERIFY_SSL, true, defects);
    final var truststore = readTruststore(global, defects);
    final var oauth = readOauth(global, defects);
    return new RestTransportConfiguration(
        baseUrl, global.get(ConfigurationKeys.REST_USERNAME), global
            .get(ConfigurationKeys.REST_PASSWORD), connectTimeout, readTimeout, proxy, verifySsl, truststore, global
                .get(ConfigurationKeys.REST_SSL_TRUSTSTORE_PASSWORD), oauth);

  }

  /**
   * The proxy the cockpit server is reached through, where one was configured.
   * <p>
   * The host is what switches the proxy on, the way the base URL switches the REST transport
   * on: a port without a host configures nothing and is more likely a leftover than a wish, so
   * it is reported rather than ignored.
   */
  private static RestTransportConfiguration.Proxy readProxy(
      final Map<String, String> global,
      final List<String> defects) {

    final var host = global.get(ConfigurationKeys.REST_PROXY_HOST);
    final var port = global.get(ConfigurationKeys.REST_PROXY_PORT);
    if ((host == null) || host.isBlank()) {
      if ((port != null) && !port.isBlank()) {
        defects
            .add(
                """
                    '%s' is set to '%s' but '%s' is missing, so no proxy is used at all. Name the \
                    proxy's host as well, or remove the port."""
                    .formatted(
                        ConfigurationKeys.globalKey(ConfigurationKeys.REST_PROXY_PORT), port,
                        ConfigurationKeys.globalKey(ConfigurationKeys.REST_PROXY_HOST)));
      }
      return null;
    }
    if ((port == null) || port.isBlank()) {
      defects
          .add(
              """
                  '%s' is missing. The proxy at '%s' is reached on a port, and there is no port a \
                  proxy has by convention."""
                  .formatted(
                      ConfigurationKeys.globalKey(ConfigurationKeys.REST_PROXY_PORT), host));
      return null;
    }
    final int number;
    try {
      number = Integer.parseInt(port.trim());
    } catch (final NumberFormatException e) {
      defects
          .add(
              "'%s' is '%s', which is no port number."
                  .formatted(
                      ConfigurationKeys.globalKey(ConfigurationKeys.REST_PROXY_PORT), port));
      return null;
    }
    return new RestTransportConfiguration.Proxy(
        host, number, global.get(ConfigurationKeys.REST_PROXY_USERNAME), global
            .get(ConfigurationKeys.REST_PROXY_PASSWORD));

  }

  /**
   * The file holding the certificates the cockpit server's is checked against.
   * <p>
   * The file is opened while the application starts rather than at the first report: a
   * truststore which cannot be read is a deployment which was assembled wrongly, and finding
   * that out when the first user task is reported means finding it out in production.
   */
  private static String readTruststore(
      final Map<String, String> global,
      final List<String> defects) {

    final var filename = global.get(ConfigurationKeys.REST_SSL_TRUSTSTORE_FILENAME);
    if ((filename == null) || filename.isBlank()) {
      if ((global.get(ConfigurationKeys.REST_SSL_TRUSTSTORE_PASSWORD) != null)) {
        defects
            .add(
                """
                    '%s' is set but '%s' is missing, so nothing is loaded with that password. Name \
                    the truststore as well, or remove the password."""
                    .formatted(
                        ConfigurationKeys
                            .globalKey(ConfigurationKeys.REST_SSL_TRUSTSTORE_PASSWORD),
                        ConfigurationKeys
                            .globalKey(ConfigurationKeys.REST_SSL_TRUSTSTORE_FILENAME)));
      }
      return null;
    }
    if (!Files.isReadable(Path.of(filename))) {
      defects
          .add(
              """
                  '%s' names '%s', which this application cannot read. Write the path the truststore \
                  has in the running container, not the one it has in your project."""
                  .formatted(
                      ConfigurationKeys.globalKey(ConfigurationKeys.REST_SSL_TRUSTSTORE_FILENAME),
                      filename));
      return null;
    }
    return filename;

  }

  /**
   * The client-credentials flow, where one was configured. The address of the authorization
   * server switches it on, and the two halves of the client's identity are needed with it.
   */
  private static RestTransportConfiguration.OAuth readOauth(
      final Map<String, String> global,
      final List<String> defects) {

    final var tokenUrl = global.get(ConfigurationKeys.REST_OAUTH_BASE_URL);
    final var clientId = global.get(ConfigurationKeys.REST_OAUTH_CLIENT_ID);
    final var clientSecret = global.get(ConfigurationKeys.REST_OAUTH_CLIENT_SECRET);
    if ((tokenUrl == null) || tokenUrl.isBlank()) {
      if (((clientId != null) && !clientId.isBlank()) || ((clientSecret != null) && !clientSecret.isBlank())) {
        defects
            .add(
                """
                    A client of the Business Cockpit's authorization server is configured but '%s' is \
                    missing, so no token is ever fetched. Name the address tokens are issued under."""
                    .formatted(ConfigurationKeys.globalKey(ConfigurationKeys.REST_OAUTH_BASE_URL)));
      }
      return null;
    }
    final var missing = new ArrayList<String>();
    if ((clientId == null) || clientId.isBlank()) {
      missing.add(ConfigurationKeys.globalKey(ConfigurationKeys.REST_OAUTH_CLIENT_ID));
    }
    if ((clientSecret == null) || clientSecret.isBlank()) {
      missing.add(ConfigurationKeys.globalKey(ConfigurationKeys.REST_OAUTH_CLIENT_SECRET));
    }
    if (!missing.isEmpty()) {
      defects
          .add(
              """
                  The client-credentials flow to '%s' is configured without %s. There is no user in \
                  this flow: the workflow module is the client, and it identifies itself with both \
                  halves."""
                  .formatted(tokenUrl, String.join(" and without ", missing)));
      return null;
    }
    if ((global.get(ConfigurationKeys.REST_USERNAME) != null)) {
      defects
          .add(
              """
                  '%s' and '%s' are both configured, so it is undecided whether the cockpit server is \
                  called with a basic authentication or with a bearer token. Remove one of them."""
                  .formatted(
                      ConfigurationKeys.globalKey(ConfigurationKeys.REST_USERNAME),
                      ConfigurationKeys.globalKey(ConfigurationKeys.REST_OAUTH_BASE_URL)));
    }
    return new RestTransportConfiguration.OAuth(
        tokenUrl, clientId, clientSecret, flag(global, ConfigurationKeys.REST_OAUTH_BASIC, false, defects));

  }

  /**
   * A span of time as both platforms let one be written: ISO-8601 (<code>PT1.5S</code>) and the
   * shorter spelling a developer expects from Spring Boot and from Quarkus
   * (<code>1500ms</code>, <code>10s</code>).
   */
  private static Duration duration(
      final Map<String, String> settings,
      final String key,
      final List<String> defects) {

    final var value = settings.get(key);
    if ((value == null) || value.isBlank()) {
      return null;
    }
    try {
      return parseDuration(value.trim());
    } catch (final RuntimeException e) {
      defects
          .add(
              """
                  '%s' is '%s', which is no span of time. Write it as ISO-8601 ('PT10S') or with a \
                  unit ('10s', '1500ms', '2m')."""
                  .formatted(ConfigurationKeys.globalKey(key), value));
      return null;
    }

  }

  private static Duration parseDuration(
      final String value) {

    final var lower = value.toLowerCase(Locale.ROOT);
    if (lower.startsWith("p")) {
      return Duration.parse(lower);
    }
    for (final var unit : new String[]{
        "ms", "s", "m", "h"
    }) {
      if (lower.endsWith(unit)) {
        final var amount = Long.parseLong(lower.substring(0, lower.length() - unit.length()).trim());
        return switch (unit) {
          case "ms" -> Duration.ofMillis(amount);
          case "s" -> Duration.ofSeconds(amount);
          case "m" -> Duration.ofMinutes(amount);
          default -> Duration.ofHours(amount);
        };
      }
    }
    throw new DateTimeParseException("no unit and no ISO-8601 spelling", value, 0);

  }

  private static boolean flag(
      final Map<String, String> settings,
      final String key,
      final boolean whenNothingIsConfigured,
      final List<String> defects) {

    final var value = settings.get(key);
    if ((value == null) || value.isBlank()) {
      return whenNothingIsConfigured;
    }
    if ("true".equalsIgnoreCase(value.trim())) {
      return true;
    }
    if ("false".equalsIgnoreCase(value.trim())) {
      return false;
    }
    defects
        .add(
            "'%s' is '%s'. Write 'true' or 'false'."
                .formatted(ConfigurationKeys.globalKey(key), value));
    return whenNothingIsConfigured;

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
                .isBlank() ? workflowModuleId : templatePath, readWorkflows(workflowModuleId, settings, defects));

  }

  /**
   * What single workflows of a module, and single user tasks of those workflows, said about the
   * cockpit.
   * <p>
   * Only three of the module's keys mean anything one workflow at a time - the language its
   * titles are written in, the languages they are reported in, and the directory its templates
   * live in - and only the last of them means anything for a single user task. A key which
   * stands below <code>workflows</code> and is none of those is reported: it looks like a
   * setting and is read by nobody, which is the kind of configuration a developer stares at for
   * an afternoon.
   * <p>
   * The key is read from its END rather than by counting dots, because a BPMN process id and a
   * task definition may contain dots themselves and both platforms hand the section over as one
   * flat map.
   */
  private static Map<String, WorkflowConfiguration> readWorkflows(
      final String workflowModuleId,
      final Map<String, String> settings,
      final List<String> defects) {

    final var ofTheWorkflows = new LinkedHashMap<String, Map<String, String>>();
    final var ofTheUserTasks = new LinkedHashMap<String, Map<String, Map<String, String>>>();
    settings
        .forEach((
            key,
            value) -> {
          if (!key.startsWith(ConfigurationKeys.WORKFLOWS_PREFIX)) {
            return;
          }
          final var belowTheWorkflows = key
              .substring(ConfigurationKeys.WORKFLOWS_PREFIX.length());
          final var userTasks = belowTheWorkflows
              .lastIndexOf(ConfigurationKeys.USER_TASKS_INFIX);
          if (userTasks < 0) {
            final var setting = endingIn(
                belowTheWorkflows, ConfigurationKeys.KEYS_OF_A_WORKFLOW);
            if (setting == null) {
              defects
                  .add(
                      """
                          '%s' is no setting a single workflow has. A workflow may differ from its \
                          workflow module in %s; everything else the Business Cockpit reads is \
                          configured for the whole module."""
                          .formatted(
                              ConfigurationKeys.workflowModuleKey(workflowModuleId, key),
                              String.join(", ", ConfigurationKeys.KEYS_OF_A_WORKFLOW)));
              return;
            }
            ofTheWorkflows
                .computeIfAbsent(
                    withoutTheSetting(belowTheWorkflows, setting),
                    ignored -> new LinkedHashMap<>())
                .put(setting, value);
            return;
          }
          final var belowTheUserTasks = belowTheWorkflows
              .substring(userTasks + ConfigurationKeys.USER_TASKS_INFIX.length());
          final var setting = endingIn(belowTheUserTasks, ConfigurationKeys.KEYS_OF_A_USER_TASK);
          if (setting == null) {
            defects
                .add(
                    """
                        '%s' is no setting a single user task has. A user task may differ from its \
                        workflow in %s and in nothing else."""
                        .formatted(
                            ConfigurationKeys.workflowModuleKey(workflowModuleId, key),
                            String.join(", ", ConfigurationKeys.KEYS_OF_A_USER_TASK)));
            return;
          }
          ofTheUserTasks
              .computeIfAbsent(
                  belowTheWorkflows.substring(0, userTasks), ignored -> new LinkedHashMap<>())
              .computeIfAbsent(
                  withoutTheSetting(belowTheUserTasks, setting),
                  ignored -> new LinkedHashMap<>())
              .put(setting, value);
        });

    final var workflows = new LinkedHashMap<String, WorkflowConfiguration>();
    Stream
        .concat(ofTheWorkflows.keySet().stream(), ofTheUserTasks.keySet().stream())
        .distinct()
        .forEach(bpmnProcessId -> {
          final var ofTheWorkflow = ofTheWorkflows.getOrDefault(bpmnProcessId, Map.of());
          final var templatePathPerUserTask = new LinkedHashMap<String, String>();
          ofTheUserTasks
              .getOrDefault(bpmnProcessId, Map.of())
              .forEach((
                  taskDefinition,
                  ofTheTask) -> templatePathPerUserTask
                      .put(taskDefinition, ofTheTask.get(ConfigurationKeys.TEMPLATE_PATH)));
          final var i18nLanguages = ofTheWorkflow.get(ConfigurationKeys.I18N_LANGUAGES);
          workflows
              .put(
                  bpmnProcessId,
                  new WorkflowConfiguration(
                      bpmnProcessId, i18nLanguages == null ? null : splitList(i18nLanguages), ofTheWorkflow
                          .get(ConfigurationKeys.BPMN_DESCRIPTION_LANGUAGE), ofTheWorkflow
                              .get(ConfigurationKeys.TEMPLATE_PATH), templatePathPerUserTask));
        });
    return workflows;

  }

  /**
   * @param key What stands below a workflow or below a user task
   * @param settings The keys that level owns
   * @return The one it ends in, or <code>null</code> where it ends in none of them
   */
  private static String endingIn(
      final String key,
      final List<String> settings) {

    return settings
        .stream()
        .filter(setting -> key.endsWith("."
            + setting))
        .findFirst()
        .orElse(null);

  }

  /**
   * @param key What stands below a workflow or below a user task
   * @param setting The setting it ends in
   * @return What is left, which is the BPMN process id respectively the task definition
   */
  private static String withoutTheSetting(
      final String key,
      final String setting) {

    return key.substring(0, key.length() - setting.length() - 1);

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
