package io.vanillabp.spi.cockpit.usertask;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * A user task as <code>BusinessCockpitService.getUserTask</code> returns it. It holds what the
 * BPMS knows about the task, after the details provider of the task added its part. Nothing of it
 * was reported to the cockpit.
 *
 * <p>The values mean the same as in {@link UserTaskDetails}.
 *
 * @see UserTaskDetails
 */
public interface UserTask {

    /**
     * The id of the user task, the one the BPMS gave it.
     *
     * @return The id of the user task
     * @see UserTaskDetails#getId()
     */
    String getId();

    /**
     * The id of the BPMN process the task belongs to.
     *
     * @return The BPMN process id
     */
    String getBpmnProcessId();

    /**
     * The version of the deployed BPMN process the task runs in, the way the BPMS reports it.
     *
     * @return The version, or <code>null</code> where the BPMS reports none
     */
    String getBpmnProcessVersion();

    /**
     * The id of the user task element in the BPMN file.
     *
     * @return The id of the BPMN element
     */
    String getBpmnTaskId();

    /**
     * The user the details provider named as the one who caused the latest change, or
     * {@link io.vanillabp.spi.cockpit.Initiator#SYSTEM}.
     *
     * @return The initiator
     * @see UserTaskDetails#getInitiator()
     */
    String getInitiator();

    /**
     * The short text stored with the task.
     *
     * @return The comment, or <code>null</code>
     * @see UserTaskDetails#getComment()
     */
    String getComment();

    /**
     * The title of the workflow this task belongs to, per language.
     *
     * @return The titles by language
     * @see UserTaskDetails#getWorkflowTitle()
     */
    Map<String, String> getWorkflowTitle();

    /**
     * The title of this task, per language.
     *
     * @return The titles by language
     * @see UserTaskDetails#getTitle()
     */
    Map<String, String> getTitle();

    /**
     * The task definition, as written in the BPMN file.
     *
     * @return The task definition
     * @see UserTaskDetails#getTaskDefinition()
     */
    String getTaskDefinition();

    /**
     * The title of the kind of task, per language.
     *
     * @return The titles by language
     * @see UserTaskDetails#getTaskDefinitionTitle()
     */
    Map<String, String> getTaskDefinitionTitle();

    /**
     * The user the task is assigned to.
     *
     * @return The user id of the assignee, or <code>null</code>
     * @see UserTaskDetails#getAssignee()
     */
    String getAssignee();

    /**
     * The users who may work on the task.
     *
     * @return The user ids of the candidates
     * @see UserTaskDetails#getCandidateUsers()
     */
    List<String> getCandidateUsers();

    /**
     * The groups whose members may work on the task.
     *
     * @return The group ids of the candidates
     * @see UserTaskDetails#getCandidateGroups()
     */
    List<String> getCandidateGroups();

    /**
     * The date by which the task should be done.
     *
     * @return The due date, or <code>null</code>
     * @see UserTaskDetails#getDueDate()
     */
    OffsetDateTime getDueDate();

    /**
     * The date before which nobody needs to work on the task.
     *
     * @return The follow-up date, or <code>null</code>
     * @see UserTaskDetails#getFollowUpDate()
     */
    OffsetDateTime getFollowUpDate();

    /**
     * The business data of the task.
     *
     * @return The business data, or <code>null</code>
     * @see UserTaskDetails#getDetails()
     */
    Map<String, Object> getDetails();

    /**
     * The languages the titles are rendered in.
     *
     * @return The languages
     * @see UserTaskDetails#getI18nLanguages()
     */
    List<String> getI18nLanguages();

}
