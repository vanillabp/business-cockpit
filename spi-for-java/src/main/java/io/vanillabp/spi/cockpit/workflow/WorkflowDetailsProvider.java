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
 *         final PrefilledWorkflowDetails prefilled
 *         ) {
 * </pre>
 * 
 * The method returns {@link WorkflowDetails}.
 * <p>
 * Two parameters may stand there, in any order. The first is the workflow aggregate, taken by
 * its type and without an annotation, the way a <code>&#64;WorkflowTask</code> method takes it.
 * The second is {@link PrefilledWorkflowDetails}, which carries what the BPMS reported about
 * the workflow and is the object most methods fill in and hand back. Take both, take one of
 * them, or take neither. Any other parameter ends the start of the application, and the message
 * names the method and says what may stand there instead.
 * <p>
 * That is less than {@link io.vanillabp.spi.cockpit.usertask.UserTaskDetailsProvider} takes, and
 * it is meant that way. A user task provider is asked about one task and sees the whole context
 * of it, so process variables and the multi-instance parameters of that task make sense there.
 * This provider stands at process level. A workflow has no multi-instance context, and
 * there is no task whose id could be handed over. Even a user task provider reads the id of its
 * task from its prefilled details and not from a parameter. Here the workflow aggregate is the
 * case the method is asked about, so these two parameters are everything this contract needs.
 * <p>
 * The workflow aggregate is handed over to be read, the same way
 * {@link io.vanillabp.spi.cockpit.usertask.UserTaskDetailsProvider} describes it. VanillaBP
 * does not save it after this method returned, and what your persistence writes on its own is
 * your change with your consequences.
 * 
 * @see WorkflowDetails
 */
@Retention(RUNTIME)
@Target(METHOD)
@Inherited
@Documented
public @interface WorkflowDetailsProvider {

    
}
