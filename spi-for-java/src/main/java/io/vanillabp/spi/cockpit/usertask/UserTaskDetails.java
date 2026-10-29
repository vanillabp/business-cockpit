package io.vanillabp.spi.cockpit.usertask;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * What a workflow module reports to the Business Cockpit about one user task.
 *
 * <p>A method annotated with {@link UserTaskDetailsProvider} returns it. Most such methods fill in
 * the {@link PrefilledUserTaskDetails} they were given and return that very object. A method may
 * also return an object of its own. The cockpit then takes only the values a provider may set,
 * and it keeps what the BPMS reported, like the id and the task definition.
 *
 * @see PrefilledUserTaskDetails
 */
public interface UserTaskDetails {

    /**
     * The id of the user task, the one the BPMS gave it. It names this one task, not the BPMN
     * element, and it is the id <code>BusinessCockpitService</code> takes for a user task.
     *
     * @return The id of the user task
     */
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
    
    /**
     * A short text the cockpit stores with the task. Lists of tasks can be sorted and filtered by
     * it.
     *
     * @return The comment, or <code>null</code>
     */
    String getComment();

    /**
     * The title of the workflow this task belongs to, in each language of {@link #getI18nLanguages()}.
     *
     * <p>The key of the map is a language, like <code>en</code>, and the value is the text in that
     * language. A value set by a details provider is first taken as the name of a template. Where a
     * template of that name exists, the cockpit renders it. Otherwise the value is shown as it is.
     * Where nothing is set, the cockpit renders its own template for this title, if the application
     * has templates, or it shows the name written in the BPMN file.
     *
     * @return The titles by language
     */
    Map<String, String> getWorkflowTitle();

    /**
     * The title of this task, in each language of {@link #getI18nLanguages()}. The task list shows it.
     *
     * <p>It is filled the same way as {@link #getWorkflowTitle()}. Its fallback is the name of the
     * user task in the BPMN file.
     *
     * @return The titles by language
     */
    Map<String, String> getTitle();

    /**
     * The task definition, as written in the BPMN file, for instance the form key. Many tasks share
     * one task definition. It is the value {@link UserTaskDetailsProvider#taskDefinition()} names.
     *
     * @return The task definition
     */
    String getTaskDefinition();

    /**
     * The title of the kind of task, in each language of {@link #getI18nLanguages()}. It is the same for every
     * task of one task definition.
     *
     * <p>It is filled the same way as {@link #getWorkflowTitle()}. Its fallback is the name of the
     * user task in the BPMN file.
     *
     * @return The titles by language
     */
    Map<String, String> getTaskDefinitionTitle();
    
    /**
     * The user the task is assigned to.
     *
     * <p>The cockpit reads this when it stores the task for the first time. A later report does
     * not change it, so set it to what is true when the task begins. To hand the work to other
     * people, end the task, for instance with a boundary event, and enter it again.
     * Taking over the task in the cockpit still changes the assignee.
     *
     * @return The user id of the assignee, or <code>null</code>
     */
    String getAssignee();

    /**
     * The users who may work on the task.
     *
     * <p>The cockpit reads this when it stores the task for the first time. A later report does
     * not change it, so set it to what is true when the task begins. To hand the work to other
     * people, end the task, for instance with a boundary event, and enter it again.
     * Assigning the task to somebody in the cockpit still adds a candidate user.
     *
     * @return The user ids of the candidates
     */
    List<String> getCandidateUsers();

    /**
     * The groups whose members may work on the task.
     *
     * <p>The cockpit reads this when it stores the task for the first time. A later report does
     * not change it, so set it to what is true when the task begins. To hand the work to other
     * people, end the task, for instance with a boundary event, and enter it again.
     *
     * @return The group ids of the candidates
     */
    List<String> getCandidateGroups();

    /**
     * Users who must not work on the task, although they are candidates, for instance to make sure
     * four eyes look at a case.
     *
     * <p>The cockpit reads this when it stores the task for the first time. A later report does
     * not change it, so set it to what is true when the task begins. To hand the work to other
     * people, end the task, for instance with a boundary event, and enter it again.
     *
     * @return The user ids left out
     */
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
     * <p>Unlike the assignee and the candidates, this list is read from every report. A later
     * report may admit other users.
     *
     * @return The user ids admitted to this task
     */
    List<String> getAdmittedUsers();

    /**
     * The date by which the task should be done. The task list shows it and can be sorted by it.
     * The prefilled details carry the due date the BPMS holds, where it holds one.
     *
     * @return The due date, or <code>null</code>
     */
    OffsetDateTime getDueDate();
    
    /**
     * The date before which nobody needs to work on the task. Until then the task list can leave
     * the task out of the open tasks, and it comes back once the date is reached. The prefilled
     * details carry the follow-up date the BPMS holds, where it holds one.
     *
     * @return The follow-up date, or <code>null</code>
     */
    OffsetDateTime getFollowUpDate();
    
    /**
     * The business data of the task. The workflow module decides what goes in. The cockpit stores
     * it with the task, and columns, sorting and filters of the task list can read its keys, like
     * <code>details.customer.name</code>.
     *
     * @return The business data, or <code>null</code>
     */
    Map<String, Object> getDetails();
    
    /**
     * The text the full-text search of the task list looks in. The workflow module puts it together
     * itself. Data which is not in this text cannot be found by that search. Where it is left
     * <code>null</code> and the application has templates, the cockpit renders it from a template.
     *
     * @return The text to search in, or <code>null</code>
     */
    String getDetailsFulltextSearch();
    
    /**
     * The languages the titles are rendered in, like <code>en</code> and <code>de</code>. Where
     * this is empty, the cockpit uses the languages configured for the workflow module.
     *
     * @return The languages
     */
    List<String> getI18nLanguages();
    
    /**
     * The context the templates for title, workflow title, task definition title and details
     * fulltext search are rendered with.
     * 
     * @return The template context
     */
    Object getTemplateContext();
    
    /**
     * Where the cockpit finds the user interface of this task.
     *
     * @return What the cockpit opens for this task. It is the path the workflow module configured
     *         unless a details provider set one of its own
     */
    String getUiUriPath();

    /**
     * How the cockpit notifies users about this task.
     *
     * @return How notifications for this user task are to be delivered. {@code null} is interpreted
     *         as {@link NotificationDelivery#USER_CONFIG}.
     */
    NotificationDelivery getNotificationDelivery();

}
