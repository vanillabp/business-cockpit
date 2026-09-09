package io.vanillabp.cockpit.extension.event;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.vanillabp.cockpit.extension.config.UiUriType;
import io.vanillabp.cockpit.extension.spi.WorkflowEventKind;
import io.vanillabp.spi.cockpit.usertask.DetailCharacteristics;
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

  private String source;

  private String workflowModuleId;

  private Map<String, String> title = new HashMap<>();

  private String comment;

  private String bpmnProcessId;

  private String bpmnProcessVersion;

  private String uiUriPath;

  private UiUriType uiUriType;

  private Map<String, Object> details = new HashMap<>();

  private String detailsFulltextSearch;

  private List<String> i18nLanguages = new ArrayList<>();

  private Object templateContext;

  private List<String> accessibleToUsers = new ArrayList<>();

  private List<String> accessibleToGroups = new ArrayList<>();

  public WorkflowEvent(
      final WorkflowEventKind eventKind) {

    this.eventKind = eventKind;

  }

  /**
   * @return What happened to the workflow, which decides the endpoint respectively the
   *         protobuf message this event is sent as
   */
  public WorkflowEventKind getEventKind() {

    return eventKind;

  }

  /**
   * Takes over what a details provider returned as an object of its own - see
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
    if (details.getTitle() != null) {
      setTitle(details.getTitle());
    }
    if (details.getUiUriPath() != null) {
      setUiUriPath(details.getUiUriPath());
    }

  }

  @Override
  public String getEventId() {

    return eventId;

  }

  public void setEventId(
      final String eventId) {

    this.eventId = eventId;

  }

  public String getWorkflowId() {

    return workflowId;

  }

  public void setWorkflowId(
      final String workflowId) {

    this.workflowId = workflowId;

  }

  public String getBusinessId() {

    return businessId;

  }

  public void setBusinessId(
      final String businessId) {

    this.businessId = businessId;

  }

  @Override
  public OffsetDateTime getEventTimestamp() {

    return timestamp;

  }

  public OffsetDateTime getTimestamp() {

    return timestamp;

  }

  public void setTimestamp(
      final OffsetDateTime timestamp) {

    this.timestamp = timestamp;

  }

  public String getSource() {

    return source;

  }

  public void setSource(
      final String source) {

    this.source = source;

  }

  public String getWorkflowModuleId() {

    return workflowModuleId;

  }

  public void setWorkflowModuleId(
      final String workflowModuleId) {

    this.workflowModuleId = workflowModuleId;

  }

  @Override
  public String getBpmnProcessId() {

    return bpmnProcessId;

  }

  public void setBpmnProcessId(
      final String bpmnProcessId) {

    this.bpmnProcessId = bpmnProcessId;

  }

  @Override
  public String getBpmnProcessVersion() {

    return bpmnProcessVersion;

  }

  public void setBpmnProcessVersion(
      final String bpmnProcessVersion) {

    this.bpmnProcessVersion = bpmnProcessVersion;

  }

  @Override
  public String getUiUriPath() {

    return uiUriPath;

  }

  public void setUiUriPath(
      final String uiUriPath) {

    this.uiUriPath = uiUriPath;

  }

  public UiUriType getUiUriType() {

    return uiUriType;

  }

  public void setUiUriType(
      final UiUriType uiUriType) {

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

  /**
   * The cockpit's BPMS API has no field for these, so nothing is transported and an empty map
   * is the honest answer. The SPI declares them for a viewer which can sort and filter by a
   * detail; that viewer does not exist yet.
   */
  @Override
  public Map<String, ? extends DetailCharacteristics> getDetailsCharacteristics() {

    return Map.of();

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

  public List<String> getAccessibleToUsers() {

    return accessibleToUsers;

  }

  @Override
  public void setAccessibleToUsers(
      final List<String> accessibleToUsers) {

    this.accessibleToUsers = accessibleToUsers;

  }

  public List<String> getAccessibleToGroups() {

    return accessibleToGroups;

  }

  @Override
  public void setAccessibleToGroups(
      final List<String> accessibleToGroups) {

    this.accessibleToGroups = accessibleToGroups;

  }

}
