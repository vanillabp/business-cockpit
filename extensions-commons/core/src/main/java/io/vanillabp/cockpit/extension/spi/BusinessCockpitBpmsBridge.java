package io.vanillabp.cockpit.extension.spi;

import java.util.List;
import java.util.Optional;

/**
 * The BPMS half of the Business Cockpit extension: one implementation per configured adapter
 * id, answering the questions the platform-neutral half cannot answer itself.
 * <p>
 * A BPMS half registers one bean per bridge, or one bean holding a
 * <code>List&lt;BusinessCockpitBpmsBridge&gt;</code> where it builds a bridge per configured
 * adapter id and cannot say at build time how many that is. Both shapes are collected, on
 * Spring Boot and on Quarkus.
 * <p>
 * The neutral half owns the event model, the details providers, the templating and the
 * transports; it knows no engine. This interface is everything it asks an engine for, and it
 * is deliberately small: five questions, all of them about one workflow or one user task named
 * by identifiers.
 * <p>
 * <b>When these methods run.</b> The two <code>prefilled…</code> methods are called while an
 * outbox entry is dispatched, which is after the transaction the BPMS event arrived in was
 * committed and on a thread of the outbox dispatcher. The three <code>…OfAggregate</code>
 * methods are called from
 * <code>io.vanillabp.spi.cockpit.BusinessCockpitService</code>, inside whatever transaction
 * the application was in. None of them may assume an engine transaction is open.
 * <p>
 * <b>What a failure means.</b> The two <code>prefilled…</code> methods have four answers, and
 * picking the right one decides whether a report is made, made later, or not made at all.
 * <ol>
 * <li>The values, where the BPMS knows the task or the workflow.</li>
 * <li>An exception, where the BPMS could not be reached. The dispatch of the entry is aborted
 * and the outbox repeats it with a backoff.</li>
 * <li><code>io.vanillabp.integration.spi.PhaseTwoRetryLater</code>, where the BPMS is reachable
 * and does not know it <em>yet</em> - a read model which has not caught up with the event it
 * just sent. The entry comes back after the window the exception names, and the number of
 * attempts an outbox store allows bounds the waiting. A remote engine which answers a report of
 * a freshly created task with 404 belongs here and not below.</li>
 * <li>An empty result, where the BPMS is reachable and does not know it any more. Nothing is
 * reported, which is right for a task somebody completed a second ago and wrong for one the
 * BPMS has merely not made searchable yet: a report dropped here is dropped for good and is
 * said out loud in the log.</li>
 * </ol>
 *
 * @see BusinessCockpitEventPublisher for the other direction, which the BPMS half calls
 */
public interface BusinessCockpitBpmsBridge {

  /**
   * @return The configured adapter id this bridge serves. Two adapter ids of the same BPMS
   *         type mean two bridges, because during a migration each of them holds workflows of
   *         its own
   */
  String adapterId();

  /**
   * @return The adapter type this bridge belongs to (<code>camunda7</code>,
   *         <code>camunda8</code>, <code>process-engine-api</code>), used only to name the
   *         BPMS in messages
   */
  String adapterType();

  /**
   * What the BPMS knows about the given user task right now.
   *
   * @param userTask The task to read
   * @return The values read, or empty where the BPMS does not know the task any more - which
   *         ends the report for good. Where the BPMS may know it in a moment, throw
   *         <code>PhaseTwoRetryLater</code> instead
   */
  Optional<UserTaskDetailsPrefill> prefilledUserTaskDetails(
      UserTaskReference userTask);

  /**
   * What the BPMS knows about the given workflow right now.
   *
   * @param workflow The workflow to read
   * @return The values read, or empty where the BPMS does not know the workflow any more -
   *         which ends the report for good. Where the BPMS may know it in a moment, throw
   *         <code>PhaseTwoRetryLater</code> instead
   */
  Optional<WorkflowDetailsPrefill> prefilledWorkflowDetails(
      WorkflowReference workflow);

  /**
   * The workflows of one workflow aggregate this BPMS holds - what
   * <code>BusinessCockpitService.aggregateChanged(aggregate)</code> reports a change of. A
   * workflow which already ended is included where the BPMS can still tell, so that the
   * cockpit's view of a finished case is complete.
   *
   * @param workflowModuleId The workflow module
   * @param bpmnProcessId The primary BPMN process of the aggregate
   * @param workflowAggregateId The aggregate's id, serialized
   * @return The workflows, empty where the BPMS holds none
   */
  List<WorkflowReference> workflowsOfAggregate(
      String workflowModuleId,
      String bpmnProcessId,
      String workflowAggregateId);

  /**
   * The user tasks of one workflow aggregate this BPMS holds - what
   * <code>BusinessCockpitService.aggregateChanged(aggregate, userTaskIds)</code> reports a
   * change of.
   *
   * @param workflowModuleId The workflow module
   * @param bpmnProcessId The primary BPMN process of the aggregate
   * @param workflowAggregateId The aggregate's id, serialized
   * @param userTaskIds The ids the caller named, or empty for every active task of the
   *          aggregate
   * @return The tasks, empty where none of them is active any more
   */
  List<UserTaskReference> userTasksOfAggregate(
      String workflowModuleId,
      String bpmnProcessId,
      String workflowAggregateId,
      List<String> userTaskIds);

  /**
   * One user task of one workflow aggregate - what
   * <code>BusinessCockpitService.getUserTask(aggregate, userTaskId)</code> reads. The id is
   * answered only where it really belongs to this aggregate, so that an application cannot
   * read another case's task by guessing an id.
   *
   * @param workflowModuleId The workflow module
   * @param bpmnProcessId The primary BPMN process of the aggregate
   * @param workflowAggregateId The aggregate's id, serialized
   * @param userTaskId The task's id
   * @return The task, or empty where the BPMS does not know it or it belongs elsewhere
   */
  Optional<UserTaskReference> userTaskOfAggregate(
      String workflowModuleId,
      String bpmnProcessId,
      String workflowAggregateId,
      String userTaskId);

}
