package io.vanillabp.cockpit.tasklist.model;

import io.vanillabp.cockpit.commons.mongo.updateinfo.UpdateInformationAware;
import io.vanillabp.cockpit.commons.security.jwt.JwtUserDetails;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.util.candidates.CandidatesAware;
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

@Document(collection = UserTask.COLLECTION_NAME)
public class UserTask extends CandidatesAware implements UpdateInformationAware {

    public static record ReadBy(String userId, OffsetDateTime timestamp) {};

    /**
     * When the cockpit learned about a personal candidate of this user task.
     *
     * @see UserTask#getCandidateUsersSince()
     */
    public static record CandidateSince(String userId, OffsetDateTime timestamp) {};

    public static final String COLLECTION_NAME = "usertask";
    
    @Id
    private String id;
    
    @Version
    private long version;

    private String initiator;

    /**
     * When the reporting workflow system created this user task. {@code null} means the cockpit
     * never saw the creation and learned about the task from its end alone, which happens when the
     * two reports overtake each other. The creation arriving afterwards fills in what the end
     * could not report.
     *
     * @see #latestEventAt
     */
    private OffsetDateTime createdAt;

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

    private UiUriType uiUriType;

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

    public void removeCandidatePerson(
            final String personId) {

        if (personId == null) {
            return;
        }
        if (getCandidateUsersSince() != null) {
            this.getCandidateUsersSince().removeIf(since -> since.userId().equals(personId));
        }
        if ((getCandidateUsers() == null)
                || getCandidateUsers().isEmpty()) {
            return;
        }

        this.getCandidateUsers().removeIf(candidate -> candidate.getId().equals(personId));

    }

    /**
     * @param userId a user
     * @return When the cockpit learned that the user is a personal candidate. {@code null} where
     *         the user is no candidate, or where it is unknown since when they are one
     */
    public OffsetDateTime getCandidateSince(
            final String userId) {

        if ((userId == null)
                || (getCandidateUsersSince() == null)) {
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

    public OffsetDateTime getReadAt(final String userId) {

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

    public void clearReadAt(
            final String userId) {

        if (this.getReadBy() == null) {
            return;
        }
        this.getReadBy().removeIf(readBy -> readBy.userId.equals(userId));

    }

    @AccessType(AccessType.Type.PROPERTY)
    public boolean isDangling() {

        if ((getCandidateUsers() != null)
                && !getCandidateUsers().isEmpty()) {
            return false;
        }
        if ((getCandidateGroups() != null)
                && !getCandidateGroups().isEmpty()) {
            return false;
        }
        if (getAssignee() != null) {
            return false;
        }
        return true;

    }

    /**
     * @see #isDangling()
     */
    public void setDangling(boolean dangling) {
        // ignored since 'dangling' is a derived value
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public String getInitiator() {
        return initiator;
    }

    public void setInitiator(String initiator) {
        this.initiator = initiator;
    }
    
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getLatestEventAt() {
        return latestEventAt;
    }

    public void setLatestEventAt(OffsetDateTime latestEventAt) {
        this.latestEventAt = latestEventAt;
    }

    public OffsetDateTime getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(OffsetDateTime reportedAt) {
        this.reportedAt = reportedAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getUpdatedBy() {
        return updatedBy;
    }
    
    @Override
    public void setUpdatedBy(String userId) {
        this.updatedBy = userId;
    }

    public OffsetDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(OffsetDateTime endedAt) {
        this.endedAt = endedAt;
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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

    public Map<String, String> getWorkflowTitle() {
        return workflowTitle;
    }

    public void setWorkflowTitle(Map<String, String> workflowTitle) {
        this.workflowTitle = workflowTitle;
    }
    
    public String getBusinessId() {
        return businessId;
    }
    
    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }
    
    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public Map<String, String> getTitle() {
        return title;
    }

    public void setTitle(Map<String, String> title) {
        this.title = title;
    }

    public String getBpmnTaskId() {
        return bpmnTaskId;
    }
    
    public void setBpmnTaskId(String bpmnTaskId) {
        this.bpmnTaskId = bpmnTaskId;
    }
    
    public String getTaskDefinition() {
        return taskDefinition;
    }

    public void setTaskDefinition(String taskDefinition) {
        this.taskDefinition = taskDefinition;
    }

    public Map<String, String> getTaskDefinitionTitle() {
        return taskDefinitionTitle;
    }

    public void setTaskDefinitionTitle(Map<String, String> taskDefinitionTitle) {
        this.taskDefinitionTitle = taskDefinitionTitle;
    }

    public String getUiUriPath() {
        return uiUriPath;
    }

    public void setUiUriPath(String uiUriPath) {
        this.uiUriPath = uiUriPath;
    }

    public UiUriType getUiUriType() {
        return uiUriType;
    }

    public void setUiUriType(UiUriType uiUriType) {
        this.uiUriType = uiUriType;
    }

    public Person getAssignee() {
        return assignee;
    }

    public void setAssignee(Person assignee) {
        this.assignee = assignee;
    }

    public List<Person> getCandidateUsers() {
        return candidateUsers;
    }

    public List<CandidateSince> getCandidateUsersSince() {
        return candidateUsersSince;
    }

    public void setCandidateUsersSince(List<CandidateSince> candidateUsersSince) {
        this.candidateUsersSince = candidateUsersSince;
    }

    public void setCandidateUsers(List<Person> candidateUsers) {
        this.candidateUsers = candidateUsers;
    }

    public List<Group> getCandidateGroups() {
        return candidateGroups;
    }

    public void setCandidateGroups(List<Group> candidateGroups) {
        this.candidateGroups = candidateGroups;
    }

    public List<Person> getExcludedCandidateUsers() {
        return excludedCandidateUsers;
    }

    public void setExcludedCandidateUsers(List<Person> excludedCandidateUsers) {
        this.excludedCandidateUsers = excludedCandidateUsers;
    }

    public List<Person> getAdmittedUsers() {
        return admittedUsers;
    }

    public void setAdmittedUsers(List<Person> admittedUsers) {
        this.admittedUsers = admittedUsers;
    }

    public OffsetDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(OffsetDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public OffsetDateTime getFollowUpDate() {
        return followUpDate;
    }

    public void setFollowUpDate(OffsetDateTime followUpDate) {
        this.followUpDate = followUpDate;
    }

    public Map<String, Object> getDetails() {
        return details;
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

    public String getSubWorkflowId() {
        return subWorkflowId;
    }
    
    public void setSubWorkflowId(String subWorkflowId) {
        this.subWorkflowId = subWorkflowId;
    }

    public List<ReadBy> getReadBy() {
        return readBy;
    }

    public void setReadBy(List<ReadBy> readBy) {
        this.readBy = readBy;
    }

    public io.vanillabp.spi.cockpit.usertask.NotificationDelivery getNotificationDelivery() {
        return notificationDelivery;
    }

    public void setNotificationDelivery(
            io.vanillabp.spi.cockpit.usertask.NotificationDelivery notificationDelivery) {
        this.notificationDelivery = notificationDelivery;
    }

    public UserTaskEndReason getEndReason() {
        return endReason;
    }

    public void setEndReason(UserTaskEndReason endReason) {
        this.endReason = endReason;
    }

    public io.vanillabp.cockpit.notification.NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(io.vanillabp.cockpit.notification.NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public boolean isForced() {
        return forced;
    }

    public void setForced(boolean forced) {
        this.forced = forced;
    }

}
