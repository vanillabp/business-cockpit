package io.vanillabp.cockpit.extension.spi;

/**
 * What happened to a workflow, as the Business Cockpit distinguishes it.
 * <p>
 * The last kind is spelled {@link #CANCELLED} with two Ls, while its user task counterpart is
 * spelled {@link UserTaskEventKind#CANCELED} with one. Both spellings are on the wire already.
 * The cockpit's REST endpoint is <code>/workflow/{id}/cancelled</code>, and the SPI enum a
 * details provider receives says <code>CANCELED</code>. Renaming either of them would break an
 * application or a running cockpit server for the sake of tidiness.
 */
public enum WorkflowEventKind {

  /** The workflow was started. */
  CREATED,

  /** Business data of the workflow changed. */
  UPDATED,

  /** The workflow reached an end state, and the report says what it ended with. */
  COMPLETED,

  /** The workflow was terminated before reaching an end state. */
  CANCELLED

}
