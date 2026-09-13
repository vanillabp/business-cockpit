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
 * All four kinds carry the same fields, and a details provider runs for all four. An end says
 * what the task was finished with, which is what the list of completed tasks shows. Where the
 * BPMS cannot answer about an ended task any more, the end is reported with its identifiers
 * alone rather than dropped.
 */
public enum UserTaskEventKind {

  /** The user task appeared and can be worked on. */
  CREATED,

  /** Something about the task changed: its assignee, its candidates, its business data. */
  UPDATED,

  /** Somebody finished the task, and the report says what it was finished with. */
  COMPLETED,

  /**
   * The task was withdrawn without being finished - an interrupting boundary event, a
   * cancelled workflow, an operator deleting it.
   */
  CANCELED

}
