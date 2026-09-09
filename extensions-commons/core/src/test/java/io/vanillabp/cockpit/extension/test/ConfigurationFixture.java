package io.vanillabp.cockpit.extension.test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.vanillabp.cockpit.extension.config.CockpitSettings;
import io.vanillabp.cockpit.extension.config.ConfigurationKeys;
import io.vanillabp.integration.adapter.migration.config.MigrationAdapterProperties;
import io.vanillabp.integration.adapter.migration.config.WorkflowModuleAdapterProperties;

/**
 * States a configuration the way a developer writes it - one property key at a time - and turns
 * it into the tree both platforms hand the extension.
 * <p>
 * A key which is none of the extension's ends the fixture rather than the assertion: a test
 * writing a key nobody reads would pass for the wrong reason, and on Quarkus such a key does not
 * reach an application either.
 */
public final class ConfigurationFixture {

  /** The workflow module every test of this module uses. */
  public static final String WORKFLOW_MODULE = "test-module";

  private final Map<String, String> global = new LinkedHashMap<>();

  private final Map<String, String> ofTheWorkflowModule = new LinkedHashMap<>();

  private final Map<String, Map<String, String>> ofTheWorkflows = new LinkedHashMap<>();

  private final Map<String, Map<String, Map<String, String>>> ofTheUserTasks = new LinkedHashMap<>();

  private ConfigurationFixture() {
  }

  /**
   * @return An application which configured nothing about the Business Cockpit but has one
   *         workflow module
   */
  public static ConfigurationFixture anApplication() {

    return new ConfigurationFixture();

  }

  /**
   * @return An application configured completely enough to boot
   */
  public static ConfigurationFixture aConfiguredApplication() {

    return anApplication()
        .with(ConfigurationKeys.REST_BASE_URL, "http://localhost:8080")
        .withWorkflowModule(ConfigurationKeys.WORKFLOW_MODULE_URI, "http://localhost:8081")
        .withWorkflowModule(ConfigurationKeys.UI_URI_TYPE, "WEBPACK_MF_REACT")
        .withWorkflowModule(ConfigurationKeys.UI_URI_PATH, "/remoteEntry.js")
        .withWorkflowModule(ConfigurationKeys.I18N_LANGUAGES, "en,de")
        .withWorkflowModule(ConfigurationKeys.BPMN_DESCRIPTION_LANGUAGE, "en");

  }

  /**
   * @param key A key below <code>vanillabp.cockpit</code>
   * @param value What it is set to
   * @return The fixture
   */
  public ConfigurationFixture with(
      final String key,
      final String value) {

    if (!ConfigurationKeys.GLOBAL_KEYS.contains(key) && !key.startsWith(ConfigurationKeys.KAFKA_PROPERTIES_PREFIX)) {
      throw new IllegalArgumentException(
          "'%s' is no setting of the Business Cockpit".formatted(key));
    }
    global.put(key, value);
    return this;

  }

  public ConfigurationFixture without(
      final String key) {

    global.remove(key);
    return this;

  }

  /**
   * @param key A key below <code>vanillabp.workflow-modules.&lt;id&gt;.cockpit</code>
   * @param value What it is set to
   * @return The fixture
   */
  public ConfigurationFixture withWorkflowModule(
      final String key,
      final String value) {

    if (!ConfigurationKeys.KEYS_OF_A_WORKFLOW_MODULE.contains(key) && !key.startsWith(ConfigurationKeys.GROUP_HIERARCHY
        + ".")) {
      throw new IllegalArgumentException(
          "'%s' is no setting of a workflow module".formatted(key));
    }
    ofTheWorkflowModule.put(key, value);
    return this;

  }

  public ConfigurationFixture withoutWorkflowModule(
      final String key) {

    ofTheWorkflowModule.remove(key);
    return this;

  }

  /**
   * @param bpmnProcessId The workflow this setting belongs to
   * @param key One of the keys a single workflow has
   * @param value What it is set to
   * @return The fixture
   */
  public ConfigurationFixture withWorkflow(
      final String bpmnProcessId,
      final String key,
      final String value) {

    if (!ConfigurationKeys.KEYS_OF_A_WORKFLOW.contains(key)) {
      throw new IllegalArgumentException("'%s' is no setting of a workflow".formatted(key));
    }
    ofTheWorkflows.computeIfAbsent(bpmnProcessId, ignored -> new LinkedHashMap<>()).put(key, value);
    return this;

  }

  /**
   * @param bpmnProcessId The workflow the user task belongs to
   * @param taskDefinition The user task
   * @param key The one key a single user task has
   * @param value What it is set to
   * @return The fixture
   */
  public ConfigurationFixture withUserTask(
      final String bpmnProcessId,
      final String taskDefinition,
      final String key,
      final String value) {

    if (!ConfigurationKeys.KEYS_OF_A_USER_TASK.contains(key)) {
      throw new IllegalArgumentException("'%s' is no setting of a user task".formatted(key));
    }
    ofTheUserTasks
        .computeIfAbsent(bpmnProcessId, ignored -> new LinkedHashMap<>())
        .computeIfAbsent(taskDefinition, ignored -> new LinkedHashMap<>())
        .put(key, value);
    return this;

  }

  /**
   * @return The workflow modules of the application, which is what the platform knows about
   *         them and what the extension iterates
   */
  public MigrationAdapterProperties properties() {

    final var properties = new MigrationAdapterProperties();
    final var module = new WorkflowModuleAdapterProperties();
    properties.setWorkflowModules(Map.of(WORKFLOW_MODULE, module));
    return properties;

  }

  /**
   * @return The cockpit's own tree, as a platform's binding hands it over
   */
  public CockpitSettings settings() {

    final var workflows = new LinkedHashMap<String, CockpitSettings.Workflow>();
    ofTheWorkflows
        .forEach((
            bpmnProcessId,
            settings) -> workflows.put(bpmnProcessId, workflowOf(bpmnProcessId, settings)));
    ofTheUserTasks
        .keySet()
        .stream()
        .filter(bpmnProcessId -> !workflows.containsKey(bpmnProcessId))
        .forEach(bpmnProcessId -> workflows.put(bpmnProcessId, workflowOf(bpmnProcessId, Map.of())));

    final var workflowModule = new CockpitSettings.WorkflowModule(
        ofTheWorkflowModule.isEmpty() ? null : cockpitOf(), workflows);
    return new CockpitSettings(
        global.get(ConfigurationKeys.USER_TASKS_ENABLED), global.get(ConfigurationKeys.WORKFLOW_LIST_ENABLED), global
            .get(
                ConfigurationKeys.TEMPLATE_LOADER_PATH), restOf(), kafkaOf(), new CockpitSettings.ProcessEngineApi(global
                    .get(ConfigurationKeys.REMEMBERED_USER_TASKS)), workflowModule.saysNothing() ? Map.of()
                        : Map.of(WORKFLOW_MODULE, workflowModule));

  }

  private CockpitSettings.Cockpit cockpitOf() {

    final var groupHierarchy = new LinkedHashMap<String, List<String>>();
    ofTheWorkflowModule
        .forEach((
            key,
            value) -> {
          if (key.startsWith(ConfigurationKeys.GROUP_HIERARCHY
              + ".")) {
            groupHierarchy
                .put(
                    key.substring(ConfigurationKeys.GROUP_HIERARCHY.length() + 1),
                    listOf(value));
          }
        });
    return new CockpitSettings.Cockpit(
        ofTheWorkflowModule.get(ConfigurationKeys.WORKFLOW_MODULE_URI), ofTheWorkflowModule
            .get(ConfigurationKeys.UI_URI_TYPE), ofTheWorkflowModule.get(ConfigurationKeys.UI_URI_PATH), listOf(
                ofTheWorkflowModule.get(ConfigurationKeys.I18N_LANGUAGES)), ofTheWorkflowModule
                    .get(ConfigurationKeys.BPMN_DESCRIPTION_LANGUAGE), ofTheWorkflowModule
                        .get(ConfigurationKeys.TEMPLATE_PATH), groupHierarchy);

  }

  private CockpitSettings.Workflow workflowOf(
      final String bpmnProcessId,
      final Map<String, String> settings) {

    final var userTasks = new LinkedHashMap<String, CockpitSettings.UserTask>();
    ofTheUserTasks
        .getOrDefault(bpmnProcessId, Map.of())
        .forEach((
            taskDefinition,
            ofTheUserTask) -> userTasks
                .put(
                    taskDefinition, new CockpitSettings.UserTask(
                        ofTheUserTask.get(ConfigurationKeys.TEMPLATE_PATH))));
    return new CockpitSettings.Workflow(
        listOf(settings.get(ConfigurationKeys.I18N_LANGUAGES)), settings.get(
            ConfigurationKeys.BPMN_DESCRIPTION_LANGUAGE), settings.get(ConfigurationKeys.TEMPLATE_PATH), userTasks);

  }

  private CockpitSettings.Rest restOf() {

    return new CockpitSettings.Rest(
        global.get(ConfigurationKeys.REST_BASE_URL), global.get(ConfigurationKeys.REST_CONNECT_TIMEOUT), global
            .get(ConfigurationKeys.REST_READ_TIMEOUT), global.get(ConfigurationKeys.REST_VERIFY_SSL), global
                .get(ConfigurationKeys.REST_SSL_TRUSTSTORE_FILENAME), global
                    .get(ConfigurationKeys.REST_SSL_TRUSTSTORE_PASSWORD), proxyOf(
                        ConfigurationKeys.REST_PREFIX), new CockpitSettings.Authentication(
                            global.get(ConfigurationKeys.REST_BASIC), global
                                .get(ConfigurationKeys.REST_USERNAME), global
                                    .get(ConfigurationKeys.REST_PASSWORD), new CockpitSettings.OAuth(
                                        global.get(ConfigurationKeys.REST_OAUTH_BASE_URL), global
                                            .get(ConfigurationKeys.REST_OAUTH_CLIENT_ID), global
                                                .get(ConfigurationKeys.REST_OAUTH_CLIENT_SECRET), global
                                                    .get(ConfigurationKeys.REST_OAUTH_BASIC), globalOfTheOauth(
                                                        ConfigurationKeys.CONNECT_TIMEOUT), globalOfTheOauth(
                                                            ConfigurationKeys.READ_TIMEOUT), globalOfTheOauth(
                                                                ConfigurationKeys.VERIFY_SSL), globalOfTheOauth(
                                                                    ConfigurationKeys.SSL_TRUSTSTORE_FILENAME), globalOfTheOauth(
                                                                        ConfigurationKeys.SSL_TRUSTSTORE_PASSWORD), proxyOf(
                                                                            ConfigurationKeys.REST_OAUTH_PREFIX))));

  }

  private CockpitSettings.Proxy proxyOf(
      final String prefix) {

    return new CockpitSettings.Proxy(
        global.get(prefix + ConfigurationKeys.PROXY_HOST), global.get(prefix + ConfigurationKeys.PROXY_PORT), global
            .get(prefix + ConfigurationKeys.PROXY_USERNAME), global.get(prefix + ConfigurationKeys.PROXY_PASSWORD));

  }

  private String globalOfTheOauth(
      final String key) {

    return global.get(ConfigurationKeys.REST_OAUTH_PREFIX + key);

  }

  private CockpitSettings.Kafka kafkaOf() {

    final var properties = new LinkedHashMap<String, String>();
    global
        .forEach((
            key,
            value) -> {
          if (key.startsWith(ConfigurationKeys.KAFKA_PROPERTIES_PREFIX)) {
            properties
                .put(key.substring(ConfigurationKeys.KAFKA_PROPERTIES_PREFIX.length()), value);
          }
        });
    return new CockpitSettings.Kafka(
        global.get(ConfigurationKeys.KAFKA_BOOTSTRAP_SERVERS), new CockpitSettings.Topics(
            global.get(ConfigurationKeys.KAFKA_TOPIC_USER_TASK), global
                .get(ConfigurationKeys.KAFKA_TOPIC_WORKFLOW), global
                    .get(ConfigurationKeys.KAFKA_TOPIC_WORKFLOW_MODULE)), properties);

  }

  private static List<String> listOf(
      final String value) {

    if ((value == null) || value.isBlank()) {
      return null;
    }
    return Arrays.stream(value.split(",")).map(String::trim).filter(entry -> !entry.isEmpty())
        .toList();

  }

}
