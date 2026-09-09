package io.vanillabp.cockpit.extension.spi;

/**
 * What happened to a user task, as the Business Cockpit distinguishes it.
 * <p>
 * The four kinds are the ones the cockpit server accepts, and they are the values of
 * <code>io.vanillabp.spi.cockpit.details.DetailsEvent.Event</code> a
 * <code>&#64;UserTaskDetailsProvider</code> method receives. A BPMS half maps whatever its
 * engine reports onto one of them: a task listener of Camunda 7, a task-listener job of
 * Camunda 8, a state change reported by the Process Engine API.
 * <p>
 * {@link #COMPLETED} and {@link #CANCELED} carry no details - the cockpit only needs to know
 * that the task is gone - so no details provider runs for them.
 */
public enum UserTaskEventKind {

  /** The user task appeared and can be worked on. */
  CREATED,

  /** Something about the task changed: its assignee, its candidates, its business data. */
  UPDATED,

  /** Somebody finished the task. */
  COMPLETED,

  /**
   * The task was withdrawn without being finished - an interrupting boundary event, a
   * cancelled workflow, an operator deleting it.
   */
  CANCELED

}
