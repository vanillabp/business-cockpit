package io.vanillabp.cockpit.adapter.common.usertask.events;

import io.vanillabp.cockpit.bpms.api.v1.UiUriType;
import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetails;

import java.time.OffsetDateTime;
import java.util.*;

public class UserTaskCreatedEvent implements UserTaskEvent, UserTaskDetails, PrefilledUserTaskDetails {
    private String id;

    private String userTaskId;

    private String initiator;

    private OffsetDateTime timestamp;

    private String source;

    private String workflowModule;

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

    private String workflowModuleUri;

    private String taskProviderApiUriPath;

    private String uiUriPath;

    private UiUriType uiUriType;

    private String assignee;

    private List<String> candidateUsers = new ArrayList<>();

    private List<String> candidateGroups = new ArrayList<>();

    private OffsetDateTime dueDate;

    private OffsetDateTime followUpDate;

    private Map<String, Object> details = new HashMap<>();

    private String detailsFulltextSearch;

    private Object templateContext;

    private List<String> i18nLanguages;

    private String apiVersion;


    public UserTaskCreatedEvent(
            final String workflowModuleId,
            final List<String> i18nLanguages) {
        this.workflowModule = workflowModuleId;
        this.i18nLanguages = i18nLanguages;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getUserTaskId() {
        return userTaskId;
    }

    public void setUserTaskId(String userTaskId) {
        this.userTaskId = userTaskId;
    }

    @Override
    public String getInitiator() {
        return initiator;
    }

    public void setInitiator(String initiator) {
        this.initiator = initiator;
    }

    @Override
    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    @Override
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

    @Override
    public String getComment() {
        return comment;
    }

    @Override
    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public String getEventId() {
        return id;
    }

    @Override
    public OffsetDateTime getEventTimestamp() {
        return timestamp;
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

    @Override
    public Map<String, String> getWorkflowTitle() {
        return workflowTitle;
    }

    public void setWorkflowTitle(Map<String, String> workflowTitle) {
        this.workflowTitle = workflowTitle;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getSubWorkflowId() {
        return subWorkflowId;
    }

    public void setSubWorkflowId(String subWorkflowId) {
        this.subWorkflowId = subWorkflowId;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    @Override
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

    @Override
    public Map<String, String> getTaskDefinitionTitle() {
        return taskDefinitionTitle;
    }

    public void setTaskDefinitionTitle(Map<String, String> taskDefinitionTitle) {
        this.taskDefinitionTitle = taskDefinitionTitle;
    }

    public String getWorkflowModuleUri() {
        return workflowModuleUri;
    }

    public void setWorkflowModuleUri(String workflowModuleUri) {
        this.workflowModuleUri = workflowModuleUri;
    }

    public String getTaskProviderApiUriPath() {
        return taskProviderApiUriPath;
    }

    public void setTaskProviderApiUriPath(String taskProviderApiUriPath) {
        this.taskProviderApiUriPath = taskProviderApiUriPath;
    }

    @Override
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

    @Override
    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    @Override
    public List<String> getCandidateUsers() {
        return candidateUsers;
    }

    public void setCandidateUsers(List<String> candidateUsers) {
        this.candidateUsers = candidateUsers;
    }

    @Override
    public List<String> getCandidateGroups() {
        return candidateGroups;
    }

    public void setCandidateGroups(List<String> candidateGroups) {
        this.candidateGroups = candidateGroups;
    }

    @Override
    public OffsetDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(OffsetDateTime dueDate) {
        this.dueDate = dueDate;
    }

    @Override
    public OffsetDateTime getFollowUpDate() {
        return followUpDate;
    }

    public void setFollowUpDate(OffsetDateTime followUpDate) {
        this.followUpDate = followUpDate;
    }

    @Override
    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    @Override
    public String getDetailsFulltextSearch() {
        return detailsFulltextSearch;
    }

    public void setDetailsFulltextSearch(String detailsFulltextSearch) {
        this.detailsFulltextSearch = detailsFulltextSearch;
    }

    @Override
    public Object getTemplateContext() {
        return templateContext;
    }

    public void setTemplateContext(Object templateContext) {
        this.templateContext = templateContext;
    }

    @Override
    public List<String> getI18nLanguages() {
        return i18nLanguages;
    }

    public void setI18nLanguages(List<String> i18nLanguages) {
        this.i18nLanguages = i18nLanguages;
    }

    @Override
    public String getApiVersion() {
        return apiVersion;
    }

    @Override
    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }
}
