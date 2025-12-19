package io.vanillabp.cockpit.adapter.common.usertask.events;

import io.vanillabp.spi.cockpit.details.DetailsEvent;
import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetails;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserTaskEventImpl implements UserTaskEvent, UserTaskDetails, PrefilledUserTaskDetails {
    private String eventId;
    private DetailsEvent.Event eventType;

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

    private UserTaskUiUriType uiUriType;

    private String assignee;

    private List<String> candidateUsers = new ArrayList<>();

    private List<String> candidateGroups = new ArrayList<>();

    private List<String> excludedCandidateUsers = new ArrayList<>();

    private OffsetDateTime dueDate;

    private OffsetDateTime followUpDate;

    private Map<String, Object> details = new HashMap<>();

    private String detailsFulltextSearch;

    private Object templateContext;

    private List<String> i18nLanguages;

    public UserTaskEventImpl(
            final String workflowModuleId,
            final List<String> i18nLanguages) {
        this.workflowModuleId = workflowModuleId;
        this.i18nLanguages = i18nLanguages;
    }

    @Override
    public String getId() {
        return userTaskId;
    }

    @Override
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    @Override
    public DetailsEvent.Event getEventType() {
        return eventType;
    }

    public void setEventType(DetailsEvent.Event eventType) {
        this.eventType = eventType;
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

    public String getWorkflowModuleId() {
        return workflowModuleId;
    }

    public void setWorkflowModuleId(String workflowModuleId) {
        this.workflowModuleId = workflowModuleId;
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
        return eventId;
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

    @Override
    public String getUiUriPath() {
        return uiUriPath;
    }

    public void setUiUriPath(String uiUriPath) {
        this.uiUriPath = uiUriPath;
    }

    public UserTaskUiUriType getUiUriType() {
        return uiUriType;
    }

    public void setUiUriType(UserTaskUiUriType uiUriType) {
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
    public List<String> getExcludedCandidateUsers() {
        return excludedCandidateUsers;
    }

    public void setExcludedCandidateUsers(List<String> excludedCandidateUsers) {
        this.excludedCandidateUsers = excludedCandidateUsers;
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

}

