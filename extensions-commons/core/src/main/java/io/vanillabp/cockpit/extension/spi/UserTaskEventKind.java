package io.vanillabp.cockpit.extension.spi;

/**
 * What happened to a user task, as the Business Cockpit distinguishes it.
 * <p>
 * The first four kinds are the values of
 * <code>io.vanillabp.spi.cockpit.details.DetailsEvent.Event</code> a
 * <code>&#64;UserTaskDetailsProvider</code> method receives. A BPMS half maps whatever its
 * engine reports onto one of them: a task listener of Camunda 7, a task-listener job of
 * Camunda 8, or a state change reported by the Process Engine API.
 * <p>
 * These four kinds carry the same fields, and a details provider runs for all four. An end says
 * what the task was finished with, which is what the list of completed tasks shows. Where the
 * BPMS cannot answer about an ended task any more, the end is reported with its identifiers
 * alone rather than dropped.
 * <p>
 * {@link #SUSPENDED} and {@link #ACTIVATED} say only that the process instance of the task was
 * suspended or activated again, and when. They carry no business data, so no details provider is
 * asked for them. A BPMS suspends a process instance and never a single task, so a BPMS half
 * reports them for every open task of the instance which was suspended. A called process instance
 * is not suspended together with its caller, so its tasks are not reported.
 */
public enum UserTaskEventKind {

  /** The user task appeared and can be worked on. */
  CREATED,

  /** Something about the task changed, such as its assignee, its candidates or its data. */
  UPDATED,

  /** Somebody finished the task, and the report says what it was finished with. */
  COMPLETED,

  /**
   * The task was withdrawn without being finished, by an interrupting boundary event, a
   * cancelled workflow or an operator deleting it.
   */
  CANCELED,

  /** The process instance of the task was suspended. The task cannot be worked on for now. */
  SUSPENDED,

  /** The process instance of the task was activated again after a suspension. */
  ACTIVATED

}
