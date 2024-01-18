package io.vanillabp.cockpit.adapter.common.workflow.events;

import io.vanillabp.spi.cockpit.usertask.DetailCharacteristics;
import io.vanillabp.spi.cockpit.workflow.PrefilledWorkflowDetails;
import io.vanillabp.spi.cockpit.workflow.WorkflowDetails;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkflowCreatedEvent implements WorkflowEvent, WorkflowDetails, PrefilledWorkflowDetails {
  private String id;

  private String workflowId;

  private String businessId;

  private String initiator;

  private OffsetDateTime timestamp;

  private String source;

  private String workflowModule;

  private Map<String, String> title = new HashMap<>();

  private String comment;

  private String bpmnProcessId;

  private String bpmnProcessVersion;

  private String workflowModuleUri;

  private String uiUriPath;

  private WorkflowUiUriType uiUriType;

  private String workflowProviderApiUriPath;

  private Map<String, Object> details = new HashMap<>();

  private String detailsFulltextSearch;

  private List<String> i18nLanguages;

  private Object templateContext;

  private String eventId;

  private OffsetDateTime eventTimestamp;


  public WorkflowCreatedEvent() {
  }

  public WorkflowCreatedEvent(String workflowModuleId, List<String> i18nLanguages) {
    this.workflowModule = workflowModuleId;
    this.i18nLanguages = i18nLanguages;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
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

  public String getWorkflowModule() {
    return workflowModule;
  }

  public void setWorkflowModule(String workflowModule) {
    this.workflowModule = workflowModule;
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

  public String getWorkflowModuleUri() {
    return workflowModuleUri;
  }

  public void setWorkflowModuleUri(String workflowModuleUri) {
    this.workflowModuleUri = workflowModuleUri;
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

  public String getWorkflowProviderApiUriPath() {
    return workflowProviderApiUriPath;
  }

  public void setWorkflowProviderApiUriPath(String workflowProviderApiUriPath) {
    this.workflowProviderApiUriPath = workflowProviderApiUriPath;
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

}

