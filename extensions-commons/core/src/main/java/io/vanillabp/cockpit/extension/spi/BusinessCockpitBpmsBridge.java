package io.vanillabp.cockpit.extension.spi;

import java.util.List;
import java.util.Optional;

/**
 * The BPMS half of the Business Cockpit extension: one implementation per configured adapter
 * id, answering the questions the platform-neutral half cannot answer itself.
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
 * <b>What a failure means.</b> An exception thrown here aborts the dispatch of the outbox
 * entry, which the outbox then retries with a backoff. Throw where the BPMS could not be
 * reached; answer with an empty result where the BPMS is reachable and simply does not know
 * the task or workflow any more, because a task somebody completed a second ago is a normal
 * answer and not a failure to repeat.
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
   * @return The values read, or empty where the BPMS no longer knows the task. An empty answer
   *         ends the dispatch quietly: the cockpit was told about the task's end already, or
   *         is about to be
   */
  Optional<UserTaskDetailsPrefill> prefilledUserTaskDetails(
      UserTaskReference userTask);

  /**
   * What the BPMS knows about the given workflow right now.
   *
   * @param workflow The workflow to read
   * @return The values read, or empty where the BPMS no longer knows the workflow
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
