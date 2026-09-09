package io.vanillabp.spi.cockpit;

import io.vanillabp.spi.cockpit.usertask.UserTask;

import java.util.Optional;

/**
 * A service to interact with the business cockpit.
 * <p>
 * Injecting it is optional: a workflow service which never reports a change of its own does
 * not need it, and asking for it is what makes it exist.
 * <p>
 * Three properties hold for everything this service does. Nothing is sent while the caller
 * waits: what is reported is written into VanillaBP's outbox inside the caller's transaction
 * and sent once that transaction committed, so nothing is reported for a change which was
 * rolled back. Repetitions are free: several reports of the same task or workflow which are
 * still waiting collapse into one, because what the cockpit is told is read again when the
 * report is sent. And the BPMS is elected per workflow rather than fixed: during a migration
 * one workflow of a workflow module may live in one BPMS and the next in another, and each is
 * asked where it is.
 *
 * @param <WA> The workflow-aggregate-class
 */
public interface BusinessCockpitService<WA> {

    /**
     * Indicate that the aggregate changed and an update
     * of workflow data stored in business cockpit is needed.
     * <p>
     * Processing is done asynchronous after the current
     * transaction and only if the transaction completes
     * successfully.
     * <p>
     * <i>Hint:</i> This does not update any user task,
     * only workflow data. To update data of user tasks
     * use {@link #aggregateChanged(Object, String...)}
     * instead.
     * 
     * @param workflowAggregate The workflow's aggregate
     */
    void aggregateChanged(WA workflowAggregate);

    /**
     * Indicate that the aggregate changed and an update
     * of user-task data stored in business cockpit is needed.
     * <p>
     * Processing is done asynchronous after the current
     * transaction and only if the transaction completes
     * successfully.
     * 
     * @param workflowAggregate The workflow's aggregate
     * @param userTaskIds The ids of the user tasks to update, or none for every user task
     *                    the BPMS currently holds for this aggregate
     */
    void aggregateChanged(WA workflowAggregate, String... userTaskIds);

    /**
     * Get details about the user-task as they would be
     * sent to the business-cockpit.
     * <p>
     * This is the one method which answers straight away: the BPMS is asked, the
     * details-provider method of that task runs, and what it produced is returned.
     * Nothing is reported to the cockpit, and the workflow aggregate is not saved
     * afterwards - a details provider invoked this way is expected to read rather than
     * to change.
     *
     * @param workflowAggregate The workflow's aggregate
     * @param userTaskId The user-task's id
     * @return The user-task details, or empty if the BPMS holding this workflow does not
     *         know a task of that id belonging to this aggregate
     */
    Optional<UserTask> getUserTask(WA workflowAggregate, String userTaskId);
    
}
