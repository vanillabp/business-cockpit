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

    /**
     * Names the user who caused this report.
     *
     * <p>This is mandatory, and your workflow module is the only one who can answer it. No
     * workflow system records who assigned a task or took it, and by the time this object is
     * prefilled the security context of the request is gone. So keep the user in the workflow
     * aggregate and read it back here.
     *
     * <p>Two answers are allowed: a user id, or
     * {@link io.vanillabp.spi.cockpit.Initiator#SYSTEM} where no user caused the action. Set the
     * constant as well to drop a value an adapter prefilled, because setting <code>null</code>
     * looks like a provider which did nothing.
     *
     * @param initiator The user who caused this report
     */
    void setInitiator(String initiator);

    void setComment(String comment);

    void setWorkflowTitle(Map<String, String> workflowTitle);

    void setTitle(Map<String, String> title);
    
    void setTaskDefinitionTitle(Map<String, String> taskDefinitionTitle);
    
    /**
     * Sets the user the task is assigned to.
     *
     * <p>The cockpit uses this value when it stores the task for the first time. A value set for a
     * later report is not used. The provider is still called for every report, so it may simply
     * set the same value each time.
     *
     * @param assignee The user id of the assignee
     * @see UserTaskDetails#getAssignee()
     */
    void setAssignee(String assignee);

    /**
     * Sets the users who may work on the task.
     *
     * <p>The cockpit uses this value when it stores the task for the first time. A value set for a
     * later report is not used. The provider is still called for every report, so it may simply
     * set the same value each time.
     *
     * @param candidateUsers The user ids of the candidates
     * @see UserTaskDetails#getCandidateUsers()
     */
    void setCandidateUsers(List<String> candidateUsers);

    /**
     * Sets the groups whose members may work on the task.
     *
     * <p>The cockpit uses this value when it stores the task for the first time. A value set for a
     * later report is not used. The provider is still called for every report, so it may simply
     * set the same value each time.
     *
     * @param candidateGroups The group ids of the candidates
     * @see UserTaskDetails#getCandidateGroups()
     */
    void setCandidateGroups(List<String> candidateGroups);

    /**
     * Sets the users who must not work on the task, although they are candidates.
     *
     * <p>The cockpit uses this value when it stores the task for the first time. A value set for a
     * later report is not used. The provider is still called for every report, so it may simply
     * set the same value each time.
     *
     * @param candidateUsers The user ids left out
     * @see UserTaskDetails#getExcludedCandidateUsers()
     */
    void setExcludedCandidateUsers(List<String> candidateUsers);

    /**
     * Sets the users who may see the task although they are no candidate for it. Unlike the
     * assignee and the candidates, this value is used for every report.
     *
     * @param admittedUsers The user ids admitted to this task
     * @see UserTaskDetails#getAdmittedUsers()
     */
    void setAdmittedUsers(List<String> admittedUsers);

    void setDetails(Map<String, Object> details);
    
    void setDetailsFulltextSearch(String detailsFulltextSearch);

    void setI18nLanguages(List<String> i18nLanguages);
    
    void setDueDate(OffsetDateTime dueDate);

    void setFollowUpDate(OffsetDateTime followUpDate);

    void setTemplateContext(Object templateContext);

    void setNotificationDelivery(NotificationDelivery notificationDelivery);

    /**
     * Tells the cockpit where the user interface of this task is found.
     *
     * <p>Without this the cockpit uses the path the workflow module configured, which is the same
     * for every task of the module. A module whose UI URI type is <code>EXTERNAL</code> uses it to
     * hand out the address of the page in the other application which shows this very task.
     *
     * @param uiUriPath What the cockpit opens for this task
     */
    void setUiUriPath(String uiUriPath);

}
