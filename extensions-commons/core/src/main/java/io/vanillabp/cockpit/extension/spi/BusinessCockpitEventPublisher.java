package io.vanillabp.cockpit.extension.spi;

import java.time.OffsetDateTime;

/**
 * What a BPMS half calls when its engine reported something the Business Cockpit has to learn
 * about. A bean of this type is produced by the extension's platform module, so a BPMS half
 * injects it and needs no other entry point.
 * <p>
 * A call writes ONE outbox entry and returns; nothing is sent to the cockpit while the caller
 * waits. The entry is dispatched after the transaction it was written in committed, so an
 * event is reported if and only if what caused it was committed too. A BPMS half may therefore
 * complete its job, acknowledge its listener or return from its transaction as soon as this
 * method returned.
 * <p>
 * <b>Repetitions are free.</b> The entries carry an idempotency key, and a repeated
 * notification about the same task and the same kind of event is discarded while the first one
 * is still waiting. Pending updates of one task collapse into one entry on purpose: the
 * dispatch reads the current state anyway, so ten updates and one update tell the cockpit the
 * same thing.
 */
public interface BusinessCockpitEventPublisher {

  /**
   * Reports an event of one user task.
   *
   * @param userTask The task the event is about
   * @param kind What happened
   * @param bpmsEventId The BPMS' own identifier of this event - a history event id, a job key,
   *          anything the BPMS repeats unchanged when it repeats the notification. It becomes
   *          the event id the cockpit sees. Where the BPMS has none, pass the task id
   * @param timestamp When the BPMS says it happened; the current time where it does not say
   * @param transaction Which transaction the entry is written in.
   *          {@link EventTransaction#CURRENT} requires a transaction to be running on this
   *          thread and says so where none is
   * @return Whether an entry was written. <code>false</code> means an entry of the same
   *         idempotency key is still waiting for its dispatch and this one is a repetition of
   *         it, or that the application reports no user tasks at all
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
   * @return Whether an entry was written. <code>false</code> also means that the application
   *         reports no workflows at all
   */
  boolean publishWorkflowEvent(
      WorkflowReference workflow,
      WorkflowEventKind kind,
      String bpmsEventId,
      OffsetDateTime timestamp,
      EventTransaction transaction);

}
