package io.vanillabp.cockpit.workflowlist.model;

import io.vanillabp.cockpit.commons.mongo.updateinfo.UpdateInformationAware;
import io.vanillabp.cockpit.tasklist.model.UiUriType;
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


@Document(collection = Workflow.COLLECTION_NAME)
public class Workflow extends CandidatesAware implements UpdateInformationAware {

    public static final String COLLECTION_NAME = "workflow";

    @Id
    private String id;

    @Version
    private long version;

    private Person initiator;

    private OffsetDateTime createdAt;

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

    private UiUriType uiUriType;

    private List<Person> accessibleToUsers;

    private List<Group> accessibleToGroups;

    private Map<String, Object> details = null;

    private String detailsFulltextSearch;

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

    public Person getInitiator() {
        return initiator;
    }

    public void setInitiator(Person initiator) {
        this.initiator = initiator;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    @Override
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
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

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public Map<String, String> getTitle() {
        return title;
    }

    public void setTitle(Map<String, String> title) {
        this.title = title;
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

    public List<Group> getAccessibleToGroups() {
        return accessibleToGroups;
    }

    public void setAccessibleToGroups(List<Group> accessibleToGroups) {
        this.accessibleToGroups = accessibleToGroups;
    }

    public List<Person> getAccessibleToUsers() {
        return accessibleToUsers;
    }

    public void setAccessibleToUsers(List<Person> accessibleToUsers) {
        this.accessibleToUsers = accessibleToUsers;
    }

}
