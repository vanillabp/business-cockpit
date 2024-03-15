package io.vanillabp.cockpit.adapter.common.workflow.events;

import io.vanillabp.spi.cockpit.usertask.DetailCharacteristics;
import io.vanillabp.spi.cockpit.workflow.PrefilledWorkflowDetails;
import io.vanillabp.spi.cockpit.workflow.WorkflowDetails;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkflowCreatedEvent implements WorkflowEvent, WorkflowDetails, PrefilledWorkflowDetails {
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

  private WorkflowUiUriType uiUriType;

  private Map<String, Object> details = new HashMap<>();

  private String detailsFulltextSearch;

  private List<String> i18nLanguages;

  private Object templateContext;

  private OffsetDateTime eventTimestamp;

  private List<String> accessibleToUsers;

  private List<String> accessibleToGroups;

  public WorkflowCreatedEvent() {
  }

  public WorkflowCreatedEvent(String workflowModuleId, List<String> i18nLanguages) {
    this.workflowModuleId = workflowModuleId;
    this.i18nLanguages = i18nLanguages;
  }

  public String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  public String getBusinessId() {
    return businessId;
  }

  public void setBusinessId(String businessId) {
    this.businessId = businessId;
  }

  public String getInitiator() {
    return initiator;
  }

  public void setInitiator(String initiator) {
    this.initiator = initiator;
  }

  public OffsetDateTime getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(OffsetDateTime timestamp) {
    this.timestamp = timestamp;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getWorkflowModuleId() {
    return workflowModuleId;
  }

  public void setWorkflowModuleId(String workflowModuleId) {
    this.workflowModuleId = workflowModuleId;
  }

  public Map<String, String> getTitle() {
    return title;
  }

  public void setTitle(Map<String, String> title) {
    this.title = title;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  @Override
  public String getEventId() {
    return eventId;
  }

  public void setEventId(String eventId){
    this.eventId = eventId;
  }

  @Override
  public OffsetDateTime getEventTimestamp() {
    return eventTimestamp;
  }

  public void setEventTimestamp(OffsetDateTime eventTimestamp){
    this.eventTimestamp = eventTimestamp;
  }

  public String getBpmnProcessId() {
    return bpmnProcessId;
  }

  public void setBpmnProcessId(String bpmnProcessId) {
    this.bpmnProcessId = bpmnProcessId;
  }

  public String getBpmnProcessVersion() {
    return bpmnProcessVersion;
  }

  public void setBpmnProcessVersion(String bpmnProcessVersion) {
    this.bpmnProcessVersion = bpmnProcessVersion;
  }

  public String getUiUriPath() {
    return uiUriPath;
  }

  public void setUiUriPath(String uiUriPath) {
    this.uiUriPath = uiUriPath;
  }

  public WorkflowUiUriType getUiUriType() {
    return uiUriType;
  }

  public void setUiUriType(WorkflowUiUriType uiUriType) {
    this.uiUriType = uiUriType;
  }

  public Map<String, Object> getDetails() {
    return details;
  }

  @Override
  public Map<String, ? extends DetailCharacteristics> getDetailsCharacteristics() {
    return null;
  }

  public void setDetails(Map<String, Object> details) {
    this.details = details;
  }

  public String getDetailsFulltextSearch() {
    return detailsFulltextSearch;
  }

  public void setDetailsFulltextSearch(String detailsFulltextSearch) {
    this.detailsFulltextSearch = detailsFulltextSearch;
  }

  public List<String> getI18nLanguages() {
    return i18nLanguages;
  }

  public void setI18nLanguages(List<String> i18nLanguages) {
    this.i18nLanguages = i18nLanguages;
  }

  @Override
  public Object getTemplateContext() {
    return this.templateContext;
  }
  @Override
  public void setTemplateContext(Object templateContext) {
    this.templateContext = templateContext;
  }

  public List<String> getAccessibleToUsers() {
    return accessibleToUsers;
  }

  @Override
  public void setAccessibleToUsers(List<String> accessibleToUsers) {
    this.accessibleToUsers = accessibleToUsers;
  }

  public List<String> getAccessibleToGroups() {
    return accessibleToGroups;
  }

  @Override
  public void setAccessibleToGroups(List<String> accessibleToGroups) {
    this.accessibleToGroups = accessibleToGroups;
  }

}

