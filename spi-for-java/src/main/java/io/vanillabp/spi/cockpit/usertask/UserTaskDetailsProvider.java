package io.vanillabp.spi.cockpit.usertask;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a method which provides the details of a user task. Those details go to a user
 * interface, the task list or a data store for example.
 * 
 * <pre>
 * &#64;UserTaskDetailsProvider(taskDefinition = "someUserTask")
 * public UserTaskDetails someUserTaskDetails(
 *         final MyWorkflowAggregate aggregate,
 *         final PrefilledUserTaskDetails prefilled
 *         ) {
 * </pre>
 * 
 * The method returns {@link UserTaskDetails}.
 * <p>
 * The method may take these parameters, in any order, and none of them is required:
 * <ul>
 * <li>the workflow aggregate, by its type and without an annotation, the way a
 * <code>&#64;WorkflowTask</code> method takes it,
 * <li>{@link PrefilledUserTaskDetails}, which carries what the BPMS reported about the task.
 * Most methods fill it in and return it,
 * <li>a process variable named by <code>&#64;TaskParam</code>,
 * <li>the multi-instance parameters of the task,
 * <li><code>&#64;DetailsEvent</code>, which says why the method is called.
 * </ul>
 * Any other parameter stops the application from starting. The message names the method and
 * says which parameters are allowed.
 * <p>
 * The id of the task is not a parameter. It is part of the prefilled details, which every
 * method may take, so <code>&#64;TaskId</code> is not bound here. <code>&#64;TaskId</code> is
 * meant for a BPMN task which VanillaBP completes, and a method which describes a task does not
 * complete it.
 *
 * <p>
 * Which method serves which user task:
 * <ol>
 * <li>the method naming the task, by its {@link #id()}, by its {@link #taskDefinition()},
 * or by carrying neither attribute and being called like the task,
 * <li>the method writing {@link #ALL}, which serves every user task of every BPMN process
 * its <code>&#64;WorkflowService</code> declares.
 * </ol>
 *
 * <p>
 * So the method which names the task wins over the one writing {@link #ALL}. Write the columns
 * every task reports once, and give a task which needs more a method of its own next to it.
 * Name a task in one method only, and write {@link #ALL} in one method only. Two methods which
 * claim the same thing end the boot, and the message names both, because nothing would say
 * which of them to call.
 *
 * <p>
 * Two methods may still name the same task, as long as the versions of the process they
 * serve do not overlap. That is how one generation of a model gets a method of its own. See
 * {@link #version()}.
 *
 * <p>
 * The workflow aggregate is handed over to be read. A provider is asked what to report. It is
 * not told to change the case, so VanillaBP does not save the aggregate after this method
 * returned. Change nothing here. If you do change something and want it kept, save it yourself.
 * Expect a version conflict then, because your report competes with the writes of the workflow
 * itself.
 * <p>
 * Whether a change survives without that save depends on the persistence of your application,
 * not on VanillaBP. JPA writes what changed on a managed object when the transaction commits,
 * whether anybody asked for it or not.
 *
 * @see UserTaskDetails
 */
@Retention(RUNTIME)
@Target(METHOD)
@Inherited
@Documented
@Repeatable(UserTaskDetailsProviders.class)
public @interface UserTaskDetailsProvider {

    /**
     * The default of {@link #id()} and {@link #taskDefinition()}. Where both attributes keep it, the
     * name of the method names the user task, the way it does for a <code>&#64;WorkflowTask</code>
     * method.
     */
    static String USE_METHOD_NAME = "";

    /**
     * Write it into {@link #id()} or into {@link #taskDefinition()}. The method then gets every
     * user task of every BPMN process its <code>&#64;WorkflowService</code> declares, except the
     * tasks which a method of their own names. Writing it into both attributes says the same as
     * writing it into one.
     */
    static String ALL = "*";

    /**
     * The user task this method serves, named by the id of its element in the BPMN file.
     *
     * @return The BPMN id of the activity. Set this one or {@link #taskDefinition()}, never both.
     *         Write {@link #ALL} for a method serving every user task of this workflow service.
     */
    String id() default USE_METHOD_NAME;

    /**
     * The user tasks this method serves, named by their task definition. Every task of that
     * task definition is served.
     *
     * @return The task-definition as defined in the BPMN.
     *         Write {@link #ALL} for a method serving every user task of this workflow service.
     */
    String taskDefinition() default USE_METHOD_NAME;
    
    /**
     * Which versions of the deployed BPMN process this method serves. The version is the version
     * of the process DEFINITION as the BPMS counts it (Camunda 7 and Camunda 8 count integers
     * upwards per BPMN process id), not a version the application invents.
     * <p>
     * A boundary is either such a version or a version TAG given in the model
     * (<code>camunda:versionTag</code> in Camunda 7, <code>zeebe:versionTag</code> in Camunda 8):
     * <ul>
     * <li><i>*</i>: every version (the default)
     * <li><i>3</i> or <i>release-2024</i>: exactly that version, or every version carrying that
     * tag
     * <li><i>1-3</i> or <i>v1.0..v2.0</i>: a range, both boundaries included
     * <li><i>&gt;3</i>, <i>&lt;v2.0</i>: open ended
     * </ul>
     * Ranges accept <code>..</code> as well as <code>-</code> as their separator. A boundary
     * naming a tag which contains a <code>-</code> has to use <code>..</code>.
     * &quot;Greater&quot; and &quot;less&quot; mean the deployment order, which for a BPMS
     * counting versions upwards is the numeric order. It is the spelling
     * <code>&#64;WorkflowTask</code> uses and it means the same here, because both go through
     * the same selection of VanillaBP.
     * <p>
     * Several methods may serve one user task as long as their versions do not overlap, which is
     * how one generation of a model gets a method of its own. Overlapping ones end the start of
     * the application, and the message names both. A method naming no version serves every
     * version, so a workflow service which says nothing about versions behaves as it always did.
     * <p>
     * Unlike a <code>&#64;WorkflowTask</code> method, a method naming no version does not take
     * the range of the <code>&#64;BpmnProcess</code> its process was declared with. That range
     * belongs to the workflow itself, and reporting to the cockpit is not part of running it.
     * <p>
     * The version arrives with the event the cockpit reacts to, so this works only as far as the
     * BPMS reports the version. Camunda 7 and Camunda 8 report it for every task. An engine behind
     * the Process-Engine-API reports a version tag where it fills one in and nothing otherwise.
     * Where no version arrives, only a method naming no version runs, and a method which names one
     * is reported while the application starts instead of silently never running.
     *
     * @return The versions of the deployed BPMN process this method serves
     */
    String[] version() default "*";
    
}
