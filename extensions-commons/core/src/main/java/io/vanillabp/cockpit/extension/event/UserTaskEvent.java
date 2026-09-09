package io.vanillabp.cockpit.extension.event;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.vanillabp.cockpit.extension.config.UiUriType;
import io.vanillabp.cockpit.extension.spi.UserTaskEventKind;
import io.vanillabp.spi.cockpit.usertask.NotificationDelivery;
import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTask;

/**
 * One user-task event on its way to the cockpit server, and at the same time the object a
 * <code>&#64;UserTaskDetailsProvider</code> method receives and enriches.
 * <p>
 * It is one class for all four kinds of event, with {@link #getEventKind()} saying which (see
 * decision 9 in the repository's DECISIONS.md). The
 * mappers of both transports read the kind; a lifecycle event simply leaves the fields nobody
 * filled at their defaults, which is what the cockpit's lifecycle endpoints accept.
 * <p>
 * Being both the event and the <code>PrefilledUserTaskDetails</code> parameter is what makes a
 * details provider which only calls setters need no copying afterwards: it wrote into this
 * object. A provider returning an object of its own is the other case, and
 * {@link #applyReturnedDetails} handles it.
 */
public class UserTaskEvent implements PrefilledUserTaskDetails, UserTask {

  private final UserTaskEventKind eventKind;

  private String eventId;

  private String userTaskId;

  private String initiator;

  private OffsetDateTime timestamp;

  private String source;

  private String workflowModuleId;

  private String comment;

  private String bpmnProcessId;

  private String bpmnProcessVersion;

  private Map<String, String> workflowTitle = new HashMap<>();

  private String workflowId;

  private String subWorkflowId;

  private String businessId;

  private Map<String, String> title = new HashMap<>();

  private String bpmnTaskId;

  private String taskDefinition;

  private Map<String, String> taskDefinitionTitle = new HashMap<>();

  private String uiUriPath;

  private UiUriType uiUriType;

  private String assignee;

  private List<String> candidateUsers = new ArrayList<>();

  private List<String> candidateGroups = new ArrayList<>();

  private List<String> excludedCandidateUsers = new ArrayList<>();

  private OffsetDateTime dueDate;

  private OffsetDateTime followUpDate;

  private Map<String, Object> details = new HashMap<>();

  private String detailsFulltextSearch;

  private NotificationDelivery notificationDelivery;

  private Object templateContext;

  private List<String> i18nLanguages = new ArrayList<>();

  public UserTaskEvent(
      final UserTaskEventKind eventKind) {

    this.eventKind = eventKind;

  }

  /**
   * @return What happened to the task, which decides the endpoint respectively the protobuf
   *         message this event is sent as
   */
  public UserTaskEventKind getEventKind() {

    return eventKind;

  }

  /**
   * Takes over what a details provider returned as an object of its own.
   * <p>
   * A provider which enriched the object it was given returns that very object, and there is
   * nothing to take over - the comparison is by identity for exactly that reason. A provider
   * which built its own <code>UserTaskDetails</code> has filled only the fields the SPI lets
   * business code fill, so only those are read; everything the BPMS reported stays.
   *
   * @param details What the provider returned, or <code>null</code> where it returned nothing
   */
  public void applyReturnedDetails(
      final io.vanillabp.spi.cockpit.usertask.UserTaskDetails details) {

    if ((details == null) || (details == this)) {
      return;
    }
    setInitiator(details.getInitiator());
    setComment(details.getComment());
    setAssignee(details.getAssignee());
    setCandidateUsers(details.getCandidateUsers());
    setCandidateGroups(details.getCandidateGroups());
    setExcludedCandidateUsers(details.getExcludedCandidateUsers());
    setDueDate(details.getDueDate());
    setFollowUpDate(details.getFollowUpDate());
    setDetails(details.getDetails());
    setDetailsFulltextSearch(details.getDetailsFulltextSearch());
    setNotificationDelivery(details.getNotificationDelivery());
    setTemplateContext(details.getTemplateContext());
    setI18nLanguages(details.getI18nLanguages());
    if (details.getTitle() != null) {
      setTitle(details.getTitle());
    }
    if (details.getWorkflowTitle() != null) {
      setWorkflowTitle(details.getWorkflowTitle());
    }
    if (details.getTaskDefinitionTitle() != null) {
      setTaskDefinitionTitle(details.getTaskDefinitionTitle());
    }
    if (details.getUiUriPath() != null) {
      setUiUriPath(details.getUiUriPath());
    }

  }

  @Override
  public String getId() {

    return userTaskId;

  }

  @Override
  public String getEventId() {

    return eventId;

  }

  public void setEventId(
      final String eventId) {

    this.eventId = eventId;

  }

  public String getUserTaskId() {

    return userTaskId;

  }

  public void setUserTaskId(
      final String userTaskId) {

    this.userTaskId = userTaskId;

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
  public String getBpmnTaskId() {

    return bpmnTaskId;

  }

  public void setBpmnTaskId(
      final String bpmnTaskId) {

    this.bpmnTaskId = bpmnTaskId;

  }

  public String getWorkflowId() {

    return workflowId;

  }

  public void setWorkflowId(
      final String workflowId) {

    this.workflowId = workflowId;

  }

  public String getSubWorkflowId() {

    return subWorkflowId;

  }

  public void setSubWorkflowId(
      final String subWorkflowId) {

    this.subWorkflowId = subWorkflowId;

  }

  public String getBusinessId() {

    return businessId;

  }

  public void setBusinessId(
      final String businessId) {

    this.businessId = businessId;

  }

  @Override
  public String getTaskDefinition() {

    return taskDefinition;

  }

  public void setTaskDefinition(
      final String taskDefinition) {

    this.taskDefinition = taskDefinition;

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
  public Map<String, String> getWorkflowTitle() {

    return workflowTitle;

  }

  @Override
  public void setWorkflowTitle(
      final Map<String, String> workflowTitle) {

    this.workflowTitle = workflowTitle;

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
  public Map<String, String> getTaskDefinitionTitle() {

    return taskDefinitionTitle;

  }

  @Override
  public void setTaskDefinitionTitle(
      final Map<String, String> taskDefinitionTitle) {

    this.taskDefinitionTitle = taskDefinitionTitle;

  }

  @Override
  public String getAssignee() {

    return assignee;

  }

  @Override
  public void setAssignee(
      final String assignee) {

    this.assignee = assignee;

  }

  @Override
  public List<String> getCandidateUsers() {

    return candidateUsers;

  }

  @Override
  public void setCandidateUsers(
      final List<String> candidateUsers) {

    this.candidateUsers = candidateUsers;

  }

  @Override
  public List<String> getCandidateGroups() {

    return candidateGroups;

  }

  @Override
  public void setCandidateGroups(
      final List<String> candidateGroups) {

    this.candidateGroups = candidateGroups;

  }

  @Override
  public List<String> getExcludedCandidateUsers() {

    return excludedCandidateUsers;

  }

  @Override
  public void setExcludedCandidateUsers(
      final List<String> excludedCandidateUsers) {

    this.excludedCandidateUsers = excludedCandidateUsers;

  }

  @Override
  public OffsetDateTime getDueDate() {

    return dueDate;

  }

  @Override
  public void setDueDate(
      final OffsetDateTime dueDate) {

    this.dueDate = dueDate;

  }

  @Override
  public OffsetDateTime getFollowUpDate() {

    return followUpDate;

  }

  @Override
  public void setFollowUpDate(
      final OffsetDateTime followUpDate) {

    this.followUpDate = followUpDate;

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
  public NotificationDelivery getNotificationDelivery() {

    return notificationDelivery;

  }

  @Override
  public void setNotificationDelivery(
      final NotificationDelivery notificationDelivery) {

    this.notificationDelivery = notificationDelivery;

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
  public List<String> getI18nLanguages() {

    return i18nLanguages;

  }

  @Override
  public void setI18nLanguages(
      final List<String> i18nLanguages) {

    this.i18nLanguages = i18nLanguages;

  }

}
