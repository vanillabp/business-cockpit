package io.vanillabp.cockpit.extension.templating;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Renders the texts the Business Cockpit shows from templates the workflow module ships.
 * <p>
 * Templating is optional in two independent ways, and both end here: the application may have
 * no template engine on its classpath, and it may have one but configure no template directory.
 * In either case {@link #none()} is used and every render answers empty, whereupon the cockpit
 * reports the names written in the BPMN files instead.
 */
public interface Templating {

  /** The template of a workflow's title, looked up per language. */
  String WORKFLOW_TITLE = "workflow-title.ftl";

  /** The template of a user task's title. */
  String TASK_TITLE = "task-title.ftl";

  /** The template of the title of a kind of user task, shown where tasks are grouped. */
  String TASK_DEFINITION_TITLE = "task-definition-title.ftl";

  /** The template of the text a user-task search matches against. */
  String TASK_FULLTEXT_SEARCH = "task-fulltext-search.ftl";

  /** The template of the text a workflow search matches against. */
  String WORKFLOW_FULLTEXT_SEARCH = "workflow-fulltext-search.ftl";

  /**
   * Renders one template.
   *
   * @param lookupPaths The directories to look in, most specific first - the first one holding
   *          a template of that name wins
   * @param templateName The file name
   * @param locale The language the text is rendered for, or <code>null</code> for the
   *          engine's default
   * @param templateContext What the template reads its values from - a map, a POJO or a record
   * @param additionalValues Values put next to the context under their own names, e.g. the
   *          titles a fulltext template joins
   * @return The text, or empty where no template of that name exists in any of the paths or
   *         rendering failed
   */
  Optional<String> render(
      List<String> lookupPaths,
      String templateName,
      Locale locale,
      Object templateContext,
      Map<String, Object> additionalValues);

  /**
   * @return A templating which renders nothing, for an application which configured none
   */
  static Templating none() {

    return (
        lookupPaths,
        templateName,
        locale,
        templateContext,
        additionalValues) -> Optional.empty();

  }

  /**
   * Whether a template engine is on the classpath at all. The extension declares Freemarker as
   * an optional dependency, so an application which never renders a title does not carry it.
   *
   * @return Whether templates can be rendered
   */
  static boolean engineAvailable() {

    try {
      Class
          .forName(
              "freemarker.template.Configuration", false,
              Templating.class.getClassLoader());
      return true;
    } catch (final ClassNotFoundException e) {
      return false;
    }

  }

}
