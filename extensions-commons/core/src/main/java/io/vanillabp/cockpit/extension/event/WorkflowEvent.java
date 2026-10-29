package io.vanillabp.cockpit.extension.event;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.vanillabp.cockpit.extension.spi.WorkflowEventKind;
import io.vanillabp.spi.cockpit.workflow.PrefilledWorkflowDetails;

/**
 * One workflow event on its way to the cockpit server, and at the same time the object a
 * <code>&#64;WorkflowDetailsProvider</code> method receives and enriches. It works the way
 * {@link UserTaskEvent} works, for workflows.
 */
public class WorkflowEvent implements PrefilledWorkflowDetails {

  private final WorkflowEventKind eventKind;

  private String eventId;

  private String workflowId;

  private String businessId;

  private String initiator;

  private OffsetDateTime timestamp;

  /**
   * When the BPMS started the workflow, where the BPMS half could tell. Only the report of an end
   * carries it, in case the end arrives at the cockpit before the creation.
   */
  private OffsetDateTime createdAt;

  private String source;

  private String workflowModuleId;

  private Map<String, String> title = new HashMap<>();

  private String comment;

  private String bpmnProcessId;

  private String bpmnProcessVersion;

  private String uiUriPath;

  private String uiUriType;

  private Map<String, Object> details = new HashMap<>();

  private String detailsFulltextSearch;

  private List<String> i18nLanguages = new ArrayList<>();

  private Object templateContext;

  private List<String> accessibleToUsers = new ArrayList<>();

  private List<String> accessibleToGroups = new ArrayList<>();

  /**
   * Creates an empty event of one kind. The extension fills it, then the BPMS half and the details
   * provider add to it.
   *
   * @param eventKind What happened to the workflow
   */
  public WorkflowEvent(
      final WorkflowEventKind eventKind) {

    this.eventKind = eventKind;

  }

  /**
   * The kind of this event.
   *
   * @return What happened to the workflow, which decides the endpoint respectively the
   *         protobuf message this event is sent as
   */
  public WorkflowEventKind getEventKind() {

    return eventKind;

  }

  /**
   * Takes over what a details provider returned as an object of its own. See
   * {@link UserTaskEvent#applyReturnedDetails} for why the comparison is by identity.
   *
   * @param details What the provider returned, or <code>null</code>
   */
  public void applyReturnedDetails(
      final io.vanillabp.spi.cockpit.workflow.WorkflowDetails details) {

    if ((details == null) || (details == this)) {
      return;
    }
    setInitiator(details.getInitiator());
    setComment(details.getComment());
    setDetails(details.getDetails());
    setDetailsFulltextSearch(details.getDetailsFulltextSearch());
    setTemplateContext(details.getTemplateContext());
    setI18nLanguages(details.getI18nLanguages());
    setAccessibleToUsers(details.getAccessibleToUsers());
    setAccessibleToGroups(details.getAccessibleToGroups());
    if (details.getTitle() != null) {
      setTitle(details.getTitle());
    }
    if (details.getUiUriPath() != null) {
      setUiUriPath(details.getUiUriPath());
    }

  }

  @Override
  public String getId() {

    return workflowId;

  }

  @Override
  public String getEventId() {

    return eventId;

  }

  /**
   * The id of this event. A report which is sent again keeps it, so the cockpit server can tell a
   * repetition from a new event.
   *
   * @param eventId The id of the event
   */
  public void setEventId(
      final String eventId) {

    this.eventId = eventId;

  }

  /**
   * The BPMS' id of the workflow.
   *
   * @return The id of the workflow
   */
  public String getWorkflowId() {

    return workflowId;

  }

  /**
   * The BPMS' id of the workflow.
   *
   * @param workflowId The id of the workflow
   */
  public void setWorkflowId(
      final String workflowId) {

    this.workflowId = workflowId;

  }

  /**
   * The business key the workflow was started with.
   *
   * @return The business key
   */
  public String getBusinessId() {

    return businessId;

  }

  /**
   * The business key the workflow was started with.
   *
   * @param businessId The business key
   */
  public void setBusinessId(
      final String businessId) {

    this.businessId = businessId;

  }

  @Override
  public OffsetDateTime getEventTimestamp() {

    return timestamp;

  }

  /**
   * When the event happened in the BPMS.
   *
   * @return The time of the event
   */
  public OffsetDateTime getTimestamp() {

    return timestamp;

  }

  /**
   * When the event happened in the BPMS.
   *
   * @param timestamp The time of the event
   */
  public void setTimestamp(
      final OffsetDateTime timestamp) {

    this.timestamp = timestamp;

  }

  /**
   * When the BPMS started the workflow. Only the report of an end carries it.
   *
   * @return When the BPMS started the workflow, or <code>null</code> where the BPMS half could
   *         not tell
   */
  public OffsetDateTime getCreatedAt() {

    return createdAt;

  }

  /**
   * When the BPMS created the workflow, where the BPMS half could tell.
   *
   * @param createdAt The time of the creation, or <code>null</code>
   */
  public void setCreatedAt(
      final OffsetDateTime createdAt) {

    this.createdAt = createdAt;

  }

  /**
   * Which instance of the workflow module reported this event. It is the host name of that
   * instance.
   *
   * @return The name of the instance
   */
  public String getSource() {

    return source;

  }

  /**
   * Which instance of the workflow module reported this event. It is the host name of that
   * instance.
   *
   * @param source The name of the instance
   */
  public void setSource(
      final String source) {

    this.source = source;

  }

  /**
   * The workflow module the workflow belongs to.
   *
   * @return The id of the workflow module
   */
  public String getWorkflowModuleId() {

    return workflowModuleId;

  }

  /**
   * The workflow module the workflow belongs to.
   *
   * @param workflowModuleId The id of the workflow module
   */
  public void setWorkflowModuleId(
      final String workflowModuleId) {

    this.workflowModuleId = workflowModuleId;

  }

  @Override
  public String getBpmnProcessId() {

    return bpmnProcessId;

  }

  /**
   * The BPMN process id of the workflow.
   *
   * @param bpmnProcessId The BPMN process id
   */
  public void setBpmnProcessId(
      final String bpmnProcessId) {

    this.bpmnProcessId = bpmnProcessId;

  }

  @Override
  public String getBpmnProcessVersion() {

    return bpmnProcessVersion;

  }

  /**
   * The version of the deployed process the workflow runs on, as a person reads it.
   *
   * @param bpmnProcessVersion The version
   */
  public void setBpmnProcessVersion(
      final String bpmnProcessVersion) {

    this.bpmnProcessVersion = bpmnProcessVersion;

  }

  @Override
  public String getUiUriPath() {

    return uiUriPath;

  }

  @Override
  public void setUiUriPath(
      final String uiUriPath) {

    this.uiUriPath = uiUriPath;

  }

  /**
   * Which kind of user interface the workflow module brings. The user interface reads it, not the
   * cockpit server.
   *
   * @return The kind of user interface
   */
  public String getUiUriType() {

    return uiUriType;

  }

  /**
   * Which kind of user interface the workflow module brings. The user interface reads it, not the
   * cockpit server.
   *
   * @param uiUriType The kind of user interface
   */
  public void setUiUriType(
      final String uiUriType) {

    this.uiUriType = uiUriType;

  }

  @Override
  public String getInitiator() {

    return initiator;

  }

  @Override
  public void setInitiator(
      final String initiator) {

    this.initiator = initiator;

  }

  @Override
  public String getComment() {

    return comment;

  }

  @Override
  public void setComment(
      final String comment) {

    this.comment = comment;

  }

  @Override
  public Map<String, String> getTitle() {

    return title;

  }

  @Override
  public void setTitle(
      final Map<String, String> title) {

    this.title = title;

  }

  @Override
  public Map<String, Object> getDetails() {

    return details;

  }

  @Override
  public void setDetails(
      final Map<String, Object> details) {

    this.details = details;

  }

  @Override
  public String getDetailsFulltextSearch() {

    return detailsFulltextSearch;

  }

  @Override
  public void setDetailsFulltextSearch(
      final String detailsFulltextSearch) {

    this.detailsFulltextSearch = detailsFulltextSearch;

  }

  @Override
  public List<String> getI18nLanguages() {

    return i18nLanguages;

  }

  @Override
  public void setI18nLanguages(
      final List<String> i18nLanguages) {

    this.i18nLanguages = i18nLanguages;

  }

  @Override
  public Object getTemplateContext() {

    return templateContext;

  }

  @Override
  public void setTemplateContext(
      final Object templateContext) {

    this.templateContext = templateContext;

  }

  @Override
  public List<String> getAccessibleToUsers() {

    return accessibleToUsers;

  }

  @Override
  public void setAccessibleToUsers(
      final List<String> accessibleToUsers) {

    this.accessibleToUsers = accessibleToUsers;

  }

  @Override
  public List<String> getAccessibleToGroups() {

    return accessibleToGroups;

  }

  @Override
  public void setAccessibleToGroups(
      final List<String> accessibleToGroups) {

    this.accessibleToGroups = accessibleToGroups;

  }

}
