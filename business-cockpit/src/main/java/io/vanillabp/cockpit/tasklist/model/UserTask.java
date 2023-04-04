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

    private String bpmnWorkflowId;

    private String workflowId;

    private Map<String, String> title;

    private String workflowTaskId;

    private String taskDefinition;

    private Map<String, String> taskDefinitionTitle = null;

    private String taskProviderApiUrl;

    private String url;

    private UrlType urlType;

    private Boolean hasIcon;

    private Boolean hasFavicon;

    private String assignee;

    private List<String> candidateUsers = null;

    private List<String> candidateGroups = null;

    private OffsetDateTime dueDate;

    private OffsetDateTime followupDate;

    private Map<String, Object> details = null;

    private List<DetailsPropertyTitle> detailsPropertyTitles = null;

    private String detailsTextSearch;

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

    public String getBpmnWorkflowId() {
        return bpmnWorkflowId;
    }

    public void setBpmnWorkflowId(String bpmnWorkflowId) {
        this.bpmnWorkflowId = bpmnWorkflowId;
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

    public String getWorkflowTaskId() {
        return workflowTaskId;
    }

    public void setWorkflowTaskId(String workflowTaskId) {
        this.workflowTaskId = workflowTaskId;
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

    public String getTaskProviderApiUrl() {
        return taskProviderApiUrl;
    }

    public void setTaskProviderApiUrl(String taskProviderApiUrl) {
        this.taskProviderApiUrl = taskProviderApiUrl;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public UrlType getUrlType() {
        return urlType;
    }

    public void setUrlType(UrlType urlType) {
        this.urlType = urlType;
    }

    public Boolean getHasIcon() {
        return hasIcon;
    }

    public void setHasIcon(Boolean hasIcon) {
        this.hasIcon = hasIcon;
    }

    public Boolean getHasFavicon() {
        return hasFavicon;
    }

    public void setHasFavicon(Boolean hasFavicon) {
        this.hasFavicon = hasFavicon;
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

    public OffsetDateTime getFollowupDate() {
        return followupDate;
    }

    public void setFollowupDate(OffsetDateTime followupDate) {
        this.followupDate = followupDate;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public List<DetailsPropertyTitle> getDetailsPropertyTitles() {
        return detailsPropertyTitles;
    }

    public void setDetailsPropertyTitles(List<DetailsPropertyTitle> detailsPropertyTitles) {
        this.detailsPropertyTitles = detailsPropertyTitles;
    }

    public String getDetailsTextSearch() {
        return detailsTextSearch;
    }

    public void setDetailsTextSearch(String detailsTextSearch) {
        this.detailsTextSearch = detailsTextSearch;
    }

}
