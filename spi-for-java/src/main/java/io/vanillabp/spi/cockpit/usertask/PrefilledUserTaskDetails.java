package io.vanillabp.spi.cockpit.usertask;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * The details of a user task as VanillaBP prefilled them from what the BPMS reported. A method
 * annotated with {@link UserTaskDetailsProvider} takes it as a parameter.
 *
 * <p>The getters say what the BPMS reported. The setters change what is reported to the cockpit.
 * Most providers set what they want to add or change and return this very object.
 *
 * @see UserTaskDetailsProvider
 */
public interface PrefilledUserTaskDetails extends UserTaskDetails {

    /**
     * The id of this report, which the cockpit receives as the id of the event. It is not the id of
     * the task, see {@link #getId()} for that.
     *
     * @return The id of this report
     */
    String getEventId();
    
    /**
     * When the reported change happened. Where the BPMS gives no time, it is the time the report
     * was built.
     *
     * @return When the change happened
     */
    OffsetDateTime getEventTimestamp();
    
    /**
     * The id of the BPMN process the task belongs to, as written in the BPMN file.
     *
     * @return The BPMN process id
     */
    String getBpmnProcessId();
    
    /**
     * The version of the deployed BPMN process the task runs in, the way the BPMS reports it. It is
     * the value {@link UserTaskDetailsProvider#version()} is matched against.
     *
     * @return The version, or <code>null</code> where the BPMS reports none
     */
    String getBpmnProcessVersion();
    
    /**
     * The id of the user task element in the BPMN file. All tasks created from that element share
     * it. It is the value {@link UserTaskDetailsProvider#id()} names, see {@link #getId()} for the
     * id of this one task.
     *
     * @return The id of the BPMN element
     */
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

    /**
     * Sets the short text the cockpit stores with the task.
     *
     * @param comment The comment
     * @see UserTaskDetails#getComment()
     */
    void setComment(String comment);

    /**
     * Sets the title of the workflow, or the name of a template, per language.
     *
     * @param workflowTitle The texts or template names, keyed by language
     * @see UserTaskDetails#getWorkflowTitle()
     */
    void setWorkflowTitle(Map<String, String> workflowTitle);

    /**
     * Sets the title of this task, or the name of a template, per language.
     *
     * @param title The texts or template names, keyed by language
     * @see UserTaskDetails#getTitle()
     */
    void setTitle(Map<String, String> title);
    
    /**
     * Sets the title of the kind of task, or the name of a template, per language.
     *
     * @param taskDefinitionTitle The texts or template names, keyed by language
     * @see UserTaskDetails#getTaskDefinitionTitle()
     */
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

    /**
     * Sets the business data the cockpit stores with the task.
     *
     * @param details The business data
     * @see UserTaskDetails#getDetails()
     */
    void setDetails(Map<String, Object> details);
    
    /**
     * Sets the text the full-text search of the task list looks in.
     *
     * @param detailsFulltextSearch The text to search in
     * @see UserTaskDetails#getDetailsFulltextSearch()
     */
    void setDetailsFulltextSearch(String detailsFulltextSearch);

    /**
     * Sets the languages the titles are rendered in. Leave it empty to use the languages
     * configured for the workflow module.
     *
     * @param i18nLanguages The languages, like <code>en</code>
     * @see UserTaskDetails#getI18nLanguages()
     */
    void setI18nLanguages(List<String> i18nLanguages);
    
    /**
     * Sets the date by which the task should be done.
     *
     * @param dueDate The due date
     * @see UserTaskDetails#getDueDate()
     */
    void setDueDate(OffsetDateTime dueDate);

    /**
     * Sets the date before which nobody needs to work on the task.
     *
     * @param followUpDate The follow-up date
     * @see UserTaskDetails#getFollowUpDate()
     */
    void setFollowUpDate(OffsetDateTime followUpDate);

    /**
     * Sets the object the templates read their values from, for instance the workflow
     * aggregate. Nothing is prefilled here, so without this call the templates see no data
     * of the case.
     *
     * @param templateContext The template context
     * @see UserTaskDetails#getTemplateContext()
     */
    void setTemplateContext(Object templateContext);

    /**
     * Sets how the cockpit notifies users about this task.
     *
     * @param notificationDelivery How to notify, or <code>null</code> for
     *        {@link NotificationDelivery#USER_CONFIG}
     * @see UserTaskDetails#getNotificationDelivery()
     */
    void setNotificationDelivery(NotificationDelivery notificationDelivery);

    /**
     * Tells the cockpit where the user interface of this task is found.
     *
     * <p>Without this the cockpit uses the path the workflow module configured, which is the same
     * for every task of the module. A path which starts with <code>http://</code> or
     * <code>https://</code> is opened as it is. This is how a module hands out the address of the
     * page in another application which shows this very task. Any other path belongs to the
     * workflow module, and the cockpit user interface puts <code>/wm/&lt;workflowModuleId&gt;/</code>
     * in front of it.
     *
     * @param uiUriPath What the cockpit opens for this task
     */
    void setUiUriPath(String uiUriPath);

}
