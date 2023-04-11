package io.vanillabp.cockpit.commons.mongo.changesets;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this annotation to mark a method as a MongoDb intializer method. The parameter
 * takes a MongoTemplate as its only parameter.
 * <p>
 * The method may return a string which is a MongoDb script to rollback the
 * changes made by the method. It is also possible to return a java.util.List of Strings
 * for multiple script commands. If no rollback is needed the return type may be void.
 * <p>
 * For the syntax of rollback scripts see https://docs.mongodb.com/manual/reference/command/. Examples:
 * <ol>
 * <li>Drop collection: <pre>{ drop: 'mycollection' }</pre></li>
 * <li>Drop index: <pre>{ drop: 'mycollection', index = 'myindex' }</pre></li>
 * </ol>
 * 
 * @see DbChangesetConfiguration
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Changeset {

    /**
     * Each changeset method has to declare its author. If not given then
     * the value of the bean class annotation DbChangesetConfiguration is
     * used.
     * 
     * @see DbChangesetConfiguration#author()
     */
    String author() default "";
    
    /**
     * Each changeset method has to declare its order. If and order value
     * is declared more than one time then an exception is thrown before
     * initialization and the runtime is stopped.
     */
    int order();
    
}
