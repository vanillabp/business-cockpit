package io.vanillabp.spi.cockpit.usertask;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public interface UserTaskDetails {

    String getId();

    /**
     * Who caused this report. The value is mandatory: once a details provider has run, every
     * details object carries one, and the two allowed answers are a user id and the constant
     * {@link io.vanillabp.spi.cockpit.Initiator#SYSTEM} for an action no user caused.
     *
     * <p>Where the property <code>vanillabp.cockpit.initiator-source</code> says
     * <code>by-application</code> and nothing is set, the report fails and the message says
     * which of the two answers to give. Where it says <code>system</code>, the constant is
     * filled in.
     *
     * <p>The cockpit also reads it to leave somebody out of a notification about their own
     * work.
     *
     * @return The user who caused this report
     */
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
