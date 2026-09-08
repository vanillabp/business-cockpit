package io.vanillabp.cockpit.extension.templating;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import io.vanillabp.cockpit.extension.config.WorkflowModuleConfiguration;
import io.vanillabp.cockpit.extension.event.UserTaskEvent;
import io.vanillabp.cockpit.extension.event.WorkflowEvent;

/**
 * Fills the texts the cockpit shows: the workflow's title, the task's title, the title of a
 * kind of task, and the text a search matches against.
 * <p>
 * There are two sources, and which one is used is a decision of the whole application rather
 * than of a single event. With a template directory configured, one template per text and per
 * language is rendered. Without one, the names written in the BPMN file are reported, in the
 * one language the module declared its BPMN to be written in.
 * <p>
 * A value the details provider already put in wins over both. It is then treated as the name of
 * a template first, and stays the literal text where no template of that name exists - which
 * is how a workflow module writes a fixed title for one task without shipping a template for
 * it.
 */
public final class EventTitles {

  private EventTitles() {
  }

  /**
   * Fills the texts of a user-task event.
   *
   * @param event The event, already carrying whatever the details provider set
   * @param module The workflow module's settings
   * @param templating The renderer, {@link Templating#none()} where the application has none
   * @param bpmnTaskName The BPMN name of the user task, the fallback title
   * @param bpmnProcessName The BPMN name of the process, the fallback workflow title
   */
  public static void fill(
      final UserTaskEvent event,
      final WorkflowModuleConfiguration module,
      final Templating templating,
      final String bpmnTaskName,
      final String bpmnProcessName) {

    final var languages = languagesOf(event.getI18nLanguages(), module);
    event.setI18nLanguages(languages);
    final var lookupPaths = lookupPaths(
        module, event.getBpmnProcessId(), event.getTaskDefinition());

    for (final var language : languages) {
      final var locale = Locale.forLanguageTag(language);
      render(
          event.getWorkflowTitle(), event::setWorkflowTitle, language, locale,
          Templating.WORKFLOW_TITLE, lookupPaths, templating, event.getTemplateContext());
      render(
          event.getTitle(), event::setTitle, language, locale, Templating.TASK_TITLE,
          lookupPaths, templating, event.getTemplateContext());
      render(
          event.getTaskDefinitionTitle(), event::setTaskDefinitionTitle, language, locale,
          Templating.TASK_DEFINITION_TITLE, lookupPaths, templating,
          event.getTemplateContext());
    }
    final var bpmnLanguage = module.bpmnDescriptionLanguage();
    fallBackToTheBpmnName(
        event.getWorkflowTitle(), event::setWorkflowTitle, bpmnLanguage, bpmnProcessName);
    fallBackToTheBpmnName(event.getTitle(), event::setTitle, bpmnLanguage, bpmnTaskName);
    fallBackToTheBpmnName(
        event.getTaskDefinitionTitle(), event::setTaskDefinitionTitle, bpmnLanguage,
        bpmnTaskName);

    if (event.getDetailsFulltextSearch() == null) {
      templating
          .render(
              lookupPaths, Templating.TASK_FULLTEXT_SEARCH, firstLocale(languages),
              event.getTemplateContext(),
              Map
                  .of(
                      "taskDefinitionTitle", event.getTaskDefinitionTitle(),
                      "taskTitle", event.getTitle(),
                      "workflowTitle", event.getWorkflowTitle(),
                      "taskLanguages", languages))
          .ifPresent(event::setDetailsFulltextSearch);
    }

  }

  /**
   * Fills the texts of a workflow event.
   *
   * @param event The event, already carrying whatever the details provider set
   * @param module The workflow module's settings
   * @param templating The renderer
   * @param bpmnProcessName The BPMN name of the process, the fallback title
   */
  public static void fill(
      final WorkflowEvent event,
      final WorkflowModuleConfiguration module,
      final Templating templating,
      final String bpmnProcessName) {

    final var languages = languagesOf(event.getI18nLanguages(), module);
    event.setI18nLanguages(languages);
    final var lookupPaths = lookupPaths(module, event.getBpmnProcessId(), null);

    for (final var language : languages) {
      render(
          event.getTitle(), event::setTitle, language, Locale.forLanguageTag(language),
          Templating.WORKFLOW_TITLE, lookupPaths, templating, event.getTemplateContext());
    }
    fallBackToTheBpmnName(
        event.getTitle(), event::setTitle, module.bpmnDescriptionLanguage(), bpmnProcessName);

    if (event.getDetailsFulltextSearch() == null) {
      templating
          .render(
              lookupPaths, Templating.WORKFLOW_FULLTEXT_SEARCH, firstLocale(languages),
              event.getTemplateContext(),
              Map.of("workflowTitle", event.getTitle(), "workflowLanguages", languages))
          .ifPresent(event::setDetailsFulltextSearch);
    }

  }

  /**
   * The directories a template is looked for in, most specific first: everything below the
   * module's own path, narrowed by the BPMN process and then by the task definition. A module
   * which configured no path contributes its id, which is what makes a module's templates land
   * in a directory of the module's name without anybody configuring it.
   *
   * @param module The workflow module's settings
   * @param bpmnProcessId The BPMN process, may be <code>null</code>
   * @param taskDefinition The task definition, may be <code>null</code>
   * @return The paths
   */
  public static List<String> lookupPaths(
      final WorkflowModuleConfiguration module,
      final String bpmnProcessId,
      final String taskDefinition) {

    final var segments = new LinkedList<String>();
    segments.add(module.templatePath());
    final var paths = new LinkedList<String>();
    if ((bpmnProcessId != null) && !bpmnProcessId.isBlank()) {
      segments.add(bpmnProcessId);
      if ((taskDefinition != null) && !taskDefinition.isBlank()) {
        segments.add(taskDefinition);
        paths.add(String.join("/", segments));
        segments.removeLast();
      }
      paths.add(String.join("/", segments));
      segments.removeLast();
    }
    paths.add(String.join("/", segments));
    return paths;

  }

  /**
   * Renders one text for one language. What the details provider already wrote is treated as
   * the name of a template, and it stays untouched where no such template exists.
   */
  private static void render(
      final Map<String, String> texts,
      final Consumer<Map<String, String>> replaceTexts,
      final String language,
      final Locale locale,
      final String defaultTemplateName,
      final List<String> lookupPaths,
      final Templating templating,
      final Object templateContext) {

    final var given = texts == null ? null : texts.get(language);
    final var templateName = given != null ? given : defaultTemplateName;
    templating
        .render(lookupPaths, templateName, locale, templateContext, Map.of())
        .ifPresent(text -> put(texts, replaceTexts, language, text));

  }

  /**
   * Where no template produced anything and no details provider wrote anything, the name the
   * modeller gave the element is what the cockpit shows - in the one language the module
   * declared its BPMN files to be written in, because that is the only language that name is
   * in.
   */
  private static void fallBackToTheBpmnName(
      final Map<String, String> texts,
      final Consumer<Map<String, String>> replaceTexts,
      final String bpmnDescriptionLanguage,
      final String bpmnName) {

    if ((texts == null) || !texts.isEmpty() || (bpmnName == null) || (bpmnDescriptionLanguage == null)) {
      return;
    }
    put(texts, replaceTexts, bpmnDescriptionLanguage, bpmnName);

  }

  private static void put(
      final Map<String, String> texts,
      final Consumer<Map<String, String>> replaceTexts,
      final String language,
      final String text) {

    try {
      texts.put(language, text);
    } catch (final UnsupportedOperationException e) {
      // a details provider may hand in an immutable map, which is not a mistake worth
      // reporting - it is replaced by one which can carry the rendered text
      final var mutable = new LinkedHashMap<>(texts);
      mutable.put(language, text);
      replaceTexts.accept(mutable);
    }

  }

  private static List<String> languagesOf(
      final List<String> ofEvent,
      final WorkflowModuleConfiguration module) {

    if ((ofEvent != null) && !ofEvent.isEmpty()) {
      return ofEvent;
    }
    return module.i18nLanguages();

  }

  private static Locale firstLocale(
      final List<String> languages) {

    return languages.isEmpty() ? null : Locale.forLanguageTag(languages.getFirst());

  }

}
