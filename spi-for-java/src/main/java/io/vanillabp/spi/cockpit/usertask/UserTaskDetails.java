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

    /**
     * Users who may see this task although they are no candidate for it. A workflow module fills
     * this where somebody has to reach the task for a reason of the business rather than because
     * work is waiting for them, for instance everybody who already worked on it and wants to read
     * back later what they entered.
     *
     * @return The user ids admitted to this task
     */
    List<String> getAdmittedUsers();

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

    /**
     * @return How notifications for this user task are to be delivered. {@code null} is interpreted
     *         as {@link NotificationDelivery#USER_CONFIG}.
     */
    NotificationDelivery getNotificationDelivery();

}
