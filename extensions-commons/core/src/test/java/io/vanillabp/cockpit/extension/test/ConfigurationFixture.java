package io.vanillabp.cockpit.extension.test;

import java.util.LinkedHashMap;
import java.util.Map;

import io.vanillabp.cockpit.extension.config.ConfigurationKeys;
import io.vanillabp.integration.adapter.migration.config.MigrationAdapterProperties;
import io.vanillabp.integration.adapter.migration.config.WorkflowModuleAdapterProperties;

/**
 * Builds the resolved properties of an application, the way both platforms hand them to an
 * extension, so that a test can state a configuration in the terms a developer writes it in.
 */
public final class ConfigurationFixture {

  /** The workflow module every test of this module uses. */
  public static final String WORKFLOW_MODULE = "test-module";

  private final Map<String, String> global = new LinkedHashMap<>();

  private final Map<String, Map<String, String>> perWorkflowModule = new LinkedHashMap<>();

  private ConfigurationFixture() {
  }

  /**
   * @return An application which configured nothing about the Business Cockpit but has one
   *         workflow module
   */
  public static ConfigurationFixture anApplication() {

    final var fixture = new ConfigurationFixture();
    fixture.perWorkflowModule.put(WORKFLOW_MODULE, new LinkedHashMap<>());
    return fixture;

  }

  /**
   * @return An application configured completely enough to boot
   */
  public static ConfigurationFixture aConfiguredApplication() {

    return anApplication()
        .with("rest.base-url", "http://localhost:8080")
        .withWorkflowModule("workflow-module-uri", "http://localhost:8081")
        .withWorkflowModule("ui-uri-type", "WEBPACK_MF_REACT")
        .withWorkflowModule("ui-uri-path", "/remoteEntry.js")
        .withWorkflowModule("i18n-languages", "en,de")
        .withWorkflowModule("bpmn-description-language", "en");

  }

  public ConfigurationFixture with(
      final String key,
      final String value) {

    global.put(key, value);
    return this;

  }

  public ConfigurationFixture without(
      final String key) {

    global.remove(key);
    return this;

  }

  public ConfigurationFixture withWorkflowModule(
      final String key,
      final String value) {

    perWorkflowModule.get(WORKFLOW_MODULE).put(key, value);
    return this;

  }

  public ConfigurationFixture withoutWorkflowModule(
      final String key) {

    perWorkflowModule.get(WORKFLOW_MODULE).remove(key);
    return this;

  }

  /**
   * @param bpmnProcessId The workflow this setting belongs to
   * @param key The key below the extension's section
   * @param value What it is set to
   * @return The fixture
   */
  public ConfigurationFixture withWorkflow(
      final String bpmnProcessId,
      final String key,
      final String value) {

    return withWorkflowModule(ConfigurationKeys.ofWorkflow(bpmnProcessId, key), value);

  }

  /**
   * @param bpmnProcessId The workflow the user task belongs to
   * @param taskDefinition The user task
   * @param key The key below the extension's section
   * @param value What it is set to
   * @return The fixture
   */
  public ConfigurationFixture withUserTask(
      final String bpmnProcessId,
      final String taskDefinition,
      final String key,
      final String value) {

    return withWorkflowModule(
        ConfigurationKeys.ofUserTask(bpmnProcessId, taskDefinition, key), value);

  }


  /**
   * @return The properties as the core resolved them
   */
  public MigrationAdapterProperties build() {

    final var properties = new MigrationAdapterProperties();
    properties.setExtensions(Map.of("business-cockpit", global));
    final var modules = new LinkedHashMap<String, WorkflowModuleAdapterProperties>();
    perWorkflowModule
        .forEach((
            workflowModuleId,
            settings) -> {
          final var module = new WorkflowModuleAdapterProperties();
          module.setExtensions(Map.of("business-cockpit", settings));
          modules.put(workflowModuleId, module);
        });
    properties.setWorkflowModules(modules);
    return properties;

  }

}
