package io.vanillabp.cockpit.extension.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vanillabp.integration.adapter.migration.config.MigrationAdapterProperties;

/**
 * Everything the Business Cockpit extension was configured with, read once at startup and
 * validated there rather than when the first event arrives.
 * <p>
 * The application is told about every gap it has in one boot: a transport which was not
 * chosen, a workflow module missing a setting, a value naming something which does not exist.
 * Each line names the property key to add, so the log is the documentation.
 * <p>
 * What is read is the tree {@link CockpitSettings} carries, which both platforms bind with
 * their own means and hand over as one object. Every value in it is the text the application
 * wrote, so a number which is none, a span of time which is none and a boolean which is none
 * are answered here - once, and with the same words on both platforms.
 */
public final class BusinessCockpitConfiguration {

  private static final Logger logger = LoggerFactory
      .getLogger(BusinessCockpitConfiguration.class);

  private final boolean userTasksEnabled;

  private final boolean workflowListEnabled;

  private final RestTransportConfiguration rest;

  private final KafkaTransportConfiguration kafka;

  private final String templateLoaderPath;

  private final Map<String, WorkflowModuleConfiguration> workflowModules;

  private final Collection<String> configuredAdapterIds;

  private BusinessCockpitConfiguration(
      final boolean userTasksEnabled,
      final boolean workflowListEnabled,
      final RestTransportConfiguration rest,
      final KafkaTransportConfiguration kafka,
      final String templateLoaderPath,
      final Map<String, WorkflowModuleConfiguration> workflowModules,
      final Collection<String> configuredAdapterIds) {

    this.userTasksEnabled = userTasksEnabled;
    this.workflowListEnabled = workflowListEnabled;
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
   * @return Whether user tasks are reported at all, which an application switches off with
   *         <code>vanillabp.cockpit.user-tasks-enabled</code>
   */
  public boolean isUserTasksEnabled() {

    return userTasksEnabled;

  }

  /**
   * @return Whether workflows are reported at all, which an application switches off with
   *         <code>vanillabp.cockpit.workflow-list-enabled</code>
   */
  public boolean isWorkflowListEnabled() {

    return workflowListEnabled;

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
   * @param properties The core's resolved properties, which know the workflow modules of the
   *          application and the adapters it configured
   * @param settings What the application wrote below <code>vanillabp.cockpit</code> and below
   *          the <code>cockpit</code> sections of its workflow modules
   * @param templatingAvailable Whether a template engine is on the classpath at all - without
   *          one a template path is pointless and the BPMN language becomes mandatory
   * @return The configuration
   * @throws IllegalStateException If anything is missing or contradictory. The message lists
   *           every defect found, each with the key which fixes it
   */
  public static BusinessCockpitConfiguration readAndValidate(
      final MigrationAdapterProperties properties,
      final CockpitSettings settings,
      final boolean templatingAvailable) {

    final var defects = new LinkedList<String>();

    final var rest = readRest(settings.rest(), defects);
    final var kafka = readKafka(settings.kafka(), defects);
    validateTransportChoice(rest, kafka, defects);

    final var userTasksEnabled = flag(
        settings.userTasksEnabled(), ConfigurationKeys.USER_TASKS_ENABLED, true, defects);
    final var workflowListEnabled = flag(
        settings.workflowListEnabled(), ConfigurationKeys.WORKFLOW_LIST_ENABLED, true, defects);

    final var templateLoaderPath = settings.templateLoaderPath();
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
        .keySet()
        .forEach(workflowModuleId -> {
          final var workflowModule = settings.workflowModule(workflowModuleId);
          if ((workflowModule == null) || workflowModule.saysNothing()) {
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
                  readWorkflowModule(workflowModuleId, workflowModule, templating, defects));
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

    if (!userTasksEnabled) {
      logger
          .info(
              "The Business Cockpit reports no user task: '{}' is false",
              ConfigurationKeys.globalKey(ConfigurationKeys.USER_TASKS_ENABLED));
    }
    if (!workflowListEnabled) {
      logger
          .info(
              "The Business Cockpit reports no workflow: '{}' is false",
              ConfigurationKeys.globalKey(ConfigurationKeys.WORKFLOW_LIST_ENABLED));
    }

    return new BusinessCockpitConfiguration(
        userTasksEnabled, workflowListEnabled, rest, kafka, templateLoaderPath, modules, properties
            .adapterTypes()
            .keySet());

  }

  private static RestTransportConfiguration readRest(
      final CockpitSettings.Rest rest,
      final List<String> defects) {

    if (rest == null) {
      return null;
    }
    final var baseUrl = rest.baseUrl();
    if ((baseUrl == null) || baseUrl.isBlank()) {
      return null;
    }
    final var authentication = rest.authentication() == null
        ? new CockpitSettings.Authentication(null, null, null, null)
        : rest.authentication();
    final var basic = readBasicAuthentication(authentication, defects);
    final var oauth = readOauth(authentication, basic, defects);
    return new RestTransportConfiguration(
        baseUrl, basic ? authentication.username() : null, basic ? authentication.password() : null, duration(
            rest.connectTimeout(), ConfigurationKeys.REST_CONNECT_TIMEOUT, RestConnection.DEFAULT_CONNECT_TIMEOUT,
            defects), duration(
                rest.readTimeout(), ConfigurationKeys.REST_READ_TIMEOUT, RestConnection.DEFAULT_READ_TIMEOUT,
                defects), readProxy(rest.proxy(), ConfigurationKeys.REST_PREFIX, defects), flag(rest.verifySsl(),
                    ConfigurationKeys.REST_VERIFY_SSL, true, defects), readTruststore(
                        rest.sslTruststoreFilename(), rest.sslTruststorePassword(),
                        ConfigurationKeys.REST_PREFIX, defects), rest.sslTruststorePassword(), oauth);

  }

  /**
   * Whether requests carry a basic authentication, which is what version 1's switch said and
   * says here.
   * <p>
   * A user name next to a switch which is off, and a switch which is on next to no user name,
   * are both reported: each of them is a deployment which believes it authenticates and does
   * not, and finding that out means reading the cockpit server's log rather than one's own.
   */
  private static boolean readBasicAuthentication(
      final CockpitSettings.Authentication authentication,
      final List<String> defects) {

    final var basic = flag(
        authentication.basic(), ConfigurationKeys.REST_BASIC, false, defects);
    final var username = authentication.username();
    final var hasUsername = (username != null) && !username.isBlank();
    if (basic && !hasUsername) {
      defects
          .add(
              """
                  '%s' is true but '%s' is missing, so nothing is sent to identify this workflow \
                  module. Name the user the cockpit server expects, or switch the basic \
                  authentication off."""
                  .formatted(
                      ConfigurationKeys.globalKey(ConfigurationKeys.REST_BASIC),
                      ConfigurationKeys.globalKey(ConfigurationKeys.REST_USERNAME)));
      return false;
    }
    if (!basic && hasUsername) {
      defects
          .add(
              """
                  '%s' is set to '%s' but '%s' is not true, so no authentication is sent at all. \
                  Switch the basic authentication on, or remove the user."""
                  .formatted(
                      ConfigurationKeys.globalKey(ConfigurationKeys.REST_USERNAME), username,
                      ConfigurationKeys.globalKey(ConfigurationKeys.REST_BASIC)));
      return false;
    }
    return basic;

  }

  /**
   * The proxy a server is reached through, where one was configured.
   * <p>
   * The host is what switches the proxy on, the way the base URL switches the REST transport
   * on: a port without a host configures nothing and is more likely a leftover than a wish, so
   * it is reported rather than ignored.
   *
   * @param proxy What was written below the connection's <code>proxy</code>
   * @param prefix Which connection this is, which is what a message names
   */
  private static RestTransportConfiguration.Proxy readProxy(
      final CockpitSettings.Proxy proxy,
      final String prefix,
      final List<String> defects) {

    final var host = proxy == null ? null : proxy.host();
    final var port = proxy == null ? null : proxy.port();
    if ((host == null) || host.isBlank()) {
      if ((port != null) && !port.isBlank()) {
        defects
            .add(
                """
                    '%s' is set to '%s' but '%s' is missing, so no proxy is used at all. Name the \
                    proxy's host as well, or remove the port."""
                    .formatted(
                        ConfigurationKeys.globalKey(prefix + ConfigurationKeys.PROXY_PORT), port,
                        ConfigurationKeys.globalKey(prefix + ConfigurationKeys.PROXY_HOST)));
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
                      ConfigurationKeys.globalKey(prefix + ConfigurationKeys.PROXY_PORT), host));
      return null;
    }
    final var noPortNumber = "'%s' is '%s', which is no port number: a port is between 1 and 65535."
        .formatted(ConfigurationKeys.globalKey(prefix + ConfigurationKeys.PROXY_PORT), port);
    final int number;
    try {
      number = Integer.parseInt(port.trim());
    } catch (final NumberFormatException e) {
      defects.add(noPortNumber);
      return null;
    }
    if ((number < 1) || (number > 65535)) {
      defects.add(noPortNumber);
      return null;
    }
    return new RestTransportConfiguration.Proxy(
        host, number, proxy.username(), proxy.password());

  }

  /**
   * The file holding the certificates a server's is checked against.
   * <p>
   * The file is opened while the application starts rather than at the first report: a
   * truststore which cannot be read is a deployment which was assembled wrongly, and finding
   * that out when the first user task is reported means finding it out in production.
   */
  private static String readTruststore(
      final String filename,
      final String password,
      final String prefix,
      final List<String> defects) {

    if ((filename == null) || filename.isBlank()) {
      if ((password != null) && !password.isBlank()) {
        defects
            .add(
                """
                    '%s' is set but '%s' is missing, so nothing is loaded with that password. Name \
                    the truststore as well, or remove the password."""
                    .formatted(
                        ConfigurationKeys.globalKey(prefix + ConfigurationKeys.SSL_TRUSTSTORE_PASSWORD),
                        ConfigurationKeys.globalKey(prefix + ConfigurationKeys.SSL_TRUSTSTORE_FILENAME)));
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
                      ConfigurationKeys.globalKey(prefix + ConfigurationKeys.SSL_TRUSTSTORE_FILENAME),
                      filename));
      return null;
    }
    return filename;

  }

  /**
   * The client-credentials flow, where one was configured. The address of the authorization
   * server switches it on, and the two halves of the client's identity are needed with it.
   * <p>
   * What the flow says about its own connection is its own: version 1 configured the token
   * client separately from the cockpit server's, and an authorization server behind another
   * proxy stays reachable because of it.
   */
  private static RestTransportConfiguration.OAuth readOauth(
      final CockpitSettings.Authentication authentication,
      final boolean basic,
      final List<String> defects) {

    final var oauth = authentication.oauth();
    if (oauth == null) {
      return null;
    }
    final var tokenUrl = oauth.baseUrl();
    final var clientId = oauth.clientId();
    final var clientSecret = oauth.clientSecret();
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
    if (basic) {
      defects
          .add(
              """
                  '%s' and '%s' are both configured, so it is undecided whether the cockpit server is \
                  called with a basic authentication or with a bearer token. Remove one of them."""
                  .formatted(
                      ConfigurationKeys.globalKey(ConfigurationKeys.REST_BASIC),
                      ConfigurationKeys.globalKey(ConfigurationKeys.REST_OAUTH_BASE_URL)));
    }
    return new RestTransportConfiguration.OAuth(
        tokenUrl, clientId, clientSecret, flag(oauth.basic(), ConfigurationKeys.REST_OAUTH_BASIC, false,
            defects), duration(
                oauth.connectTimeout(), ConfigurationKeys.REST_OAUTH_PREFIX + ConfigurationKeys.CONNECT_TIMEOUT,
                RestConnection.DEFAULT_CONNECT_TIMEOUT, defects), duration(
                    oauth.readTimeout(), ConfigurationKeys.REST_OAUTH_PREFIX + ConfigurationKeys.READ_TIMEOUT,
                    RestConnection.DEFAULT_READ_TIMEOUT,
                    defects), readProxy(oauth.proxy(), ConfigurationKeys.REST_OAUTH_PREFIX, defects), flag(
                        oauth.verifySsl(), ConfigurationKeys.REST_OAUTH_PREFIX + ConfigurationKeys.VERIFY_SSL, true,
                        defects), readTruststore(
                            oauth.sslTruststoreFilename(), oauth.sslTruststorePassword(),
                            ConfigurationKeys.REST_OAUTH_PREFIX, defects), oauth.sslTruststorePassword());

  }

  /**
   * A span of time as both platforms let one be written: the number of milliseconds version 1
   * expected (<code>1500</code>), ISO-8601 (<code>PT1.5S</code>) and the shorter spelling a
   * developer expects from Spring Boot and from Quarkus (<code>1500ms</code>, <code>10s</code>).
   *
   * @param whenNothingIsConfigured What version 1 waited, which an application saying nothing
   *          keeps
   */
  private static Duration duration(
      final String value,
      final String key,
      final Duration whenNothingIsConfigured,
      final List<String> defects) {

    if ((value == null) || value.isBlank()) {
      return whenNothingIsConfigured;
    }
    try {
      final var span = parseDuration(value.trim());
      if (span.isNegative() || span.isZero()) {
        defects
            .add(
                """
                    '%s' is '%s'. A client which waits no time at all reaches nothing, so write a span \
                    of time greater than zero or remove the key."""
                    .formatted(ConfigurationKeys.globalKey(key), value));
        return whenNothingIsConfigured;
      }
      return span;
    } catch (final RuntimeException e) {
      defects
          .add(
              """
                  '%s' is '%s', which is no span of time. Write it as a number of milliseconds \
                  ('1500'), as ISO-8601 ('PT10S') or with a unit ('10s', '1500ms', '2m')."""
                  .formatted(ConfigurationKeys.globalKey(key), value));
      return whenNothingIsConfigured;
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
    return Duration.ofMillis(Long.parseLong(lower));

  }

  private static boolean flag(
      final String value,
      final String key,
      final boolean whenNothingIsConfigured,
      final List<String> defects) {

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
      final CockpitSettings.Kafka kafka,
      final List<String> defects) {

    if (kafka == null) {
      return null;
    }
    final var bootstrapServers = kafka.bootstrapServers();
    if ((bootstrapServers == null) || bootstrapServers.isBlank()) {
      return null;
    }
    final var topics = kafka.topics() == null
        ? new CockpitSettings.Topics(null, null, null)
        : kafka.topics();
    final var missingTopics = new ArrayList<String>();
    final var userTask = requiredTopic(
        topics.userTask(), ConfigurationKeys.KAFKA_TOPIC_USER_TASK, missingTopics);
    final var workflow = requiredTopic(
        topics.workflow(), ConfigurationKeys.KAFKA_TOPIC_WORKFLOW, missingTopics);
    final var workflowModule = requiredTopic(
        topics.workflowModule(), ConfigurationKeys.KAFKA_TOPIC_WORKFLOW_MODULE, missingTopics);
    if (!missingTopics.isEmpty()) {
      defects.add(
          """
              The Kafka transport was chosen by '%s' but these topics are missing: %s. The cockpit \
              server reads one topic per kind of event, so all three are needed."""
              .formatted(
                  ConfigurationKeys.globalKey(ConfigurationKeys.KAFKA_BOOTSTRAP_SERVERS),
                  String.join(", ", missingTopics)));
    }
    return new KafkaTransportConfiguration(
        bootstrapServers, userTask, workflow, workflowModule, kafka.properties());

  }

  private static String requiredTopic(
      final String value,
      final String key,
      final List<String> missing) {

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
      final CockpitSettings.WorkflowModule workflowModule,
      final boolean templating,
      final List<String> defects) {

    final var settings = workflowModule.cockpit() == null
        ? new CockpitSettings.Cockpit(null, null, null, null, null, null, Map.of())
        : workflowModule.cockpit();

    final var workflowModuleUri = required(
        workflowModuleId, settings.workflowModuleUri(), ConfigurationKeys.WORKFLOW_MODULE_URI,
        defects,
        "It is the address the cockpit server calls back for user-task forms and workflow pages.");
    final var uiUriPath = required(
        workflowModuleId, settings.uiUriPath(), ConfigurationKeys.UI_URI_PATH, defects,
        "It is the path below the module's URI the forms are served from.");
    final var workflows = readWorkflows(workflowModule.workflows());
    final var moduleSaysTheLanguages = (settings.i18nLanguages() != null) && !settings
        .i18nLanguages()
        .isEmpty();
    if (!moduleSaysTheLanguages) {
      refuseWhereAWorkflowIsSilent(
          workflowModuleId, ConfigurationKeys.I18N_LANGUAGES, workflows, defects,
          workflow -> workflow.i18nLanguages() != null,
          "Write the languages titles are reported in, e.g. [en, de].");
    }
    final var i18nLanguages = moduleSaysTheLanguages ? settings.i18nLanguages() : null;

    final var moduleSaysTheBpmnLanguage = (settings.bpmnDescriptionLanguage() != null) && !settings
        .bpmnDescriptionLanguage()
        .isBlank();
    if (!templating && !moduleSaysTheBpmnLanguage) {
      refuseWhereAWorkflowIsSilent(
          workflowModuleId, ConfigurationKeys.BPMN_DESCRIPTION_LANGUAGE, workflows, defects,
          workflow -> workflow.bpmnDescriptionLanguage() != null,
          """
              Without templates the cockpit reports the names written in the BPMN files, and it has \
              to know which language they are in. Configure '%s' instead to render titles from \
              templates."""
              .formatted(ConfigurationKeys.globalKey(ConfigurationKeys.TEMPLATE_LOADER_PATH)));
    }
    final var bpmnDescriptionLanguage = moduleSaysTheBpmnLanguage
        ? settings.bpmnDescriptionLanguage()
        : null;

    final var uiUriTypeValue = required(
        workflowModuleId, settings.uiUriType(), ConfigurationKeys.UI_URI_TYPE, defects,
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
    settings.groupHierarchy().forEach(groupHierarchy::put);

    final var templatePath = settings.templatePath();

    return new WorkflowModuleConfiguration(
        workflowModuleId, workflowModuleUri, uiUriType, uiUriPath, i18nLanguages, bpmnDescriptionLanguage, groupHierarchy, (templatePath == null) || templatePath
            .isBlank()
                ? workflowModuleId
                : templatePath, workflows);

  }

  /**
   * A key of a workflow module which the module itself does not write, and which its workflows
   * may write instead.
   * <p>
   * Version 1 asked for the language of a module's BPMN names and for the languages it reports in
   * when the first event of a workflow arrived, so a module whose workflows all said it for
   * themselves never had to. That rule is kept, and the boot only ends here where no workflow says
   * it either. Whether the workflows which say it are ALL the workflows of the module is a
   * question only the deployment answers, and it is answered by
   * {@link #validateWhatTheWorkflowsHaveToSay} once VanillaBP has wired them.
   *
   * @param saysIt Whether one workflow's settings carry the key
   * @param why What the message tells somebody who has to add the key
   */
  private static void refuseWhereAWorkflowIsSilent(
      final String workflowModuleId,
      final String key,
      final Map<String, WorkflowConfiguration> workflows,
      final List<String> defects,
      final Predicate<WorkflowConfiguration> saysIt,
      final String why) {

    if (!workflows.isEmpty() && workflows.values().stream().allMatch(saysIt)) {
      return;
    }
    defects
        .add(
            """
                '%s' is missing. %s It may stand below a single workflow instead, at '%s', as long as \
                every workflow of the module says it."""
                .formatted(
                    ConfigurationKeys.workflowModuleKey(workflowModuleId, key), why,
                    ConfigurationKeys.workflowKey(workflowModuleId, "<process>", key)));

  }

  /**
   * Ends the boot where a workflow module left a key to its workflows and one of the workflows
   * VanillaBP deployed does not say it.
   * <p>
   * Which BPMN processes a workflow module holds is nothing the configuration knows: it is what
   * the deployment found, so this runs when the modules are registered rather than when the
   * configuration is read. A workflow which reports in no language and a workflow whose BPMN
   * names are in an unknown language are what this spares the application, and they would
   * otherwise surface at the first event of exactly that process.
   *
   * @param workflowModuleId The module which was deployed
   * @param bpmnProcessIds The BPMN processes VanillaBP wired for it
   * @throws IllegalStateException If one of them lacks what the module left out
   */
  public void validateWhatTheWorkflowsHaveToSay(
      final String workflowModuleId,
      final Collection<String> bpmnProcessIds) {

    final var module = workflowModules.get(workflowModuleId);
    if (module == null) {
      return;
    }
    final var defects = new LinkedList<String>();
    bpmnProcessIds
        .forEach(bpmnProcessId -> {
          if (module.i18nLanguages(bpmnProcessId).isEmpty()) {
            defects
                .add(
                    """
                        '%s' is missing, and workflow '%s' does not say it either. Write the languages \
                        titles are reported in, e.g. [en, de]."""
                        .formatted(
                            ConfigurationKeys
                                .workflowKey(
                                    workflowModuleId, bpmnProcessId,
                                    ConfigurationKeys.I18N_LANGUAGES),
                            bpmnProcessId));
          }
          if (module.bpmnDescriptionLanguage(bpmnProcessId) == null) {
            defects
                .add(
                    """
                        '%s' is missing, and workflow '%s' does not say it either. Without templates \
                        the cockpit reports the names written in the BPMN files, and it has to know \
                        which language they are in."""
                        .formatted(
                            ConfigurationKeys
                                .workflowKey(
                                    workflowModuleId, bpmnProcessId,
                                    ConfigurationKeys.BPMN_DESCRIPTION_LANGUAGE),
                            bpmnProcessId));
          }
        });
    if (defects.isEmpty()) {
      return;
    }
    throw new IllegalStateException(
        """
            Workflow module '%s' left a setting of the Business Cockpit to its workflows, and one of \
            them does not carry it:
            %s"""
            .formatted(
                workflowModuleId,
                defects.stream().map("  - "::concat).reduce((
                    a,
                    b) -> a
                        + "\n"
                        + b)
                    .get()));

  }

  /**
   * What single workflows of a module, and single user tasks of those workflows, said about the
   * cockpit.
   * <p>
   * Only three of the module's keys mean anything one workflow at a time - the language its
   * titles are written in, the languages they are reported in, and the directory its templates
   * live in - and only the last of them means anything for a single user task. Which keys those
   * are is declared by the binding of each platform, so a key which is none of them is refused
   * by Quarkus while it starts and ignored by Spring Boot, the way every other key unknown to
   * the <code>vanillabp</code> tree is.
   */
  private static Map<String, WorkflowConfiguration> readWorkflows(
      final Map<String, CockpitSettings.Workflow> workflows) {

    final var configurations = new LinkedHashMap<String, WorkflowConfiguration>();
    workflows
        .forEach((
            bpmnProcessId,
            workflow) -> {
          final var templatePathPerUserTask = new LinkedHashMap<String, String>();
          workflow
              .userTasks()
              .forEach((
                  taskDefinition,
                  userTask) -> {
                if (userTask.templatePath() != null) {
                  templatePathPerUserTask.put(taskDefinition, userTask.templatePath());
                }
              });
          configurations
              .put(
                  bpmnProcessId,
                  new WorkflowConfiguration(
                      bpmnProcessId, workflow.i18nLanguages(), workflow.bpmnDescriptionLanguage(), workflow
                          .templatePath(), templatePathPerUserTask));
        });
    return configurations;

  }

  private static String required(
      final String workflowModuleId,
      final String value,
      final String key,
      final List<String> defects,
      final String why) {

    if ((value != null) && !value.isBlank()) {
      return value;
    }
    defects
        .add(
            "'%s' is missing. %s".formatted(
                ConfigurationKeys.workflowModuleKey(workflowModuleId, key), why));
    return null;

  }


}
