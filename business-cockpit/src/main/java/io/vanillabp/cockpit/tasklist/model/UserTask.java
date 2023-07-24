package io.vanillabp.cockpit.tasklist.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import io.vanillabp.cockpit.commons.mongo.updateinfo.UpdateInformationAware;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = UserTask.COLLECTION_NAME)
public class UserTask implements UpdateInformationAware {

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

    private String workflowModule;

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

    public String getWorkflowModule() {
        return workflowModule;
    }

    public void setWorkflowModule(String workflowModule) {
        this.workflowModule = workflowModule;
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

}
