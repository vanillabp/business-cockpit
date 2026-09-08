package io.vanillabp.cockpit.extension.spi;

/**
 * Where the outbox entry of an observed event is written.
 * <p>
 * The entry has to be scheduled inside a transaction which commits together with whatever the
 * BPMS did, so that an event is never reported for something which was rolled back and never
 * lost for something which was not. Which transaction that is depends on how the BPMS delivers
 * its events, and only the BPMS half knows.
 */
public enum EventTransaction {

  /**
   * Schedule in the transaction which is already running on this thread. This is what an
   * embedded engine needs: Camunda 7 invokes its task listeners inside the engine's own
   * transaction, so the entry belongs in that one and in no other.
   * <p>
   * A thread which carries no transaction is a defect and is answered with a message saying so,
   * rather than with an entry committed on its own: an entry written outside the transaction
   * would report an event whether or not what caused it survived.
   */
  CURRENT,

  /**
   * Open a transaction for the entry alone. This is what a remote engine needs: a Camunda 8
   * job worker runs on a thread of its own with nothing to join, and the job is completed only
   * after the entry was committed.
   */
  NEW

}
