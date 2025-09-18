package io.vanillabp.spi.cockpit.details;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This annotation is used to define a parameter for processing a particular
 * event.
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

    enum Event {
        CREATED,
        CANCELED,
        COMPLETED,
        UPDATED
    }

}
