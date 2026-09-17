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
 * transports, and it knows no engine. This interface is everything it asks an engine for, and
 * it is kept small: five questions, each about one workflow or one user task named by
 * identifiers.
 * <p>
 * <b>When these methods run.</b> The two <code>prefilled…</code> methods are called at the
 * moment of the event, inside the transaction it arrived in, while the report is being put
 * together. A half answers them out of the event it is reporting: a task listener of an
 * embedded engine holds every field of a task already, and a half whose engine is remote keeps
 * what the engine delivered to it. Asking a storage which runs behind the engine does not work
 * there, because the event being reported has not reached it yet.
 * <p>
 * <code>prefilledUserTaskDetails</code> serves a second moment as well.
 * <code>BusinessCockpitService.getUserTask</code> reads a task somebody asks about now, and
 * that read is not about an event at all. A half which cannot answer both out of one source
 * reads the engine for this one and keeps the event's own values for the other.
 * <p>
 * The three <code>…OfAggregate</code> methods are called from
 * <code>io.vanillabp.spi.cockpit.BusinessCockpitService</code>. The two which serve a report
 * run inside the transaction the application was in. <code>userTaskOfAggregate</code> runs
 * inside that one, or inside a transaction the extension opened where the application brought
 * none. None of them may assume an engine transaction is open.
 * <p>
 * <b>What a failure means.</b> The two <code>prefilled…</code> methods have three answers, and
 * picking the right one decides whether a report is made, made without business data, or not
 * made at all.
 * <ol>
 * <li>The values, where the BPMS says something about the task or the workflow.</li>
 * <li>An exception, where the half cannot read what it was asked for. It travels on to the
 * caller, which is the engine reporting the event, so the engine's own work fails and says so.
 * A report is only read here, but what is read has to be right, and a defect which is repeated
 * away is a defect nobody fixes.</li>
 * <li>An empty result, where the BPMS says nothing about it. A report about a task or a case
 * which is still running is then dropped and the log says so; an end is reported with its
 * identifiers and without business data, because a report which never arrives leaves a task
 * the cockpit shows as open for good.</li>
 * </ol>
 * <code>io.vanillabp.integration.spi.PhaseTwoRetryLater</code> is no answer here any more.
 * It asked for an entry to be dispatched again, and at the moment of the event there is no
 * entry yet. A half throwing it reads a message saying so.
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
   * What the BPMS says about the given user task: the values of the event being reported, or
   * the values of now where <code>BusinessCockpitService.getUserTask</code> asks.
   *
   * @param userTask The task to read
   * @return The values read, or empty where the BPMS says nothing about the task, which ends
   *         the report of a running task for good
   */
  Optional<UserTaskDetailsPrefill> prefilledUserTaskDetails(
      UserTaskReference userTask);

  /**
   * What the BPMS says about the given workflow, at the moment of the event being reported.
   *
   * @param workflow The workflow to read
   * @return The values read, or empty where the BPMS says nothing about the workflow, which
   *         ends the report of a running case for good
   */
  Optional<WorkflowDetailsPrefill> prefilledWorkflowDetails(
      WorkflowReference workflow);

  /**
   * The workflows of one workflow aggregate this BPMS holds, which is what
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
   * The user tasks of one workflow aggregate this BPMS holds, which is what
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
   * One user task of one workflow aggregate, which is what
   * <code>BusinessCockpitService.getUserTask(aggregate, userTaskId)</code> reads. The task is
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
