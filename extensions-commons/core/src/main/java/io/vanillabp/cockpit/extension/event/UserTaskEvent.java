package io.vanillabp.cockpit.extension.event;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.vanillabp.cockpit.extension.spi.UserTaskEventKind;
import io.vanillabp.spi.cockpit.usertask.NotificationDelivery;
import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTask;

/**
 * One user-task event on its way to the cockpit server, and at the same time the object a
 * <code>&#64;UserTaskDetailsProvider</code> method receives and enriches.
 * <p>
 * It is one class for all four kinds of event, and {@link #getEventKind()} says which. See
 * decision 9 in the repository's DECISIONS.md. The mappers of both transports read the kind. A
 * lifecycle event simply leaves the fields nobody filled at their defaults, which is what the
 * cockpit's lifecycle endpoints accept.
 * <p>
 * The object is both the event and the <code>PrefilledUserTaskDetails</code> parameter. So a
 * details provider which only calls setters needs no copying afterwards, because it wrote into
 * this very object. A provider returning an object of its own is the other case, and
 * {@link #applyReturnedDetails} handles it.
 */
public class UserTaskEvent implements PrefilledUserTaskDetails, UserTask {

  private final UserTaskEventKind eventKind;

  private String eventId;

  private String userTaskId;

  private String initiator;

  private OffsetDateTime timestamp;

  /**
   * When the BPMS created the task, where the BPMS half could tell. Only the report of an end
   * carries it, in case the end arrives at the cockpit before the creation.
   */
  private OffsetDateTime createdAt;

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

  private String uiUriType;

  private String assignee;

  private List<String> candidateUsers = new ArrayList<>();

  private List<String> candidateGroups = new ArrayList<>();

  private List<String> excludedCandidateUsers = new ArrayList<>();

  private List<String> admittedUsers = new ArrayList<>();

  private OffsetDateTime dueDate;

  private OffsetDateTime followUpDate;

  private Map<String, Object> details = new HashMap<>();

  private String detailsFulltextSearch;

  private NotificationDelivery notificationDelivery;

  private Object templateContext;

  private List<String> i18nLanguages = new ArrayList<>();

  /**
   * Creates an empty event of one kind. The extension fills it, then the BPMS half and the details
   * provider add to it.
   *
   * @param eventKind What happened to the user task
   */
  public UserTaskEvent(
      final UserTaskEventKind eventKind) {

    this.eventKind = eventKind;

  }

  /**
   * The kind of this event.
   *
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
   * nothing to take over. That is why the comparison is by identity. A provider which built its
   * own <code>UserTaskDetails</code> filled only the fields the SPI lets business code fill, so
   * only those are read. Everything the BPMS reported stays.
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
    setAdmittedUsers(details.getAdmittedUsers());
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
   * The BPMS' id of the user task.
   *
   * @return The id of the user task
   */
  public String getUserTaskId() {

    return userTaskId;

  }

  /**
   * The BPMS' id of the user task.
   *
   * @param userTaskId The id of the user task
   */
  public void setUserTaskId(
      final String userTaskId) {

    this.userTaskId = userTaskId;

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
   * When the BPMS created the user task. Only the report of an end carries it.
   *
   * @return When the BPMS created the task, or <code>null</code> where the BPMS half could not
   *         tell
   */
  public OffsetDateTime getCreatedAt() {

    return createdAt;

  }

  /**
   * When the BPMS created the user task, where the BPMS half could tell.
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
   * The workflow module the user task belongs to.
   *
   * @return The id of the workflow module
   */
  public String getWorkflowModuleId() {

    return workflowModuleId;

  }

  /**
   * The workflow module the user task belongs to.
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
  public String getBpmnTaskId() {

    return bpmnTaskId;

  }

  /**
   * The BPMN element id of the user task.
   *
   * @param bpmnTaskId The element id
   */
  public void setBpmnTaskId(
      final String bpmnTaskId) {

    this.bpmnTaskId = bpmnTaskId;

  }

  /**
   * The BPMS' id of the workflow the user task belongs to.
   *
   * @return The id of the workflow
   */
  public String getWorkflowId() {

    return workflowId;

  }

  /**
   * The BPMS' id of the workflow the user task belongs to.
   *
   * @param workflowId The id of the workflow
   */
  public void setWorkflowId(
      final String workflowId) {

    this.workflowId = workflowId;

  }

  /**
   * The id of the called workflow the user task lives in, if there is one.
   *
   * @return The id of the called workflow, or <code>null</code>
   */
  public String getSubWorkflowId() {

    return subWorkflowId;

  }

  /**
   * The id of the called workflow the user task lives in, if there is one.
   *
   * @param subWorkflowId The id of the called workflow, or <code>null</code>
   */
  public void setSubWorkflowId(
      final String subWorkflowId) {

    this.subWorkflowId = subWorkflowId;

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
  public String getTaskDefinition() {

    return taskDefinition;

  }

  /**
   * The task definition of the user task, which the BPMN model names.
   *
   * @param taskDefinition The task definition
   */
  public void setTaskDefinition(
      final String taskDefinition) {

    this.taskDefinition = taskDefinition;

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
  public List<String> getAdmittedUsers() {

    return admittedUsers;

  }

  @Override
  public void setAdmittedUsers(
      final List<String> admittedUsers) {

    this.admittedUsers = admittedUsers;

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
