package io.vanillabp.cockpit.workflowlist.model;

import io.vanillabp.cockpit.commons.mongo.updateinfo.UpdateInformationAware;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.util.candidates.CandidatesAware;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.annotation.AccessType;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;


/**
 * A workflow, which is one business case, as the cockpit stores it: what the workflow modules
 * reported about it.
 */
@Document(collection = Workflow.COLLECTION_NAME)
public class Workflow extends CandidatesAware implements UpdateInformationAware {

    /** The name of the MongoDB collection the workflows are stored in. */
    public static final String COLLECTION_NAME = "workflow";

    @Id
    private String id;

    @Version
    private long version;

    private Person initiator;

    /**
     * When the reporting workflow system started this workflow. It is never {@code null}.
     * <p>
     * A case the cockpit learned about from its end alone, because the end overtook the creation,
     * starts when the end says the case began. Where the end does not say it, the case starts at the
     * end. The creation arriving afterwards fills in what the end could not report, and its start
     * replaces this one. {@link #knownFromItsEndAlone} says which case still waits for its creation.
     *
     * @see #latestEventAt
     */
    private OffsetDateTime createdAt;

    /**
     * Whether the cockpit learned about this case from its end alone and still waits for its
     * creation. Only such a case takes everything from a creation which arrives later. Any other
     * case takes only its start, and only if it is earlier, see decision 18 in the repository's
     * DECISIONS.md. For cases stored from their end alone before this property existed, a
     * changeset set it.
     */
    private boolean knownFromItsEndAlone;

    /**
     * When the event behind the latest report the cockpit stored about this workflow happened,
     * measured by the reporting workflow system's clock.
     * <p>
     * Reports overtake each other, so the cockpit has to know how old its own state is before it
     * can refuse an older report. {@link #reportedAt} cannot answer that, because it is the
     * cockpit's clock. Neither can {@link #updatedAt}, which is audit information and is
     * overwritten on every save. {@code null} means the cockpit cannot say which event its state
     * came from, and the next report is then applied whatever its timestamp says. A workflow
     * stored before this property existed had its {@link #createdAt} copied here by a changeset,
     * so a case the cockpit knows from its end alone is the one which stays without a value.
     */
    private OffsetDateTime latestEventAt;

    /**
     * When the cockpit stored the report about this workflow, measured by the cockpit's own
     * clock. {@link #createdAt} is the other one, the timestamp of the reporting workflow system.
     * This property is kept in step with the one of the same name on a user task. So a scan for
     * what changed, the way the notification poller scans user tasks, can tell a newly reported
     * workflow from an updated one without comparing its cursor to a foreign clock.
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

    private String businessId;

    private Map<String, String> title;

    private String uiUriPath;

    private String uiUriType;

    private List<Person> accessibleToUsers;

    private List<Group> accessibleToGroups;

    private Map<String, Object> details = null;

    private String detailsFulltextSearch;

    /**
     * Creates an empty workflow. Used by Spring Data when it reads a document, and by the cockpit
     * before it fills in a report.
     */
    public Workflow() {

    }

    @Override
    protected List<String> getGroupIds() {
        return Optional
                .ofNullable(getAccessibleToGroups())
                .orElse(List.of())
                .stream()
                .map(Group::getId)
                .toList();
    }

    @Override
    protected List<String> getUserIds() {
        return Optional
                .ofNullable(getAccessibleToUsers())
                .orElse(List.of())
                .stream()
                .map(Person::getId)
                .toList();
    }

    /**
     * Adds a person to the users the workflow is accessible to, or replaces the entry of the same
     * id.
     *
     * @param person The person to add. Nothing happens if it is {@code null}.
     */
    public void addPersonForAccess(
            final Person person) {

        if (person == null) {
            return;
        }
        if (accessibleToUsers == null) {
            setAccessibleToUsers(List.of(person));
        } else {
            this.getAccessibleToUsers().removeIf(candidate -> candidate.getId().equals(person.getId()));
            this.getAccessibleToUsers().add(person);
        }

    }

    /**
     * Removes a person from the users the workflow is accessible to.
     *
     * @param personId The id of the person. Nothing happens if it is {@code null}.
     */
    public void removeUserForAccess(
            final String personId) {

        if (personId == null) {
            return;
        }
        if ((getAccessibleToUsers() == null)
                || getAccessibleToUsers().isEmpty()) {
            return;
        }

        this.getAccessibleToUsers().removeIf(candidate -> candidate.getId().equals(personId));

    }

    /**
     * Whether the workflow addresses nobody: no user and no group it is accessible to. The value is
     * stored, so a query can find such workflows.
     *
     * @return {@code true} if the workflow addresses nobody
     */
    @AccessType(AccessType.Type.PROPERTY)
    public boolean isDangling() {

        if ((getAccessibleToUsers() != null)
                && !getAccessibleToUsers().isEmpty()) {
            return false;
        }
        if ((getAccessibleToGroups() != null)
                && !getAccessibleToGroups().isEmpty()) {
            return false;
        }
        return true;

    }

    /**
     * Does nothing, because the value is derived from the users and groups. Spring Data calls it
     * when it reads a document.
     *
     * @param dangling Ignored
     * @see #isDangling()
     */
    public void setDangling(boolean dangling) {
        // ignored since 'dangling' is a derived value
    }

    /**
     * The id of the workflow, the same one the workflow module reports.
     *
     * @return The id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the id of the workflow.
     *
     * @param id The id
     */
    public void setId(String id) {
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
    public void setVersion(long version) {
        this.version = version;
    }

    /**
     * The person who caused the latest report, as the workflow module named them.
     *
     * @return The person, or {@code null} if the system caused it
     */
    public Person getInitiator() {
        return initiator;
    }

    /**
     * Sets the person who caused the latest report.
     *
     * @param initiator The person
     */
    public void setInitiator(Person initiator) {
        this.initiator = initiator;
    }

    /**
     * When the reporting workflow system started this workflow. A workflow the cockpit knows from
     * its end alone starts when the end says it began, or at its end.
     *
     * @return The start, never {@code null} once stored
     */
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * When the cockpit stored the report about this workflow, by the cockpit's own clock.
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
    public void setReportedAt(OffsetDateTime reportedAt) {
        this.reportedAt = reportedAt;
    }

    /**
     * Sets when the workflow was started.
     *
     * @param createdAt The start
     */
    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Whether the cockpit learned about this workflow from its end alone and still waits for its
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
    public void setKnownFromItsEndAlone(boolean knownFromItsEndAlone) {
        this.knownFromItsEndAlone = knownFromItsEndAlone;
    }

    /**
     * When the event behind the latest stored report about this workflow happened, by the clock of
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
    public void setLatestEventAt(OffsetDateTime latestEventAt) {
        this.latestEventAt = latestEventAt;
    }

    /**
     * When the workflow was saved the last time. It is audit information and changes on every save.
     *
     * @return The time of the last save
     */
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Who saved the workflow the last time. It is audit information and changes on every save.
     *
     * @return The id of the user, or a name for the system
     */
    public String getUpdatedBy() {
        return updatedBy;
    }

    @Override
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    /**
     * When the workflow ended.
     *
     * @return The time of the end, or {@code null} while the workflow is open
     */
    public OffsetDateTime getEndedAt() {
        return endedAt;
    }

    /**
     * Sets when the workflow ended.
     *
     * @param endedAt The time of the end
     */
    public void setEndedAt(OffsetDateTime endedAt) {
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
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * The id of the workflow module the workflow belongs to.
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
    public void setWorkflowModuleId(String workflowModuleId) {
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
    public void setComment(String comment) {
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
    public void setBpmnProcessId(String bpmnProcessId) {
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
    public void setBpmnProcessVersion(String bpmnProcessVersion) {
        this.bpmnProcessVersion = bpmnProcessVersion;
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
    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    /**
     * The title of the workflow, by language.
     *
     * @return The titles, keyed by the language
     */
    public Map<String, String> getTitle() {
        return title;
    }

    /**
     * Sets the title of the workflow, by language.
     *
     * @param title The titles, keyed by the language
     */
    public void setTitle(Map<String, String> title) {
        this.title = title;
    }

    /**
     * Where the user interface of this workflow is found, as the workflow module reported it. The
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
    public void setUiUriPath(String uiUriPath) {
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
    public void setUiUriType(String uiUriType) {
        this.uiUriType = uiUriType;
    }

    /**
     * The business data the workflow module reported for this workflow.
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
    public void setDetails(Map<String, Object> details) {
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
    public void setDetailsFulltextSearch(String detailsFulltextSearch) {
        this.detailsFulltextSearch = detailsFulltextSearch;
    }

    /**
     * The groups whose members the workflow is accessible to.
     *
     * @return The groups, or {@code null}
     */
    public List<Group> getAccessibleToGroups() {
        return accessibleToGroups;
    }

    /**
     * Sets the groups whose members the workflow is accessible to.
     *
     * @param accessibleToGroups The groups
     */
    public void setAccessibleToGroups(List<Group> accessibleToGroups) {
        this.accessibleToGroups = accessibleToGroups;
    }

    /**
     * The users the workflow is accessible to.
     *
     * @return The users, or {@code null}
     */
    public List<Person> getAccessibleToUsers() {
        return accessibleToUsers;
    }

    /**
     * Sets the users the workflow is accessible to.
     *
     * @param accessibleToUsers The users
     */
    public void setAccessibleToUsers(List<Person> accessibleToUsers) {
        this.accessibleToUsers = accessibleToUsers;
    }

}
