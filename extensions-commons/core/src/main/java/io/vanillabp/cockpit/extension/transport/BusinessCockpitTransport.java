package io.vanillabp.cockpit.extension.transport;

import io.vanillabp.cockpit.extension.event.RegisterWorkflowModuleEvent;
import io.vanillabp.cockpit.extension.event.UserTaskEvent;
import io.vanillabp.cockpit.extension.event.WorkflowEvent;

/**
 * How the extension reaches the cockpit server. Two of them ship with the extension, one over
 * REST and one over Kafka, and an application chooses by configuring one of them.
 * <p>
 * An application may also bring its own. Both platform modules provide the shipped transport as a
 * bean it can replace, which is the seam version 1 of the Business Cockpit had on its three
 * publishing beans. So this interface is a published contract, and
 * {@link io.vanillabp.cockpit.extension.BusinessCockpitAssembly#transportOf} is how an application
 * builds the shipped transport it wants to wrap. See decision 22 in the repository's DECISIONS.md.
 * <p>
 * Every method is called while an outbox entry is dispatched, so a failure is reported by
 * throwing. The entry stays and is retried with a backoff, and no event is lost because a
 * cockpit server was restarting.
 * <p>
 * Two failures are worth telling apart from that. A server which says how long it will be
 * unavailable is answered with <code>io.vanillabp.integration.spi.PhaseTwoRetryLater</code>,
 * which gives the entry back instead of holding the dispatching thread. A server which refuses
 * the report itself, because of a payload it does not accept or a credential it does not know,
 * ends the entry with <code>io.vanillabp.integration.spi.PhaseTwoPermanentFailure</code>,
 * because sending the same bytes again would be refused again. Everything else is repeated,
 * which is the safe side of the classification.
 */
public interface BusinessCockpitTransport {

  /**
   * @return How this transport is called in a message about it
   */
  String describe();

  /**
   * Sends one user-task event.
   *
   * @param event The event
   */
  void publishUserTaskEvent(
      UserTaskEvent event);

  /**
   * Sends one workflow event.
   *
   * @param event The event
   */
  void publishWorkflowEvent(
      WorkflowEvent event);

  /**
   * Registers one workflow module.
   *
   * @param event The registration
   */
  void registerWorkflowModule(
      RegisterWorkflowModuleEvent event);

  /**
   * Releases whatever the transport holds - a connection pool, a Kafka producer. Called when
   * the application shuts down.
   */
  default void close() {
  }

}
