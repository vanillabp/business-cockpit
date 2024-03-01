package io.vanillabp.cockpit.tasklist.model;

import io.vanillabp.cockpit.commons.mongo.updateinfo.UpdateInformationAware;
import io.vanillabp.cockpit.commons.security.jwt.JwtUserDetails;
import io.vanillabp.cockpit.util.candidates.CandidatesAware;
import org.springframework.data.annotation.AccessType;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Document(collection = UserTask.COLLECTION_NAME)
public class UserTask extends CandidatesAware implements UpdateInformationAware {

    public static record ReadBy(String userId, OffsetDateTime timestamp) {};

    public static final String COLLECTION_NAME = "usertask";
    
    @Id
    private String id;
    
    @Version
    private long version;

    private String initiator;

    private OffsetDateTime createdAt;

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

    private String workflowModuleUri;
    
    private String taskProviderApiUriPath;

    private String uiUriPath;

    private UiUriType uiUriType;

    private String assignee;

    private List<String> candidateUsers = null;

    private List<String> candidateGroups = null;

    private OffsetDateTime dueDate;

    private OffsetDateTime followUpDate;

    private Map<String, Object> details = null;

    private String detailsFulltextSearch;

    private List<ReadBy> readBy;

    @Override
    protected List<String> getGroups() {
        return getCandidateGroups();
    }

    @Override
    protected List<String> getUsers() {
        return getCandidateUsers();
    }

    public Collection<String> getTargetRoles() {

        final var result = super.getTargetRoles();
        if (result == null) {
            return null;
        }
        if (getAssignee() != null) {
            result.add(JwtUserDetails.USER_AUTHORITY_PREFIX + getAssignee());
        }
        return result;

    }

    public void addCandidateUser(
            final String userId) {

        if (userId == null) {
            return;
        }
        if (getUsers() == null) {
            setCandidateUsers(List.of(userId));
        } else {
            this.getUsers().removeIf(candidate -> candidate.equals(userId));
            this.getUsers().add(userId);
        }

    }

    public void removeCandidateUser(
            final String userId) {

        if (userId == null) {
            return;
        }
        if ((getUsers() == null)
                || getUsers().isEmpty()) {
            return;
        }

        this.getUsers().removeIf(candidate -> candidate.equals(userId));

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
            this.setReadBy(List.of(newReadBy));
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
        if (StringUtils.hasText(getAssignee())) {
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
    
    public String getTaskProviderApiUriPath() {
        return taskProviderApiUriPath;
    }

    public void setTaskProviderApiUriPath(String taskProviderApiUriPath) {
        this.taskProviderApiUriPath = taskProviderApiUriPath;
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

    public String getWorkflowModuleUri() {
        return workflowModuleUri;
    }
    
    public void setWorkflowModuleUri(String workflowModuleUri) {
        this.workflowModuleUri = workflowModuleUri;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public List<String> getCandidateUsers() {
        return candidateUsers;
    }

    public void setCandidateUsers(List<String> candidateUsers) {
        this.candidateUsers = candidateUsers;
    }

    public List<String> getCandidateGroups() {
        return candidateGroups;
    }

    public void setCandidateGroups(List<String> candidateGroups) {
        this.candidateGroups = candidateGroups;
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

}
