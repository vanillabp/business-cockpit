package io.vanillabp.spi.cockpit.usertask;

import io.vanillabp.spi.service.TaskId;
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
 * &#64;UserTaskDetails(taskDefinition = "someUserTask")
 * public UserTaskDetails someUserTaskDetails(
 *         final MyWorkflowAggregate aggregate
 *         @TaskId final String taskId,
 *         ) {
 * </pre>
 * 
 * The method returns {@link UserTaskDetails}. To get the id of the task, add a parameter
 * annotated with {@link TaskId}. Parameters carrying a multi-instance annotation work here as
 * well.
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

    static String USE_METHOD_NAME = "";

    /**
     * Write it into {@link #id()} or into {@link #taskDefinition()}. The method then gets every
     * user task of every BPMN process its <code>&#64;WorkflowService</code> declares, except the
     * tasks which a method of their own names. Writing it into both attributes says the same as
     * writing it into one.
     */
    static String ALL = "*";

    /**
     * @return The BPMN id of the activity. Set this one or {@link #taskDefinition()}, never both.
     *         Write {@link #ALL} for a method serving every user task of this workflow service.
     */
    String id() default USE_METHOD_NAME;

    /**
     * @return The task-definition as defined in the BPMN.
     *         Write {@link #ALL} for a method serving every user task of this workflow service.
     */
    String taskDefinition() default USE_METHOD_NAME;
    
    /**
     * Reserved, and expected to stay unset.
     * <p>
     * The idea was to let one method serve certain versions, or ranges of versions, of a
     * process. Version 1 of the Business Cockpit documented the attribute and never read it.
     * Version 2 refuses a value while the application starts, and the message names the
     * method. A value which is silently ignored is worse than one which is refused, and
     * applications wrote this one believing it worked.
     * <p>
     * It would need a version on the events the cockpit reacts to. A task listener says which
     * task fired, not which version of the model it came from, so there is nothing to decide
     * by. Match by {@link #id()} or {@link #taskDefinition()} instead.
     *
     * @return Nothing to set: {@link #ALL}, the default
     */
    String[] version() default ALL;
    
}
