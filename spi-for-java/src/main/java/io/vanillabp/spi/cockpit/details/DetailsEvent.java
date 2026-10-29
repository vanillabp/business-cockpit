package io.vanillabp.spi.cockpit.details;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks the parameter which receives the event a details provider is called for.
 *
 * <pre>
 * &#64;UserTaskDetailsProvider(taskDefinition = "myFormKey")
 * public UserTaskDetails details(
 *         final PrefilledUserTaskDetails userTaskDetails,
 *         final MyWorkflowAggregate aggregate,
 *         &#64;DetailsEvent {@link DetailsEvent.Event} event) {
 * </pre>
 */
@Retention(RUNTIME)
@Target(ElementType.PARAMETER)
@Inherited
@Documented
public @interface DetailsEvent {

    /**
     * What happened to the user task the details provider is called for. A provider may use it
     * to report less for a task which has ended, for instance.
     */
    enum Event {
        /** The BPMS created the task. This is the first report the cockpit gets about it. */
        CREATED,
        /**
         * The task ended without being completed, for instance by a boundary event or because
         * its workflow was cancelled.
         */
        CANCELED,
        /** The task was completed. */
        COMPLETED,
        /**
         * The task is still open, but something about it changed. A call of
         * {@code BusinessCockpitService.aggregateChanged} reports this, for instance.
         * {@code BusinessCockpitService.getUserTask} calls the provider with this value as
         * well, although it reports nothing.
         */
        UPDATED
    }

}
