package io.vanillabp.spi.cockpit.workflow;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a method which provides the details of a workflow.
 * 
 * <pre>
 * &#64;WorkflowDetailsProvider
 * public WorkflowDetails workflowDetails(
 *         final MyWorkflowAggregate aggregate,
 *         final String workflowId
 *         ) {
 * </pre>
 * 
 * The method returns {@link WorkflowDetails}.
 * <p>
 * The workflow aggregate is handed over to be read, the same way
 * {@link io.vanillabp.spi.cockpit.usertask.UserTaskDetailsProvider} describes it. VanillaBP
 * does not save it after this method returned, and what your persistence writes on its own is
 * your change with your consequences.
 *
 * TODO GWI: parameters
 * 
 * @see WorkflowDetails
 */
@Retention(RUNTIME)
@Target(METHOD)
@Inherited
@Documented
public @interface WorkflowDetailsProvider {

    
}
