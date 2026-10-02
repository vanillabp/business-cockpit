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
     * <p>
     * A task the BPMS names none of is left out, and the cockpit keeps the data it stored before.
     * It is not ended by this and it does not leave any list, because only the BPMS' own report
     * of an end does that. A task of a workflow started in this very transaction is such a task
     * on a BPMS which publishes its tasks with a delay, so a report right after a start is
     * usually one the BPMS' own listeners send anyway.
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
     * <p>
     * <b>What an empty answer means.</b> That the BPMS said nothing about a task of that id. It
     * does not say the task is over.
     * <p>
     * A BPMS which answers out of a storage of its own writes that storage behind its engine, so
     * a task created a moment ago is not in it yet. Camunda 8 works that way. The
     * Process-Engine-API answers out of what the asking node was served, which is a second way
     * to the same gap. On both of them a living task reads like a task nobody knows. Camunda 7
     * asks its own engine inside the caller's transaction, so there empty really is empty.
     * <p>
     * So read an empty answer as "no details right now" and keep what you already had. A caller
     * which turns it into "there is no such task", a REST endpoint answering 404 for instance,
     * says more than it was told, and the task it denies may be on somebody's screen in the
     * business cockpit while it does so. The end of a task is reported by the BPMS when it
     * happens, and that report is what tells the cockpit a task is done.
     *
     * @param workflowAggregate The workflow's aggregate
     * @param userTaskId The user-task's id
     * @return The user-task details, or empty where the BPMS said nothing about a task of that
     *         id belonging to this aggregate. Empty covers a task which ended and a task the
     *         BPMS has not published yet
     */
    Optional<UserTask> getUserTask(WA workflowAggregate, String userTaskId);
    
}
