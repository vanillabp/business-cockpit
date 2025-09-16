package io.vanillabp.cockpit.adapter.camunda8.receiver.events;

import io.vanillabp.spi.cockpit.details.DetailsEvent;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public class Camunda8UserTaskEvent {
    private long userTaskKey;
    private long jobKey;
    private OffsetDateTime timestamp;
    private DetailsEvent.Event event;
    private String bpmnProcessId;
    private String tenantId;
    private int processDefinitionVersion;
    private long processDefinitionKey;
    private long processInstanceKey;
    private String elementId;
    private String taskDefinition;
    private String assignee;
    private List<String> candidateUsers;
    private List<String> candidateGroups;
    private OffsetDateTime dueDate;
    private OffsetDateTime followUpDate;

    private Map<String, Object> variables;

    public long getJobKey() {
        return jobKey;
    }

    public void setJobKey(long jobKey) {
        this.jobKey = jobKey;
    }

    public long getUserTaskKey() {
        return userTaskKey;
    }

    public void setUserTaskKey(long userTaskKey) {
        this.userTaskKey = userTaskKey;
    }

    public DetailsEvent.Event getEvent() {
        return event;
    }

    public void setEvent(DetailsEvent.Event event) {
        this.event = event;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getBpmnProcessId() {
        return bpmnProcessId;
    }

    public void setBpmnProcessId(String bpmnProcessId) {
        this.bpmnProcessId = bpmnProcessId;
    }

    public int getProcessDefinitionVersion() {
        return processDefinitionVersion;
    }

    public void setProcessDefinitionVersion(int processDefinitionVersion) {
        this.processDefinitionVersion = processDefinitionVersion;
    }

    public long getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public void setProcessDefinitionKey(long processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
    }

    public long getProcessInstanceKey() {
        return processInstanceKey;
    }

    public void setProcessInstanceKey(long processInstanceKey) {
        this.processInstanceKey = processInstanceKey;
    }

    public String getElementId() {
        return elementId;
    }

    public void setElementId(String elementId) {
        this.elementId = elementId;
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

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
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

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

    public String getTaskDefinition() {
        return taskDefinition;
    }

    public void setTaskDefinition(String taskDefinition) {
        this.taskDefinition = taskDefinition;
    }
}
