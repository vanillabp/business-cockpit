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
 * @see UserTaskDetails
 */
@Retention(RUNTIME)
@Target(METHOD)
@Inherited
@Documented
@Repeatable(UserTaskDetailsProviders.class)
public @interface UserTaskDetailsProvider {

    static String USE_METHOD_NAME = "";

    static String ALL = "*";
    
    /**
     * @return The activity's BPMN id. If this property is defined then {@link #taskDefinition()} must not be defined.
     */
    String id() default USE_METHOD_NAME;

    /**
     * @return The task-definition as defined in the BPMN. Use '*' to have once method for all user tasks of this service class.
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
