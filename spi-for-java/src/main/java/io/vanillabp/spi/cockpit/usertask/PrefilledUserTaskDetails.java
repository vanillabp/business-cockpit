package io.vanillabp.spi.cockpit.usertask;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public interface PrefilledUserTaskDetails extends UserTaskDetails {

    String getEventId();
    
    OffsetDateTime getEventTimestamp();
    
    String getBpmnProcessId();
    
    String getBpmnProcessVersion();
    
    String getBpmnTaskId();

    void setInitiator(String initiator);
    
    void setComment(String comment);

    void setWorkflowTitle(Map<String, String> workflowTitle);

    void setTitle(Map<String, String> title);
    
    void setTaskDefinitionTitle(Map<String, String> taskDefinitionTitle);
    
    void setAssignee(String assignee);
    
    void setCandidateUsers(List<String> candidateUsers);

    void setDetails(Map<String, Object> details);
    
    void setDetailsFulltextSearch(String detailsFulltextSearch);

    void setI18nLanguages(List<String> i18nLanguages);
    
    void setDueDate(OffsetDateTime dueDate);

    void setFollowUpDate(OffsetDateTime followUpDate);
    
}
