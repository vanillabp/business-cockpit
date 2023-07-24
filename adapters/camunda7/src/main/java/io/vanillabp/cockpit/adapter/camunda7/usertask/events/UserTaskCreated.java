package io.vanillabp.cockpit.adapter.camunda7.usertask.events;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.vanillabp.cockpit.adapter.common.usertask.EventWrapper;
import io.vanillabp.cockpit.bpms.api.v1.DetailProperties;
import io.vanillabp.cockpit.bpms.api.v1.UserTaskCreatedOrUpdatedEvent;
import io.vanillabp.spi.cockpit.usertask.DetailCharacteristics;
import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;

public class UserTaskCreated
        implements PrefilledUserTaskDetails, EventWrapper {

    private final UserTaskCreatedOrUpdatedEvent event;
    
    private List<String> i18nLanguages;
    
    private Map<String, ? extends DetailCharacteristics> detailsCharacteristics;

    private Object templateContext;
    
    private String uiUriPath;
    
    public UserTaskCreated(
            final UserTaskCreatedOrUpdatedEvent event,
            final String workflowModuleId,
            final List<String> i18nLanguages) {
        
        this.event = event;
        this.i18nLanguages = i18nLanguages;
        event.setUpdated(Boolean.FALSE);
        event.setWorkflowModule(workflowModuleId);
        
    }
    
    @Override
    public void setId(String id) {
        event.setId(id);
    }
    
    @Override
    public void setUserTaskId(String userTaskId) {
        event.setUserTaskId(userTaskId);
    }
    
    @Override
    public String getEventId() {
        
        return event.getId();
        
    }
    
    @Override
    public OffsetDateTime getEventTimestamp() {
        
        return event.getTimestamp();
        
    }
    
    @Override
    public String getBpmnProcessVersion() {
        
        return event.getBpmnProcessVersion();
        
    }
    
    public void setBusinessId(String businessId) {
        event.setBusinessId(businessId);
    }

    public void setWorkflowId(String workflowId) {
        event.setWorkflowId(workflowId);
    }
    
    public void setSubWorkflowId(String subWorkflowId) {
        event.setSubWorkflowId(subWorkflowId);
    }

    public void setBpmnProcessId(String bpmnProcessId) {
        event.setBpmnProcessId(bpmnProcessId);
    }

    public void setBpmnProcessVersion(String bpmnProcessVersion) {
        event.setBpmnProcessVersion(bpmnProcessVersion);
    }

    public void setBpmnTaskId(String bpmnTaskId) {
        event.setBpmnTaskId(bpmnTaskId);
    }

    public String getBpmnProcessId() {
        return event.getBpmnProcessId();
    }

    public String getBpmnTaskId() {
        return event.getBpmnTaskId();
    }
    
    
    public Map<String, String> getWorkflowTitle() {
        return event.getWorkflowTitle();
    }
    
    @Override
    public String getId() {
        return event.getUserTaskId();
    }

    @Override
    public void setI18nLanguages(
            final List<String> i18nLanguages) {
        
        this.i18nLanguages = i18nLanguages;
        
    }

    @Override
    public List<String> getI18nLanguages() {
        
        return i18nLanguages;
        
    }
    
    @Override
    public Map<String, ? extends DetailCharacteristics> getDetailsCharacteristics() {
        
        return detailsCharacteristics;
        
    }
    
    public void setDetailsCharacteristics(Map<String, ? extends DetailCharacteristics> detailsCharacteristics) {
        
        this.detailsCharacteristics = detailsCharacteristics;
        
    }
    
    public void setTemplateContext(Object templateContext) {
        
        this.templateContext = templateContext;
        
    }
    
    @Override
    public Object getTemplateContext() {
        
        return templateContext;
        
    }
    
    public UserTaskCreatedOrUpdatedEvent getEvent() {
        
        if (detailsCharacteristics == null) {
            event.setDetailsProperties(null);
        } else {
            event.setDetailsProperties(
                    detailsCharacteristics
                            .entrySet()
                            .stream()
                            .map(entry -> new DetailProperties()
                                    .path(entry.getKey())
                                    .filterable(entry.getValue().isFilterable())
                                    .sortable(entry.getValue().isSortable()))
                            .collect(Collectors.toList()));
        }
        
        return event;
        
    }

    public String getWorkflowId() {
        return event.getWorkflowId();
    }
    
    public String getInitiator() {
        return event.getInitiator();
    }

    public void setInitiator(String initiator) {
        event.setInitiator(initiator);
    }

    public OffsetDateTime getTimestamp() {
        return event.getTimestamp();
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        event.setTimestamp(timestamp);
    }

    public String getComment() {
        return event.getComment();
    }

    public void setComment(String comment) {
        event.setComment(comment);
    }

    public void setWorkflowTitle(Map<String, String> workflowTitle) {
        event.setWorkflowTitle(workflowTitle);
    }

    public Map<String, String> getTitle() {
        return event.getTitle();
    }

    public void setTitle(Map<String, String> title) {
        event.setTitle(title);
    }

    public String getTaskDefinition() {
        return event.getTaskDefinition();
    }

    public Map<String, String> getTaskDefinitionTitle() {
        return event.getTaskDefinitionTitle();
    }

    public void setTaskDefinitionTitle(Map<String, String> taskDefinitionTitle) {
        event.setTaskDefinitionTitle(taskDefinitionTitle);
    }

    public void setTaskDefinition(String taskDefinition) {
        event.setTaskDefinition(taskDefinition);
    }

    public String getAssignee() {
        return event.getAssignee();
    }

    public void setAssignee(String assignee) {
        event.setAssignee(assignee);
    }

    public List<String> getCandidateUsers() {
        return event.getCandidateUsers();
    }

    public void setCandidateUsers(List<String> candidateUsers) {
        event.setCandidateUsers(candidateUsers);
    }

    public List<String> getCandidateGroups() {
        return event.getCandidateGroups();
    }

    public void setCandidateGroups(List<String> candidateGroups) {
        event.setCandidateGroups(candidateGroups);
    }

    public OffsetDateTime getDueDate() {
        return event.getDueDate();
    }

    public void setDueDate(OffsetDateTime dueDate) {
        event.setDueDate(dueDate);
    }

    public OffsetDateTime getFollowUpDate() {
        return event.getFollowUpDate();
    }

    public void setFollowUpDate(OffsetDateTime followUpDate) {
        event.setFollowUpDate(followUpDate);
    }

    public Map<String, Object> getDetails() {
        return event.getDetails();
    }

    public void setDetails(Map<String, Object> details) {
        event.setDetails(details);
    }

    public String getDetailsFulltextSearch() {
        return event.getDetailsFulltextSearch();
    }

    public void setDetailsFulltextSearch(String detailsFulltextSearch) {
        event.setDetailsFulltextSearch(detailsFulltextSearch);
    }
    
    @Override
    public String getUiUriPath() {
        return uiUriPath;
    }
    
    public void setUiUriPath(String uiUriPath) {
        this.uiUriPath = uiUriPath;
    }
    
}
