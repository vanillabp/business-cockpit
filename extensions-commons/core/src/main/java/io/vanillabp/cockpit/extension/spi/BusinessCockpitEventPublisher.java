package io.vanillabp.cockpit.extension.spi;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * What a BPMS half calls when its engine reported something the Business Cockpit has to learn
 * about. A bean of this type is produced by the extension's platform module, so a BPMS half
 * injects it and needs no other entry point.
 * <p>
 * A call puts the whole report together and writes ONE outbox entry carrying it. Nothing is
 * sent to the cockpit while the caller waits. The entry is dispatched after the transaction it
 * was written in committed, so an event is reported if and only if what caused it was committed
 * too. A BPMS half may complete its job, acknowledge its listener or return from its
 * transaction as soon as this method returned.
 * <p>
 * What the report is built from is this half's own answer to
 * {@link BusinessCockpitBpmsBridge#prefilledUserTaskDetails} and the application's details
 * provider, both asked here and not later. A failure of either travels back to the caller, so
 * the engine's own work fails with it.
 * <p>
 * <b>Repetitions are free.</b> The entries carry an idempotency key, and a second report about
 * the same task and the same kind of event takes the place of the one which is still waiting.
 * Waiting updates of one task collapse into one entry on purpose, and the one which is left
 * carries the youngest report.
 */
public interface BusinessCockpitEventPublisher {

  /**
   * Reports an event of one user task.
   *
   * @param userTask The task the event is about
   * @param kind What happened
   * @param bpmsEventId The BPMS' own identifier of this event: a history event id, a job key,
   *          anything the BPMS repeats unchanged when it repeats the notification. It becomes
   *          the event id the cockpit sees. Where the BPMS has none, pass the task id
   * @param timestamp When the BPMS says it happened, or the current time where it does not say
   * @param transaction Which transaction the entry is written in.
   *          {@link EventTransaction#CURRENT} requires a transaction to be running on this
   *          thread and says so where none is
   * @return Whether an entry was written. <code>false</code> means that the application
   *         reports no user tasks at all, that this workflow module configured nothing about
   *         the cockpit, or that the BPMS half says nothing about a task which is still
   *         running, which is written into the log
   */
  boolean publishUserTaskEvent(
      UserTaskReference userTask,
      UserTaskEventKind kind,
      String bpmsEventId,
      OffsetDateTime timestamp,
      EventTransaction transaction);

  /**
   * Reports an event of one workflow. The parameters mean what they mean for
   * {@link #publishUserTaskEvent}.
   *
   * @param workflow The workflow the event is about
   * @param kind What happened
   * @param bpmsEventId The BPMS' own identifier of this event
   * @param timestamp When the BPMS says it happened
   * @param transaction Which transaction the entry is written in
   * @return Whether an entry was written. <code>false</code> means the same as it does for
   *         {@link #publishUserTaskEvent}, about workflows
   */
  boolean publishWorkflowEvent(
      WorkflowReference workflow,
      WorkflowEventKind kind,
      String bpmsEventId,
      OffsetDateTime timestamp,
      EventTransaction transaction);


  /**
   * Whether user tasks are reported at all, which an application switches off with
   * <code>vanillabp.cockpit.user-tasks-enabled</code>.
   * <p>
   * A BPMS half asks this to say why nothing was written. A report which was dropped because
   * the engine said nothing about the task looks exactly like a report nobody wanted, and a
   * developer reading the log deserves to be told which of the two it was.
   *
   * @return Whether a reported user task reaches the cockpit
   */
  default boolean reportsUserTasks() {

    return true;

  }

  /**
   * Whether workflows are reported at all, which an application switches off with
   * <code>vanillabp.cockpit.workflow-list-enabled</code>.
   *
   * @return Whether a reported workflow reaches the cockpit
   */
  default boolean reportsWorkflows() {

    return true;

  }

  /**
   * The process variables the application's details providers read with
   * <code>&#64;TaskParam</code> when they are asked about one user task.
   * <p>
   * A BPMS half asks this where its engine hands out only the variables it was asked for, such
   * as a Camunda 8 worker. It asks for these names, the engine puts their values on the event,
   * and the half passes them on as {@link UserTaskDetailsPrefill#variables()}. A half whose
   * engine hands out every variable of the task anyway does not need to ask.
   * <p>
   * VanillaBP answers the question, per workflow module and BPMN process, from the providers it
   * bound for that task. The answer may name a variable too many, but it never misses one a
   * provider of this task reads. A worker serving the task in several BPMN processes asks once
   * for each of them and fetches all names it got.
   * <p>
   * The names are known once VanillaBP scanned the application's workflow services, which is
   * before any workflow module starts processing. Ask while a module starts, not while its
   * models are wired.
   *
   * @param workflowModuleId The workflow module the task belongs to
   * @param bpmnProcessId The BPMN process the task belongs to
   * @param taskDefinition The task's task definition, may be <code>null</code>
   * @param bpmnTaskId The task's BPMN element id, may be <code>null</code>
   * @return The names, sorted. Empty where no provider of that task reads a variable, and
   *         empty where the application reports no user tasks
   */
  default List<String> variablesTheDetailsProvidersRead(
      final String workflowModuleId,
      final String bpmnProcessId,
      final String taskDefinition,
      final String bpmnTaskId) {

    return List.of();

  }

}
