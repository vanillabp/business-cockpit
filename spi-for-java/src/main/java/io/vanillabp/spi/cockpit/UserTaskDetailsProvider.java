package io.vanillabp.spi.cockpit;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.vanillabp.spi.cockpit.usertask.UserTaskDetails;
import io.vanillabp.spi.service.TaskId;

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
public @interface UserTaskDetailsProvider {

    static String ALL = "*";
    
    static String NONE = "*";
    
    /**
     * @return The activity's BPMN id. If this property is defined then {@link #taskDefinition()} must not be defined.
     */
    String id() default NONE;

    /**
     * @return The task-definition as defined in the BPMN. Use '*' to have once method for all user tasks of this service class.
     */
    String taskDefinition();
    
    /**
     * Can be used to define certain versions or ranges of versions of a process for
     * which the annotated method should be used for.
     * <p>
     * Format:
     * <ul>
     * <li><i>*</i>: all versions
     * <li><i>1</i>: only version &quot;1&quot;
     * <li><i>1-3</i>: only versions &quot;1&quot;, &quot;2&quot; and &quot;3&quot;
     * <li><i>&gt;3</i>: only versions less than &quot;3&quot;
     * <li><i>&lt;3</i>: only versions higher than &quot;3&quot;</li>
     * </ul>
     * 
     * @throws RuntimeException If a process' version does not match any method
     *                          annotated.
     * @return The version of the process this method belongs to.
     */
    String[] version() default "*";
    
}
