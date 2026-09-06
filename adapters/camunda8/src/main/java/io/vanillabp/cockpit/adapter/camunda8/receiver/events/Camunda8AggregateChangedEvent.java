package io.vanillabp.cockpit.adapter.camunda8.receiver.events;

/**
 * Announces that a workflow aggregate changed, without naming the workflows the change is about.
 *
 * <p>On Camunda 8 that question cannot be answered while the caller's transaction is still open:
 * the workflow may have been started in that very transaction, and VanillaBP's Camunda 8 adapter
 * sends the create-instance command only after the commit. Even for a workflow started earlier, the
 * search which answers "which workflows carry this aggregate id" reads the secondary storage of the
 * cluster, which lags behind the broker. So the lookup is what this event defers, and
 * {@link #findWorkflowsAndNotifyTheCockpit()} performs it once the transaction is through.
 */
public class Camunda8AggregateChangedEvent {

    private final Runnable findWorkflowsAndNotifyTheCockpit;

    public Camunda8AggregateChangedEvent(
            final Runnable findWorkflowsAndNotifyTheCockpit) {

        this.findWorkflowsAndNotifyTheCockpit = findWorkflowsAndNotifyTheCockpit;

    }

    /**
     * Asks Camunda 8 which workflows the changed aggregate belongs to and hands each of them to the
     * cockpit's event handlers. Must not be called before the caller's transaction committed.
     */
    public void findWorkflowsAndNotifyTheCockpit() {

        findWorkflowsAndNotifyTheCockpit.run();

    }

}
