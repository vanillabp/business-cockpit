package io.vanillabp.cockpit.extension.templating;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import io.vanillabp.cockpit.extension.config.ConfigurationKeys;
import io.vanillabp.cockpit.extension.config.TemplateDirectory;
import io.vanillabp.cockpit.extension.config.WorkflowConfiguration;
import io.vanillabp.cockpit.extension.config.WorkflowModuleConfiguration;
import io.vanillabp.cockpit.extension.event.UserTaskEvent;
import io.vanillabp.cockpit.extension.event.WorkflowEvent;
import io.vanillabp.cockpit.extension.spi.UserTaskEventKind;
import io.vanillabp.cockpit.extension.spi.WorkflowEventKind;

/**
 * Checks in a test of a workflow module that the template context of a user task or a workflow has
 * every value its templates read.
 * <p>
 * The cockpit adapter cannot check that when the application starts. The template context is an
 * object the details provider builds for each report, so only a test which builds one knows it. A
 * value which is missing shows up at runtime as an error in the log for each report, and the text
 * stays as it was. This check renders the templates the way the cockpit adapter does and names
 * each template, value and task which would fail:
 *
 * <pre>
 * TemplateContextCheck
 *     .ofWorkflowModule("loan-approval", "en", "de")
 *     .assertUserTask("LoanApproval", "assessRisk", Map.of("loan", loan));
 * </pre>
 * <p>
 * A user task renders all of its texts with its own template context: its title, the title of its
 * kind and the text a search matches against, and the title of its workflow as well. So the
 * template context of every user task needs the values of <code>workflow-title.ftl</code> too.
 * <p>
 * The templates are looked for where the cockpit adapter looks for them, in
 * <code>&lt;workflow module id&gt;/business-cockpit-templates</code> of the classpath unless
 * {@link #withTemplateLoaderPath} names another directory. A module which configures
 * <code>template-path</code> for a workflow or a user task says so with
 * {@link #withTemplatePathOfWorkflow} and {@link #withTemplatePathOfUserTask}.
 */
public final class TemplateContextCheck {

  private final String workflowModuleId;

  private final List<String> languages;

  private TemplateDirectory templateDirectory;

  private final Map<String, String> templatePathPerWorkflow = new HashMap<>();

  private final Map<String, Map<String, String>> templatePathPerUserTask = new HashMap<>();

  private TemplateContextCheck(
      final String workflowModuleId,
      final List<String> languages) {

    this.workflowModuleId = workflowModuleId;
    this.languages = List.copyOf(languages);
    this.templateDirectory = new TemplateDirectory(
        Templating.CLASSPATH_PREFIX, workflowModuleId
            + "/"
            + ConfigurationKeys.DEFAULT_TEMPLATE_DIRECTORY, null);

  }

  /**
   * Starts the check of one workflow module.
   *
   * @param workflowModuleId The id of the workflow module
   * @param languages The languages the module reports its titles in, its
   *          <code>i18n-languages</code>. Each title is rendered once per language, so a template
   *          for one language only is checked as well
   * @return The check
   */
  public static TemplateContextCheck ofWorkflowModule(
      final String workflowModuleId,
      final String... languages) {

    if (languages.length == 0) {
      throw new IllegalArgumentException(
          "Name the languages of the workflow module '%s', the values of its key i18n-languages"
              .formatted(workflowModuleId));
    }
    return new TemplateContextCheck(workflowModuleId, List.of(languages));

  }

  /**
   * Names the directory of the templates, where the module configures
   * <code>template-loader-path</code>.
   *
   * @param templateLoaderPath The directory as it is configured, with <code>classpath:</code> or
   *          <code>file:</code> in front of it
   * @return This check
   */
  public TemplateContextCheck withTemplateLoaderPath(
      final String templateLoaderPath) {

    if (!Templating.saysWhereItIs(templateLoaderPath)) {
      throw new IllegalArgumentException(
          "The template directory '%s' does not say where it is. Write '%s' or '%s' in front of it."
              .formatted(templateLoaderPath, Templating.CLASSPATH_PREFIX, Templating.FILE_PREFIX));
    }
    this.templateDirectory = new TemplateDirectory(templateLoaderPath, "", null);
    return this;

  }

  /**
   * Names the segment of a workflow in the template paths, where the workflow configures
   * <code>template-path</code>.
   *
   * @param bpmnProcessId The workflow
   * @param templatePath The segment it configures
   * @return This check
   */
  public TemplateContextCheck withTemplatePathOfWorkflow(
      final String bpmnProcessId,
      final String templatePath) {

    templatePathPerWorkflow.put(bpmnProcessId, templatePath);
    return this;

  }

  /**
   * Names the segment of a user task in the template paths, where the user task configures
   * <code>template-path</code>.
   *
   * @param bpmnProcessId The workflow the task belongs to
   * @param taskDefinition The task
   * @param templatePath The segment it configures
   * @return This check
   */
  public TemplateContextCheck withTemplatePathOfUserTask(
      final String bpmnProcessId,
      final String taskDefinition,
      final String templatePath) {

    templatePathPerUserTask
        .computeIfAbsent(bpmnProcessId, workflow -> new HashMap<>())
        .put(taskDefinition, templatePath);
    return this;

  }

  /**
   * Renders the templates of one user task with a template context.
   *
   * @param bpmnProcessId The workflow the task belongs to
   * @param taskDefinition The task
   * @param templateContext What the details provider of the task sets with
   *          <code>setTemplateContext</code>
   * @return One message for each template and value which is missing, and for each template which
   *         fails for another reason, empty where every template renders
   */
  public List<String> missingValuesOfUserTask(
      final String bpmnProcessId,
      final String taskDefinition,
      final Object templateContext) {

    final var event = new UserTaskEvent(UserTaskEventKind.CREATED);
    event.setWorkflowModuleId(workflowModuleId);
    event.setBpmnProcessId(bpmnProcessId);
    event.setTaskDefinition(taskDefinition);
    event.setTemplateContext(templateContext);
    final var templating = new CollectingTemplating(workflowModuleId, templateDirectory);
    EventTitles.fill(event, module(), templating, null, null);
    return templating.missingValues();

  }

  /**
   * Renders the templates of one workflow with a template context.
   *
   * @param bpmnProcessId The workflow
   * @param templateContext What the details provider of the workflow sets with
   *          <code>setTemplateContext</code>
   * @return One message for each template and value which is missing, and for each template which
   *         fails for another reason, empty where every template renders
   */
  public List<String> missingValuesOfWorkflow(
      final String bpmnProcessId,
      final Object templateContext) {

    final var event = new WorkflowEvent(WorkflowEventKind.CREATED);
    event.setWorkflowModuleId(workflowModuleId);
    event.setBpmnProcessId(bpmnProcessId);
    event.setTemplateContext(templateContext);
    final var templating = new CollectingTemplating(workflowModuleId, templateDirectory);
    EventTitles.fill(event, module(), templating, null);
    return templating.missingValues();

  }

  /**
   * Fails when a template of one user task reads a value its template context does not have.
   *
   * @param bpmnProcessId The workflow the task belongs to
   * @param taskDefinition The task
   * @param templateContext What the details provider of the task sets
   * @throws AssertionError Naming every template, value and the task, where a value is missing
   */
  public void assertUserTask(
      final String bpmnProcessId,
      final String taskDefinition,
      final Object templateContext) {

    failOn(missingValuesOfUserTask(bpmnProcessId, taskDefinition, templateContext));

  }

  /**
   * Fails when a template of one workflow reads a value its template context does not have.
   *
   * @param bpmnProcessId The workflow
   * @param templateContext What the details provider of the workflow sets
   * @throws AssertionError Naming every template, value and the workflow, where a value is missing
   */
  public void assertWorkflow(
      final String bpmnProcessId,
      final Object templateContext) {

    failOn(missingValuesOfWorkflow(bpmnProcessId, templateContext));

  }

  private static void failOn(
      final List<String> missingValues) {

    if (missingValues.isEmpty()) {
      return;
    }
    throw new AssertionError(String.join(System.lineSeparator(), missingValues));

  }

  /**
   * The module as the cockpit adapter would read it from the configuration. A workflow names only
   * its template paths. Its languages stay <code>null</code>, so the ones of the module count.
   */
  private WorkflowModuleConfiguration module() {

    final var workflows = new LinkedHashMap<String, WorkflowConfiguration>();
    final var bpmnProcessIds = new LinkedHashSet<String>(templatePathPerWorkflow.keySet());
    bpmnProcessIds.addAll(templatePathPerUserTask.keySet());
    bpmnProcessIds
        .forEach(bpmnProcessId -> workflows
            .put(
                bpmnProcessId, new WorkflowConfiguration(
                    bpmnProcessId, null, null, templatePathPerWorkflow.get(bpmnProcessId), templatePathPerUserTask
                        .getOrDefault(bpmnProcessId, Map.of()), null)));
    return new WorkflowModuleConfiguration(
        workflowModuleId, null, null, null, languages, null, Map
            .of(), workflowModuleId, templateDirectory, workflows, null);

  }

  /**
   * The templating of the cockpit adapter, which collects a missing value instead of logging it.
   */
  private static final class CollectingTemplating extends FreemarkerTemplating {

    private final List<String> missingValues = new ArrayList<>();

    private CollectingTemplating(
        final String workflowModuleId,
        final TemplateDirectory templateDirectory) {

      // the first segment of each template name is the module id, as in the cockpit adapter
      super(Map.of(workflowModuleId, templateDirectory));

    }

    @Override
    protected void reportAMissingVariable(
        final String template,
        final String variable,
        final String renderedFor,
        final Exception cause) {

      final var message = missingVariable(template, variable, renderedFor);
      // a title is rendered once per language, and the same template fails each time
      if (!missingValues.contains(message)) {
        missingValues.add(message);
      }

    }

    @Override
    protected void reportAFailure(
        final String template,
        final String renderedFor,
        final Exception cause) {

      final var message = "The template '%s' for %s cannot be rendered: %s"
          .formatted(template, renderedFor, cause.getMessage());
      if (!missingValues.contains(message)) {
        missingValues.add(message);
      }

    }

    private List<String> missingValues() {

      return List.copyOf(missingValues);

    }

  }

}
