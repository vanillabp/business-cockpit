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
    
    void setAssignee(String assignee);
    
    void setCandidateUsers(List<String> candidateUsers);
    
    void setCandidateGroups(List<String> candidateGroups);

    void setExcludedCandidateUsers(List<String> candidateUsers);

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
