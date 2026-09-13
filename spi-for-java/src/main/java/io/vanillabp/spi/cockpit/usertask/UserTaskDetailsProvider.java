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
 * This annotation is used to define a method for providing details for a certain
 * user-task which will be available to a user interface (tasklist, data-store, etc.).
 * 
 * <pre>
 * &#64;UserTaskDetails(taskDefinition = "someUserTask")
 * public UserTaskDetails someUserTaskDetails(
 *         final MyWorkflowAggregate aggregate
 *         @TaskId final String taskId,
 *         ) {
 * </pre>
 * 
 * A result type {@link UserTaskDetails} is expected. The effected task-id can be passed
 * by defining a parameter annotated by {@link TaskId}. Also parameters annotated using
 * multi-instance annotations are supported.
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
 * The method naming the task therefore wins over the one writing {@link #ALL}: reporting the
 * same columns for every task is written once, and a task needing more than that gets a
 * method of its own next to it. Name a task in one method only, and write {@link #ALL} in
 * one method only. Two methods claiming the same thing end the boot, naming both, because
 * nothing would say which of them to call.
 *
 * <p>
 * The workflow aggregate is handed over to be read. A provider is asked what to report, it
 * is not told to change the case, so VanillaBP does not save the aggregate after this method
 * returned. Change nothing here. If you do change something and want it kept, save it
 * yourself, and expect the version conflict a report competing with the workflow's own writes
 * can produce.
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
     * Written into {@link #id()} or into {@link #taskDefinition()}, it hands the method every
     * user task of every BPMN process its <code>&#64;WorkflowService</code> declares, except
     * those a method of its own names. Writing it into both attributes says the same as
     * writing it into one.
     */
    static String ALL = "*";

    /**
     * @return The activity's BPMN id. If this property is defined then {@link #taskDefinition()} must not be defined.
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
     * The idea is to let one method serve certain versions or ranges of versions of a
     * process. Version 1 of the Business Cockpit documented the attribute and never read
     * it; version 2 refuses a value instead, at startup, naming the method - a value which
     * is silently ignored is worse than one which is refused, and applications wrote this
     * one believing it worked.
     * <p>
     * What it would take is a version on the events the cockpit reacts to. A task listener
     * says which task fired, not which version of the model it came from, so there is
     * nothing to decide by. Match by {@link #id()} or {@link #taskDefinition()} instead.
     *
     * @return Nothing to set: {@link #ALL}, the default
     */
    String[] version() default ALL;
    
}
