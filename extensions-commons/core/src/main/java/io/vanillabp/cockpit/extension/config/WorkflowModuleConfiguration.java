package io.vanillabp.cockpit.extension.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.vanillabp.integration.extension.spi.settings.SettingsResolution;

/**
 * What the Business Cockpit has to know about one workflow module. Every value is resolved
 * from the module's own section first and from the extension's global section afterwards, so a
 * setting which is the same for every module is written once.
 * <p>
 * A single workflow of the module, and a single user task of that workflow, may say something
 * of their own about the texts they are shown with. Those levels are asked before the module's
 * own value, key by key, which is what the four methods taking a BPMN process id are for.
 *
 * @param workflowModuleId The module these settings belong to
 * @param workflowModuleUri Where this module answers the cockpit's provider APIs
 * @param uiUriType How the cockpit loads this module's forms
 * @param uiUriPath Where this module's forms are served from
 * @param i18nLanguages The languages titles are reported in, in the order they were configured
 * @param bpmnDescriptionLanguage The language the names in the BPMN files are written in
 * @param groupHierarchy Which groups a group stands for, reported to the cockpit so that it
 *          can resolve a user's permissions
 * @param templatePath The segment this module contributes to the template lookup path, which
 *          is the module id where nothing was configured
 * @param workflows What single workflows of this module said, by BPMN process id
 */
public record WorkflowModuleConfiguration(
                                          String workflowModuleId,
                                          String workflowModuleUri,
                                          UiUriType uiUriType,
                                          String uiUriPath,
                                          List<String> i18nLanguages,
                                          String bpmnDescriptionLanguage,
                                          Map<String, Collection<String>> groupHierarchy,
                                          String templatePath,
                                          Map<String, WorkflowConfiguration> workflows) implements CockpitSection {

  public WorkflowModuleConfiguration {
    i18nLanguages = i18nLanguages == null ? List.of() : List.copyOf(i18nLanguages);
    groupHierarchy = groupHierarchy == null ? Map.of() : Map.copyOf(groupHierarchy);
    workflows = workflows == null ? Map.of() : Map.copyOf(workflows);
  }

  /**
   * The languages the titles of one workflow are reported in.
   *
   * @param bpmnProcessId The workflow, may be <code>null</code>
   * @return What the workflow configured, else what the module configured
   */
  public List<String> i18nLanguages(
      final String bpmnProcessId) {

    return resolved(bpmnProcessId, CockpitSection::i18nLanguages);

  }

  /**
   * The language the BPMN names of one workflow are written in.
   *
   * @param bpmnProcessId The workflow, may be <code>null</code>
   * @return What the workflow configured, else what the module configured
   */
  public String bpmnDescriptionLanguage(
      final String bpmnProcessId) {

    return resolved(bpmnProcessId, CockpitSection::bpmnDescriptionLanguage);

  }

  /**
   * What the most specific level says about one key. The walk over the levels is the platform's,
   * not this repository's. See {@link CockpitSettingsLevels} for which section belongs to which
   * level.
   *
   * @param <T> The type of the value
   * @param bpmnProcessId The workflow, may be <code>null</code>
   * @param key What to read off a section
   * @return The value of the most specific level which says anything, or <code>null</code>
   */
  private <T> T resolved(
      final String bpmnProcessId,
      final Function<CockpitSection, T> key) {

    return SettingsResolution
        .resolve(
            CockpitSettingsLevels.of(this), workflowModuleId, bpmnProcessId, null, null, key);

  }

  /**
   * The segment one workflow contributes to the template lookup path.
   *
   * @param bpmnProcessId The workflow
   * @return What the workflow configured, else the BPMN process id. That is what makes the
   *         templates of a process land in a directory of its name, with nothing configured
   */
  public String templatePathOfWorkflow(
      final String bpmnProcessId) {

    final var workflow = workflows.get(bpmnProcessId);
    return (workflow == null) || (workflow.templatePath() == null)
        ? bpmnProcessId
        : workflow.templatePath();

  }

  /**
   * The segment one user task contributes to the template lookup path.
   *
   * @param bpmnProcessId The workflow the task belongs to
   * @param taskDefinition The task
   * @return What the user task configured, else the task definition
   */
  public String templatePathOfUserTask(
      final String bpmnProcessId,
      final String taskDefinition) {

    final var workflow = workflows.get(bpmnProcessId);
    if (workflow == null) {
      return taskDefinition;
    }
    return workflow.templatePathPerUserTask().getOrDefault(taskDefinition, taskDefinition);

  }

}
