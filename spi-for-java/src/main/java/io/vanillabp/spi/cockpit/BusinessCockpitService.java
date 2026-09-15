package io.vanillabp.spi.cockpit;

import io.vanillabp.spi.cockpit.usertask.UserTask;

import java.util.Optional;

/**
 * A service to interact with the business cockpit.
 * <p>
 * Injecting it is optional: a workflow service which never reports a change of its own does
 * not need it, and asking for it is what makes it exist.
 * <p>
 * Three things hold for everything this service does.
 * <p>
 * Nothing is sent while the caller waits. What is reported is written into VanillaBP's outbox
 * inside the caller's transaction and sent once that transaction committed. So nothing is
 * reported for a change which was rolled back.
 * <p>
 * Repetitions are free. Several reports of the same task or workflow which are still waiting
 * collapse into one, because what the cockpit is told is read again when the report is sent.
 * <p>
 * The BPMS is elected per workflow and not fixed. During a migration one workflow of a workflow
 * module may live in one BPMS and the next in another, and each is asked where it is.
 * <p>
 * The methods come in two shapes, and each one says below which transaction it works in. A
 * report needs the transaction which persists the change it is about, and it is refused without
 * one. A question takes part in the transaction which is running, and it gets one of VanillaBP's
 * own where nothing runs. So the answer sees what the caller changed and has not written yet,
 * and a caller outside a transaction is answered all the same.
 *
 * @param <WA> The workflow-aggregate-class
 */
public interface BusinessCockpitService<WA> {

    /**
     * Report that the aggregate changed, so that the workflow data the business cockpit holds
     * is updated.
     * <p>
     * The report is sent after the current transaction, and only if that transaction commits.
     * <p>
     * <i>Hint:</i> This updates the workflow data only, no user task. Use
     * {@link #aggregateChanged(Object, String...)} for the data of user tasks.
     * <p>
     * <b>Which transaction this works in.</b> The one running on the calling thread. The entry is
     * written into that transaction, which is what ties the report to the change. A thread which
     * carries no transaction is refused, with a message asking to open one.
     *
     * @param workflowAggregate The workflow's aggregate
     */
    void aggregateChanged(WA workflowAggregate);

    /**
     * Report that the aggregate changed, so that the user task data the business cockpit holds
     * is updated.
     * <p>
     * The report is sent after the current transaction, and only if that transaction commits.
     * <p>
     * <b>Which transaction this works in.</b> The one running on the calling thread, like the
     * report of the workflow data, and a thread carrying no transaction is refused the same way.
     *
     * @param workflowAggregate The workflow's aggregate
     * @param userTaskIds The ids of the user tasks to update, or none for every user task
     *                    the BPMS currently holds for this aggregate
     */
    void aggregateChanged(WA workflowAggregate, String... userTaskIds);

    /**
     * Get the details of a user task, as they would be sent to the business cockpit.
     * <p>
     * This is the one method which answers straight away. The BPMS is asked, the details
     * provider method of that task runs, and what it produced is returned. Nothing is reported
     * to the cockpit, and the workflow aggregate is not saved afterwards. A details provider
     * called this way is meant to read, not to change.
     * <p>
     * <b>Which transaction this works in.</b> The one running on the calling thread, and one of
     * VanillaBP's own where nothing runs. So a workflow service which changed its aggregate and
     * has not written it yet reads an answer built from that state. A caller outside a
     * transaction, say a REST controller showing a task, gets an answer as well.
     * <p>
     * Taking part in the caller's transaction has a second side. A details provider which
     * changes the aggregate leaves that change where the caller will commit it, because a
     * persistence which writes the changes of a managed object by itself writes it there. A
     * provider reached by this method should read only.
     *
     * @param workflowAggregate The workflow's aggregate
     * @param userTaskId The user-task's id
     * @return The user-task details, or empty if the BPMS holding this workflow does not
     *         know a task of that id belonging to this aggregate
     */
    Optional<UserTask> getUserTask(WA workflowAggregate, String userTaskId);
    
}
