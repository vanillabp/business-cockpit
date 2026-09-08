package io.vanillabp.cockpit.extension.transport;

import io.vanillabp.cockpit.extension.event.RegisterWorkflowModuleEvent;
import io.vanillabp.cockpit.extension.event.UserTaskEvent;
import io.vanillabp.cockpit.extension.event.WorkflowEvent;

/**
 * How the extension reaches the cockpit server. There are two, one over REST and one over
 * Kafka, and an application chooses by configuring one of them.
 * <p>
 * Every method is called while an outbox entry is dispatched, so a failure is reported by
 * throwing: the entry stays and is retried with a backoff, and no event is lost because a
 * cockpit server was restarting.
 * <p>
 * Two failures are worth telling apart from that. A server which says how long it will be
 * unavailable is answered with <code>io.vanillabp.integration.spi.PhaseTwoRetryLater</code>,
 * which gives the entry back instead of holding the dispatching thread. A server which refuses
 * the report itself - a payload it does not accept, a credential it does not know - ends the
 * entry with <code>io.vanillabp.integration.spi.PhaseTwoPermanentFailure</code>, because
 * sending the same bytes again would be refused again. Everything else is repeated, which is
 * the safe side of the classification.
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
