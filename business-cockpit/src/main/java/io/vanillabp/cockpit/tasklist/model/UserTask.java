package io.vanillabp.cockpit.tasklist.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.data.annotation.AccessType;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import io.vanillabp.cockpit.commons.mongo.updateinfo.UpdateInformationAware;
import io.vanillabp.cockpit.commons.security.jwt.JwtUserDetails;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.util.candidates.CandidatesAware;

/**
 * A user task as the cockpit stores it: what the workflow modules reported about it, plus what
 * the cockpit itself records, like who read it and who took it.
 */
@Document(collection = UserTask.COLLECTION_NAME)
public class UserTask extends CandidatesAware implements UpdateInformationAware {

  /**
   * When a user read this user task.
   *
   * @param userId The id of the user
   * @param timestamp When the user read the task
   */
  public static record ReadBy(String userId, OffsetDateTime timestamp) {
  };

  /**
   * When the cockpit learned about a personal candidate of this user task.
   *
   *
   * @param userId The id of the candidate
   * @param timestamp When the cockpit learned that the user is a candidate
   * @see UserTask#getCandidateUsersSince()
   */
  public static record CandidateSince(String userId, OffsetDateTime timestamp) {
  };

  /** The name of the MongoDB collection the user tasks are stored in. */
  public static final String COLLECTION_NAME = "usertask";

  @Id
  private String id;

  @Version
  private long version;

  private String initiator;

  /**
   * When the reporting workflow system created this user task. It is never {@code null}.
   * <p>
   * A task the cockpit learned about from its end alone, because the end overtook the creation,
   * starts when the end says the task began. Where the end does not say it, the task starts at the
   * end. The creation arriving afterwards fills in what the end could not report, and its start
   * replaces this one. {@link #knownFromItsEndAlone} says which task still waits for its creation.
   *
   * @see #latestEventAt
   */
  private OffsetDateTime createdAt;

  /**
   * Whether the cockpit learned about this task from its end alone and still waits for its
   * creation. Only such a task takes everything from a creation which arrives later. Any other
   * task takes only its start, and only if it is earlier, see decision 18 in the repository's
   * DECISIONS.md. For tasks stored from their end alone before this property existed, a
   * changeset set it.
   */
  private boolean knownFromItsEndAlone;

  /**
   * When the event behind the latest report the cockpit stored about this user task happened,
   * measured by the reporting workflow system's clock.
   * <p>
   * Reports overtake each other, so the cockpit has to know how old its own state is before it
   * can refuse an older report. {@link #reportedAt} cannot answer that, because it is the
   * cockpit's clock. Neither can {@link #updatedAt}, which is audit information and is
   * overwritten on every save. {@code null} means the cockpit cannot say which event its state
   * came from, and the next report is then applied whatever its timestamp says. A task stored
   * before this property existed had its {@link #createdAt} copied here by a changeset, so a
   * task the cockpit knows from its end alone is the one which stays without a value.
   */
  private OffsetDateTime latestEventAt;

  /**
   * When the cockpit stored the report about this user task, measured by the cockpit's own
   * clock. {@link #createdAt} is the other one, the timestamp of the reporting workflow system.
   * <p>
   * The scan for notifications needs it to tell a newly reported task from an updated one. The
   * cursor of that scan moves with the cockpit's clock. Comparing it to a foreign clock, or to
   * the timestamp of an event which arrived late, drops notifications without a word.
   */
  private OffsetDateTime reportedAt;

  private OffsetDateTime updatedAt;

  private String updatedBy;

  private OffsetDateTime endedAt;

  private String source;

  private String workflowModuleId;

  private String comment;

  private String bpmnProcessId;

  private String bpmnProcessVersion;

  private Map<String, String> workflowTitle = null;

  private String businessId;

  private String workflowId;

  private String subWorkflowId;

  private Map<String, String> title;

  private String bpmnTaskId;

  private String taskDefinition;

  private Map<String, String> taskDefinitionTitle = null;

  private String uiUriPath;

  private String uiUriType;

  private Person assignee;

  private List<Person> candidateUsers = null;

  /**
   * When the cockpit learned about each personal candidate, either as the task was reported or
   * as it was assigned in the cockpit. {@link #addCandidatePerson(Person)} and
   * {@link #removeCandidatePerson(String)} keep it up to date, and
   * {@link #stampCandidatesSince(OffsetDateTime)} does so for the candidates a report brought
   * along.
   * <p>
   * A candidate has to be notified when they <i>become</i> a candidate, and not on every later
   * change of the task. The outbox alone cannot tell the two apart, because its entries are
   * cleaned up after a while and a proposal repeated after that turns into a second message.
   */
  private List<CandidateSince> candidateUsersSince = null;

  private List<Group> candidateGroups = null;

  private List<Person> excludedCandidateUsers = null;

  /**
   * Users the workflow module let through although they are no candidate. Whoever is in here
   * sees the task whatever the candidates say, and even where {@link #excludedCandidateUsers}
   * names them. That is how a module keeps a case readable for everybody who had a hand in it.
   */
  private List<Person> admittedUsers = null;

  private OffsetDateTime dueDate;

  private OffsetDateTime followUpDate;

  private Map<String, Object> details = null;

  private String detailsFulltextSearch;

  private List<ReadBy> readBy;

  /**
   * How the workflow module wants notifications for this user task to be delivered.
   * {@code null} is interpreted as {@link io.vanillabp.spi.cockpit.usertask.NotificationDelivery#USER_CONFIG}.
   * Precedence: FORCE &gt; SUPPRESS &gt; user-config.
   */
  private io.vanillabp.spi.cockpit.usertask.NotificationDelivery notificationDelivery;

  /**
   * Why the task ended, and {@code null} while it is open. It lets the notification poller tell
   * a completion from a cancellation.
   */
  private UserTaskEndReason endReason;

  /**
   * Carries the kind of change a notification is about. The notification poller sets it before
   * it calls {@code NotificationService#sendNotification}. It is never stored.
   */
  @org.springframework.data.annotation.Transient
  private io.vanillabp.cockpit.notification.NotificationType notificationType;

  /**
   * Says whether the workflow module forced this notification. The notification poller sets it
   * before it calls {@code NotificationService#sendNotification}. It is never stored.
   */
  @org.springframework.data.annotation.Transient
  private boolean forced;

  /**
   * Creates an empty user task. Used by Spring Data when it reads a document, and by the cockpit
   * before it fills in a report.
   */
  public UserTask() {

  }

  @Override
  protected List<String> getGroupIds() {
    return Optional
        .ofNullable(getCandidateGroups())
        .orElse(List.of())
        .stream()
        .map(Group::getId)
        .filter(Objects::nonNull)
        .toList();
  }

  @Override
  protected List<String> getUserIds() {
    return Optional
        .ofNullable(getCandidateUsers())
        .orElse(List.of())
        .stream()
        .map(Person::getId)
        .filter(Objects::nonNull)
        .toList();
  }

  /**
   * The candidate groups and users, plus the assignee. A user id carries the prefix
   * {@link JwtUserDetails#USER_AUTHORITY_PREFIX}.
   *
   * @return The target groups, or {@code null} if the task has no candidates, so everybody may
   *         see it
   */
  public Collection<String> getTargetGroups() {

    final var result = super.getTargetGroups();
    if (result == null) {
      return null;
    }
    if (getAssignee() != null) {
      result.add(JwtUserDetails.USER_AUTHORITY_PREFIX + getAssignee().getId());
    }
    return result;

  }

  /**
   * Adds a person to the candidate users, or replaces the entry of the same id. It also records
   * that the person is a candidate as of now.
   *
   * @param person The person to add. Nothing happens if it is {@code null}.
   */
  public void addCandidatePerson(
      final Person person) {

    if (person == null) {
      return;
    }
    if (getCandidateUsers() == null) {
      setCandidateUsers(new ArrayList<>(List.of(person)));
    } else {
      this.getCandidateUsers().removeIf(candidate -> candidate.getId().equals(person.getId()));
      this.getCandidateUsers().add(person);
    }
    stampCandidateSince(person.getId(), OffsetDateTime.now());

  }

  /**
   * Removes a person from the candidate users, and forgets since when they were one.
   *
   * @param personId The id of the person. Nothing happens if it is {@code null}.
   */
  public void removeCandidatePerson(
      final String personId) {

    if (personId == null) {
      return;
    }
    if (getCandidateUsersSince() != null) {
      this.getCandidateUsersSince().removeIf(since -> since.userId().equals(personId));
    }
    if ((getCandidateUsers() == null) || getCandidateUsers().isEmpty()) {
      return;
    }

    this.getCandidateUsers().removeIf(candidate -> candidate.getId().equals(personId));

  }

  /**
   * Since when the given user is a personal candidate.
   *
   * @param userId a user
   * @return When the cockpit learned that the user is a personal candidate. {@code null} where
   *         the user is no candidate, or where it is unknown since when they are one
   */
  public OffsetDateTime getCandidateSince(
      final String userId) {

    if ((userId == null) || (getCandidateUsersSince() == null)) {
      return null;
    }
    return getCandidateUsersSince()
        .stream()
        .filter(since -> since.userId().equals(userId))
        .map(CandidateSince::timestamp)
        .findFirst()
        .orElse(null);

  }

  /**
   * Records the given timestamp for every personal candidate which carries none yet. It is used
   * for the candidates a report brought along.
   *
   * @param timestamp when the cockpit learned about those candidates
   */
  public void stampCandidatesSince(
      final OffsetDateTime timestamp) {

    Optional
        .ofNullable(getCandidateUsers())
        .orElse(List.of())
        .stream()
        .map(Person::getId)
        .filter(userId -> getCandidateSince(userId) == null)
        .forEach(userId -> stampCandidateSince(userId, timestamp));

  }

  private void stampCandidateSince(
      final String userId,
      final OffsetDateTime timestamp) {

    if (getCandidateUsersSince() == null) {
      setCandidateUsersSince(new ArrayList<>());
    }
    getCandidateUsersSince().removeIf(since -> since.userId().equals(userId));
    getCandidateUsersSince().add(new CandidateSince(userId, timestamp));

  }

  /**
   * When the given user read the task.
   *
   * @param userId The id of the user
   * @return The time, or {@code null} if the user has not read the task
   */
  public OffsetDateTime getReadAt(
      final String userId) {

    if (userId == null) {
      return null;
    }
    if (this.getReadBy() == null) {
      return null;
    }
    return this
        .getReadBy()
        .stream()
        .filter(readBy -> readBy.userId().equals(userId))
        .findFirst()
        .map(ReadBy::timestamp)
        .orElse(null);

  }

  /**
   * Records that the given user read the task now. An earlier entry of the same user is replaced.
   *
   * @param userId The id of the user
   */
  public void setReadAt(
      final String userId) {

    final var newReadBy = new ReadBy(userId, OffsetDateTime.now());
    if (this.getReadBy() == null) {
      // a modifiable list: the next reader has to be able to add himself
      this.setReadBy(new ArrayList<>(List.of(newReadBy)));
    } else {
      this.getReadBy().removeIf(readBy -> readBy.userId().equals(userId));
      this.getReadBy().add(newReadBy);
    }

  }

  /**
   * Forgets that the given user read the task.
   *
   * @param userId The id of the user
   */
  public void clearReadAt(
      final String userId) {

    if (this.getReadBy() == null) {
      return;
    }
    this.getReadBy().removeIf(readBy -> readBy.userId.equals(userId));

  }

  /**
   * Whether the task addresses nobody: no candidate user, no candidate group and no assignee.
   * The value is stored, so a query can find such tasks.
   *
   * @return {@code true} if the task addresses nobody
   */
  @AccessType(AccessType.Type.PROPERTY)
  public boolean isDangling() {

    if ((getCandidateUsers() != null) && !getCandidateUsers().isEmpty()) {
      return false;
    }
    if ((getCandidateGroups() != null) && !getCandidateGroups().isEmpty()) {
      return false;
    }
    if (getAssignee() != null) {
      return false;
    }
    return true;

  }

  /**
   * Does nothing, because the value is derived from the candidates and the assignee. Spring
   * Data calls it when it reads a document.
   *
   * @param dangling Ignored
   * @see #isDangling()
   */
  public void setDangling(
      boolean dangling) {
    // ignored since 'dangling' is a derived value
  }

  /**
   * The id of the user task, the same one the workflow module reports.
   *
   * @return The id
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the id of the user task.
   *
   * @param id The id
   */
  public void setId(
      String id) {
    this.id = id;
  }

  /**
   * The version of the document, used by Spring Data for optimistic locking.
   *
   * @return The version
   */
  public long getVersion() {
    return version;
  }

  /**
   * Sets the version of the document.
   *
   * @param version The version
   */
  public void setVersion(
      long version) {
    this.version = version;
  }

  /**
   * Who caused the latest change of the task: the user who changed it in the cockpit, or what the
   * workflow module reported. The notification poller reads it to skip a notification a user
   * caused themselves.
   *
   * @return The id of the user, or a name for the cockpit or the system
   */
  public String getInitiator() {
    return initiator;
  }

  /**
   * Sets who caused the latest change.
   *
   * @param initiator The id of the user, or a name for the cockpit or the system
   */
  public void setInitiator(
      String initiator) {
    this.initiator = initiator;
  }

  /**
   * When the reporting workflow system created this user task. A task the cockpit knows from
   * its end alone starts when the end says it began, or at its end.
   *
   * @return The start, never {@code null} once stored
   */
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  /**
   * Sets when the user task was created.
   *
   * @param createdAt The start
   */
  public void setCreatedAt(
      OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  /**
   * Whether the cockpit learned about this user task from its end alone and still waits for its
   * creation.
   *
   * @return {@code true} if the creation has not arrived yet
   */
  public boolean isKnownFromItsEndAlone() {
    return knownFromItsEndAlone;
  }

  /**
   * Sets whether the cockpit still waits for the creation.
   *
   * @param knownFromItsEndAlone {@code true} if the creation has not arrived yet
   */
  public void setKnownFromItsEndAlone(
      boolean knownFromItsEndAlone) {
    this.knownFromItsEndAlone = knownFromItsEndAlone;
  }

  /**
   * When the event behind the latest stored report about this user task happened, by the clock of
   * the reporting workflow system.
   *
   * @return The time of the latest event, or {@code null} if it is not known
   */
  public OffsetDateTime getLatestEventAt() {
    return latestEventAt;
  }

  /**
   * Sets when the event behind the latest stored report happened.
   *
   * @param latestEventAt The time of the latest event
   */
  public void setLatestEventAt(
      OffsetDateTime latestEventAt) {
    this.latestEventAt = latestEventAt;
  }

  /**
   * When the cockpit stored the report about this user task, by the cockpit's own clock.
   *
   * @return The time the report was stored
   */
  public OffsetDateTime getReportedAt() {
    return reportedAt;
  }

  /**
   * Sets when the cockpit stored the report.
   *
   * @param reportedAt The time the report was stored
   */
  public void setReportedAt(
      OffsetDateTime reportedAt) {
    this.reportedAt = reportedAt;
  }

  /**
   * When the user task was saved the last time. It is audit information and changes on every
   * save.
   *
   * @return The time of the last save
   */
  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  /**
   * Sets when the document was saved the last time.
   *
   * @param updatedAt The time of the last save
   */
  public void setUpdatedAt(
      OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  /**
   * Who saved the user task the last time. It is audit information and changes on every save.
   *
   * @return The id of the user, or a name for the system
   */
  public String getUpdatedBy() {
    return updatedBy;
  }

  @Override
  public void setUpdatedBy(
      String userId) {
    this.updatedBy = userId;
  }

  /**
   * When the user task ended.
   *
   * @return The time of the end, or {@code null} while the user task is open
   */
  public OffsetDateTime getEndedAt() {
    return endedAt;
  }

  /**
   * Sets when the user task ended.
   *
   * @param endedAt The time of the end
   */
  public void setEndedAt(
      OffsetDateTime endedAt) {
    this.endedAt = endedAt;
  }

  /**
   * The name of the system which sent the report.
   *
   * @return The name of the system
   */
  public String getSource() {
    return source;
  }

  /**
   * Sets the name of the system which sent the report.
   *
   * @param source The name of the system
   */
  public void setSource(
      String source) {
    this.source = source;
  }

  /**
   * The id of the workflow module the user task belongs to.
   *
   * @return The id of the workflow module
   */
  public String getWorkflowModuleId() {
    return workflowModuleId;
  }

  /**
   * Sets the id of the workflow module.
   *
   * @param workflowModuleId The id of the workflow module
   */
  public void setWorkflowModuleId(
      String workflowModuleId) {
    this.workflowModuleId = workflowModuleId;
  }

  /**
   * A comment the workflow module sent with the latest event, for example why something was
   * cancelled.
   *
   * @return The comment, or {@code null}
   */
  public String getComment() {
    return comment;
  }

  /**
   * Sets the comment of the latest event.
   *
   * @param comment The comment
   */
  public void setComment(
      String comment) {
    this.comment = comment;
  }

  /**
   * The id of the BPMN process.
   *
   * @return The id of the BPMN process
   */
  public String getBpmnProcessId() {
    return bpmnProcessId;
  }

  /**
   * Sets the id of the BPMN process.
   *
   * @param bpmnProcessId The id of the BPMN process
   */
  public void setBpmnProcessId(
      String bpmnProcessId) {
    this.bpmnProcessId = bpmnProcessId;
  }

  /**
   * The version of the BPMN process.
   *
   * @return The version of the BPMN process
   */
  public String getBpmnProcessVersion() {
    return bpmnProcessVersion;
  }

  /**
   * Sets the version of the BPMN process.
   *
   * @param bpmnProcessVersion The version of the BPMN process
   */
  public void setBpmnProcessVersion(
      String bpmnProcessVersion) {
    this.bpmnProcessVersion = bpmnProcessVersion;
  }

  /**
   * The title of the workflow the task belongs to, by language.
   *
   * @return The titles, keyed by the language
   */
  public Map<String, String> getWorkflowTitle() {
    return workflowTitle;
  }

  /**
   * Sets the title of the workflow, by language.
   *
   * @param workflowTitle The titles, keyed by the language
   */
  public void setWorkflowTitle(
      Map<String, String> workflowTitle) {
    this.workflowTitle = workflowTitle;
  }

  /**
   * The business id of the workflow, for example an order id.
   *
   * @return The business id
   */
  public String getBusinessId() {
    return businessId;
  }

  /**
   * Sets the business id of the workflow.
   *
   * @param businessId The business id
   */
  public void setBusinessId(
      String businessId) {
    this.businessId = businessId;
  }

  /**
   * The id of the workflow the task belongs to.
   *
   * @return The id of the workflow
   */
  public String getWorkflowId() {
    return workflowId;
  }

  /**
   * Sets the id of the workflow.
   *
   * @param workflowId The id of the workflow
   */
  public void setWorkflowId(
      String workflowId) {
    this.workflowId = workflowId;
  }

  /**
   * The title of the user task, by language.
   *
   * @return The titles, keyed by the language
   */
  public Map<String, String> getTitle() {
    return title;
  }

  /**
   * Sets the title of the user task, by language.
   *
   * @param title The titles, keyed by the language
   */
  public void setTitle(
      Map<String, String> title) {
    this.title = title;
  }

  /**
   * The id of the user task in the BPMN model.
   *
   * @return The id of the BPMN element
   */
  public String getBpmnTaskId() {
    return bpmnTaskId;
  }

  /**
   * Sets the id of the user task in the BPMN model.
   *
   * @param bpmnTaskId The id of the BPMN element
   */
  public void setBpmnTaskId(
      String bpmnTaskId) {
    this.bpmnTaskId = bpmnTaskId;
  }

  /**
   * The form key of the task, which tells the user interface which form to show.
   *
   * @return The form key
   */
  public String getTaskDefinition() {
    return taskDefinition;
  }

  /**
   * Sets the form key of the task.
   *
   * @param taskDefinition The form key
   */
  public void setTaskDefinition(
      String taskDefinition) {
    this.taskDefinition = taskDefinition;
  }

  /**
   * The title of the form, by language.
   *
   * @return The titles, keyed by the language
   */
  public Map<String, String> getTaskDefinitionTitle() {
    return taskDefinitionTitle;
  }

  /**
   * Sets the title of the form, by language.
   *
   * @param taskDefinitionTitle The titles, keyed by the language
   */
  public void setTaskDefinitionTitle(
      Map<String, String> taskDefinitionTitle) {
    this.taskDefinitionTitle = taskDefinitionTitle;
  }

  /**
   * Where the user interface of this user task is found, as the workflow module reported it. The
   * cockpit hands it out unchanged.
   *
   * @return The path
   */
  public String getUiUriPath() {
    return uiUriPath;
  }

  /**
   * Sets where the user interface is found.
   *
   * @param uiUriPath The path
   */
  public void setUiUriPath(
      String uiUriPath) {
    this.uiUriPath = uiUriPath;
  }

  /**
   * Which kind of user interface the workflow module brings. The cockpit carries the value and
   * never reads it.
   *
   * @return The kind of user interface
   */
  public String getUiUriType() {
    return uiUriType;
  }

  /**
   * Sets which kind of user interface the workflow module brings.
   *
   * @param uiUriType The kind of user interface
   */
  public void setUiUriType(
      String uiUriType) {
    this.uiUriType = uiUriType;
  }

  /**
   * The person who works on the task.
   *
   * @return The assignee, or {@code null}
   */
  public Person getAssignee() {
    return assignee;
  }

  /**
   * Sets the person who works on the task.
   *
   * @param assignee The assignee, or {@code null}
   */
  public void setAssignee(
      Person assignee) {
    this.assignee = assignee;
  }

  /**
   * The users who may work on the task.
   *
   * @return The candidate users, or {@code null}
   */
  public List<Person> getCandidateUsers() {
    return candidateUsers;
  }

  /**
   * When the cockpit learned about each personal candidate. A candidate is notified when they
   * become one, and not on every later change of the task.
   *
   * @return The timestamps, or {@code null}
   */
  public List<CandidateSince> getCandidateUsersSince() {
    return candidateUsersSince;
  }

  /**
   * Sets when the cockpit learned about each personal candidate.
   *
   * @param candidateUsersSince The timestamps
   */
  public void setCandidateUsersSince(
      List<CandidateSince> candidateUsersSince) {
    this.candidateUsersSince = candidateUsersSince;
  }

  /**
   * Sets the users who may work on the task.
   *
   * @param candidateUsers The candidate users
   */
  public void setCandidateUsers(
      List<Person> candidateUsers) {
    this.candidateUsers = candidateUsers;
  }

  /**
   * The groups whose members may work on the task.
   *
   * @return The candidate groups, or {@code null}
   */
  public List<Group> getCandidateGroups() {
    return candidateGroups;
  }

  /**
   * Sets the groups whose members may work on the task.
   *
   * @param candidateGroups The candidate groups
   */
  public void setCandidateGroups(
      List<Group> candidateGroups) {
    this.candidateGroups = candidateGroups;
  }

  /**
   * The users who may not work on the task, even if they are candidates. This is how a workflow
   * module enforces four eyes.
   *
   * @return The excluded users, or {@code null}
   */
  public List<Person> getExcludedCandidateUsers() {
    return excludedCandidateUsers;
  }

  /**
   * Sets the users who may not work on the task.
   *
   * @param excludedCandidateUsers The excluded users
   */
  public void setExcludedCandidateUsers(
      List<Person> excludedCandidateUsers) {
    this.excludedCandidateUsers = excludedCandidateUsers;
  }

  /**
   * The users who may reach the task although they are no candidate. They see the task even
   * where {@link #getExcludedCandidateUsers()} names them.
   *
   * @return The admitted users, or {@code null}
   */
  public List<Person> getAdmittedUsers() {
    return admittedUsers;
  }

  /**
   * Sets the users who may reach the task although they are no candidate.
   *
   * @param admittedUsers The admitted users
   */
  public void setAdmittedUsers(
      List<Person> admittedUsers) {
    this.admittedUsers = admittedUsers;
  }

  /**
   * When the task has to be done. A task without a due date stores {@link OffsetDateTime#MAX}, so
   * it sorts behind every real date.
   *
   * @return The due date
   */
  public OffsetDateTime getDueDate() {
    return dueDate;
  }

  /**
   * Sets when the task has to be done.
   *
   * @param dueDate The due date
   */
  public void setDueDate(
      OffsetDateTime dueDate) {
    this.dueDate = dueDate;
  }

  /**
   * Until when a user put the task aside. Before this date, some lists do not show it.
   *
   * @return The follow-up date, or {@code null}
   */
  public OffsetDateTime getFollowUpDate() {
    return followUpDate;
  }

  /**
   * Sets until when the task is put aside.
   *
   * @param followUpDate The follow-up date, or {@code null}
   */
  public void setFollowUpDate(
      OffsetDateTime followUpDate) {
    this.followUpDate = followUpDate;
  }

  /**
   * The business data the workflow module reported for this user task.
   *
   * @return The business data, keyed by name
   */
  public Map<String, Object> getDetails() {
    return details;
  }

  /**
   * Sets the business data.
   *
   * @param details The business data, keyed by name
   */
  public void setDetails(
      Map<String, Object> details) {
    this.details = details;
  }

  /**
   * The words of the business data, for a filter without a path.
   *
   * @return The words
   */
  public String getDetailsFulltextSearch() {
    return detailsFulltextSearch;
  }

  /**
   * Sets the words of the business data for the full text search.
   *
   * @param detailsFulltextSearch The words
   */
  public void setDetailsFulltextSearch(
      String detailsFulltextSearch) {
    this.detailsFulltextSearch = detailsFulltextSearch;
  }

  /**
   * The id of the call activity which created the task, or the id of the workflow.
   *
   * @return The id
   */
  public String getSubWorkflowId() {
    return subWorkflowId;
  }

  /**
   * Sets the id of the call activity which created the task.
   *
   * @param subWorkflowId The id
   */
  public void setSubWorkflowId(
      String subWorkflowId) {
    this.subWorkflowId = subWorkflowId;
  }

  /**
   * Who read the task, and when.
   *
   * @return The readers, or {@code null}
   */
  public List<ReadBy> getReadBy() {
    return readBy;
  }

  /**
   * Sets who read the task, and when.
   *
   * @param readBy The readers
   */
  public void setReadBy(
      List<ReadBy> readBy) {
    this.readBy = readBy;
  }

  /**
   * How the workflow module wants notifications about this task to be delivered. Precedence:
   * FORCE &gt; SUPPRESS &gt; the user's own configuration.
   *
   * @return The kind of delivery, or {@code null} for the user's own configuration
   */
  public io.vanillabp.spi.cockpit.usertask.NotificationDelivery getNotificationDelivery() {
    return notificationDelivery;
  }

  /**
   * Sets how notifications about this task are delivered.
   *
   * @param notificationDelivery The kind of delivery
   */
  public void setNotificationDelivery(
      io.vanillabp.spi.cockpit.usertask.NotificationDelivery notificationDelivery) {
    this.notificationDelivery = notificationDelivery;
  }

  /**
   * Why the task ended.
   *
   * @return The reason, or {@code null} while the task is open
   */
  public UserTaskEndReason getEndReason() {
    return endReason;
  }

  /**
   * Sets why the task ended.
   *
   * @param endReason The reason
   */
  public void setEndReason(
      UserTaskEndReason endReason) {
    this.endReason = endReason;
  }

  /**
   * The kind of change a notification is about. It is never stored.
   *
   * @return The kind of change
   */
  public io.vanillabp.cockpit.notification.NotificationType getNotificationType() {
    return notificationType;
  }

  /**
   * Sets the kind of change a notification is about.
   *
   * @param notificationType The kind of change
   */
  public void setNotificationType(
      io.vanillabp.cockpit.notification.NotificationType notificationType) {
    this.notificationType = notificationType;
  }

  /**
   * Whether the workflow module forced this notification. It is never stored.
   *
   * @return {@code true} if the notification is forced
   */
  public boolean isForced() {
    return forced;
  }

  /**
   * Sets whether the workflow module forced this notification.
   *
   * @param forced {@code true} if the notification is forced
   */
  public void setForced(
      boolean forced) {
    this.forced = forced;
  }

}
