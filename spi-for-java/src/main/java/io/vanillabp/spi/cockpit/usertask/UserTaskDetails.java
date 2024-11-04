package io.vanillabp.spi.cockpit.usertask;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public interface UserTaskDetails {

    String getId();
    
    String getInitiator();
    
    String getComment();

    Map<String, String> getWorkflowTitle();

    Map<String, String> getTitle();

    String getTaskDefinition();

    Map<String, String> getTaskDefinitionTitle();
    
    String getAssignee();
    
    List<String> getCandidateUsers();
    
    List<String> getCandidateGroups();

    List<String> getExcludedCandidateUsers();
    
    OffsetDateTime getDueDate();
    
    OffsetDateTime getFollowUpDate();
    
    Map<String, Object> getDetails();
    
    String getDetailsFulltextSearch();
    
    List<String> getI18nLanguages();
    
    /**
     * Used for rendering title, workflow-title, task-definition-title and details-fulltext-search
     * based on templates.
     * 
     * @return The template context
     */
    Object getTemplateContext();
    
    /**
     * @return A URI path if user-task is EXTERNAL
     */
    String getUiUriPath();
    
}
