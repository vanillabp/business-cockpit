package io.vanillabp.spi.cockpit.workflow;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * The details of a workflow as VanillaBP prefilled them from what the BPMS reported. A method
 * annotated with {@link WorkflowDetailsProvider} takes it as a parameter.
 *
 * <p>The getters say what the BPMS reported. The setters change what is reported to the cockpit.
 * Most providers set what they want to add or change and return this very object.
 *
 * @see WorkflowDetailsProvider
 */
public interface PrefilledWorkflowDetails extends WorkflowDetails {
  /**
   * The id of this report, which the cockpit receives as the id of the event. It is not the id of
   * the workflow, see {@link #getId()} for that.
   *
   * @return The id of this report
   */
  String getEventId();

  /**
   * When the reported change happened. Where the BPMS gives no time, it is the time the report
   * was built.
   *
   * @return When the change happened
   */
  OffsetDateTime getEventTimestamp();

  /**
   * The id of the BPMN process the workflow runs, as written in the BPMN file.
   *
   * @return The BPMN process id
   */
  String getBpmnProcessId();

  /**
   * The version of the deployed BPMN process the workflow runs on, the way the BPMS reports it. It
   * is the value {@link WorkflowDetailsProvider#version()} is matched against.
   *
   * @return The version, or <code>null</code> where the BPMS reports none
   */
  String getBpmnProcessVersion();

  /**
   * Names the user who caused this report.
   *
   * <p>This is mandatory, and your workflow module is the only one who can answer it. No
   * workflow system records who started a case, and by the time this object is prefilled the
   * security context of the request is gone. So keep the user in the workflow aggregate and
   * read it back here.
   *
   * <p>Two answers are allowed: a user id, or
   * {@link io.vanillabp.spi.cockpit.Initiator#SYSTEM} where no user caused the action. Set the
   * constant as well to drop a value an adapter prefilled, because setting <code>null</code>
   * looks like a provider which did nothing.
   *
   * @param initiator The user who caused this report
   */
  void setInitiator(
      String initiator);

  /**
   * Sets the short text the cockpit stores with the workflow.
   *
   * @param comment The comment
   * @see WorkflowDetails#getComment()
   */
  void setComment(
      String comment);

  /**
   * Sets the title of the workflow, or the name of a template, per language.
   *
   * @param title The texts or template names, keyed by language
   * @see WorkflowDetails#getTitle()
   */
  void setTitle(
      Map<String, String> title);

  /**
   * Sets the business data the cockpit stores with the workflow.
   *
   * @param details The business data
   * @see WorkflowDetails#getDetails()
   */
  void setDetails(
      Map<String, Object> details);

  /**
   * Sets the text the full-text search of the workflow list looks in.
   *
   * @param detailsFulltextSearch The text to search in
   * @see WorkflowDetails#getDetailsFulltextSearch()
   */
  void setDetailsFulltextSearch(
      String detailsFulltextSearch);

  /**
   * Sets the languages the title is rendered in. Leave it empty to use the languages
   * configured for the workflow module.
   *
   * @param i18nLanguages The languages, like <code>en</code>
   * @see WorkflowDetails#getI18nLanguages()
   */
  void setI18nLanguages(
      List<String> i18nLanguages);

  /**
   * Sets the object the templates read their values from, for instance the workflow
   * aggregate. Nothing is prefilled here, so without this call the templates see no data
   * of the case.
   *
   * @param templateContext The template context
   * @see WorkflowDetails#getTemplateContext()
   */
  void setTemplateContext(
      Object templateContext);

  /**
   * Sets the users who may open this workflow in the cockpit.
   *
   * @param accessibleToUsers The user ids admitted to this workflow
   * @see WorkflowDetails#getAccessibleToUsers()
   */
  void setAccessibleToUsers(
      List<String> accessibleToUsers);

  /**
   * Sets the groups whose members may open this workflow in the cockpit.
   *
   * @param accessibleToGroups The group ids admitted to this workflow
   * @see WorkflowDetails#getAccessibleToGroups()
   */
  void setAccessibleToGroups(
      List<String> accessibleToGroups);

  /**
   * Tells the cockpit where the status page of this workflow is found.
   *
   * <p>Without this the cockpit uses the path the workflow module configured, which is the same
   * for every workflow of the module. A path which starts with <code>http://</code> or
   * <code>https://</code> is opened as it is. This is how a module hands out the address of the
   * page in another application which shows this very case. Any other path belongs to the
   * workflow module, and the cockpit user interface puts <code>/wm/&lt;workflowModuleId&gt;/</code>
   * in front of it.
   *
   * @param uiUriPath What the cockpit opens for this workflow
   */
  void setUiUriPath(
      String uiUriPath);

}
