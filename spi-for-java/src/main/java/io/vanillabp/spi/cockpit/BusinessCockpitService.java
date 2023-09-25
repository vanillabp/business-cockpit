package io.vanillabp.spi.cockpit;

/**
 * A service to interact with the business cockpit.
 * 
 * @param <WA> The workflow-aggregate-class
 */
public interface BusinessCockpitService<WA> {

    /**
     * Indicate that the aggregate changed and an update
     * of workflow data stored in business cockpit is needed.
     * <p>
     * Processing is done asynchronous after the current
     * transaction and only if the transaction completes
     * successfully.
     * <p>
     * <i>Hint:</i> This does not update any user task,
     * only workflow data. To update data of user tasks
     * use {@link #aggregateChanged(Object, String...)}
     * instead.
     * 
     * @param workflowAggregate The workflow's aggregate
     */
    void aggregateChanged(WA workflowAggregate);

    /**
     * Indicate that the aggregate changed and an update
     * of user-task data stored in business cockpit is needed.
     * <p>
     * Processing is done asynchronous after the current
     * transaction and only if the transaction completes
     * successfully.
     * 
     * @param workflowAggregate The workflow's aggregate
     */
    void aggregateChanged(WA workflowAggregate, String... userTaskIds);
    
}
