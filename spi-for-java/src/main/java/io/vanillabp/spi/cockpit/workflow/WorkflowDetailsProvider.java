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
 *  * <p>
 * A BPMN process has one such method. It may have one per generation of the model instead, and
 * then each of them names the versions it serves, see {@link #version()}.
 * 
 * @see WorkflowDetails
 */
@Retention(RUNTIME)
@Target(METHOD)
@Inherited
@Documented
public @interface WorkflowDetailsProvider {

    /**
     * Which versions of the deployed BPMN process this method serves. The version is the version
     * of the process DEFINITION as the BPMS counts it (Camunda 7 and Camunda 8 count integers
     * upwards per BPMN process id), not a version the application invents.
     * <p>
     * A boundary is either such a version or a version TAG given in the model
     * (<code>camunda:versionTag</code> in Camunda 7, <code>zeebe:versionTag</code> in Camunda 8):
     * <ul>
     * <li><i>*</i>: every version (the default)
     * <li><i>3</i> or <i>release-2024</i>: exactly that version, respectively every version
     * carrying that tag
     * <li><i>1-3</i> or <i>v1.0..v2.0</i>: a range, both boundaries included
     * <li><i>&gt;3</i>, <i>&lt;v2.0</i>: open ended
     * </ul>
     * Ranges accept <code>..</code> as well as <code>-</code> as their separator. A boundary
     * naming a tag which contains a <code>-</code> has to use <code>..</code>.
     * &quot;Greater&quot; and &quot;less&quot; mean the deployment order, which for a BPMS
     * counting versions upwards is the numeric order. It is the spelling
     * <code>&#64;WorkflowTask</code> uses and it means the same here, because both go through
     * the same selection of VanillaBP. The attribute of
     * {@link io.vanillabp.spi.cockpit.usertask.UserTaskDetailsProvider} reads the same way.
     * <p>
     * Such a method stands for the whole BPMN process, so two of them in one workflow service
     * are told apart by their versions and by nothing else. Overlapping ones end the start of
     * the application, and the message names both. A method naming no version serves every
     * version, so a workflow service which says nothing about versions behaves as it always did.
     * <p>
     * Unlike a <code>&#64;WorkflowTask</code> method, a method naming no version does not take
     * the range of the <code>&#64;BpmnProcess</code> its process was declared with. That range
     * belongs to the workflow itself, and reporting to the cockpit is not part of running it.
     * <p>
     * The version arrives with the event the cockpit reacts to, so what the BPMS reports decides
     * how far this gets. Camunda 7 and Camunda 8 report it for every workflow. An engine behind
     * the Process-Engine-API reports a version tag where it fills one in and nothing otherwise.
     * Where no version arrives, only a method naming no version runs, and a method which names
     * one is reported while the application starts instead of silently never running.
     *
     * @return The versions of the deployed BPMN process this method serves
     */
    String[] version() default "*";

}
