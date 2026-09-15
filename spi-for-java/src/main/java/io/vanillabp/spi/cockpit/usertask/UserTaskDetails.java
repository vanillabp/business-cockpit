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
     * <p>This list is the stronger word of the two: a user named here sees the task even when
     * {@link #getExcludedCandidateUsers()} names them as well. What such a reader gets to see is
     * then up to the workflow module, because the cockpit only opens the module's own form.
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
     * The context the templates for title, workflow title, task definition title and details
     * fulltext search are rendered with.
     * 
     * @return The template context
     */
    Object getTemplateContext();
    
    /**
     * @return What the cockpit opens for this task. It is the path the workflow module configured
     *         unless a details provider set one of its own
     */
    String getUiUriPath();

    /**
     * @return How notifications for this user task are to be delivered. {@code null} is interpreted
     *         as {@link NotificationDelivery#USER_CONFIG}.
     */
    NotificationDelivery getNotificationDelivery();

}
