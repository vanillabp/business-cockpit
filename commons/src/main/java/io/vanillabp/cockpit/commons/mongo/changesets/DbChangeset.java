package io.vanillabp.cockpit.commons.mongo.changesets;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method of a changeset bean as one step of the MongoDb migration. The method takes a
 * MongoTemplate as its only parameter, or no parameter at all.
 * <p>
 * A step runs once per database. What ran is stored under the class name of the bean and the
 * name of the method, so renaming either of the two makes the step run again on a database
 * which already has it.
 * <p>
 * The method answers with a MongoDb script which undoes what it did, or with a
 * java.util.List of scripts where one is not enough. A rollback runs those scripts. The
 * return type has to be a String or a Collection of Strings, and a step with nothing to undo
 * answers with null.
 * <p>
 * For the syntax of a rollback script see https://docs.mongodb.com/manual/reference/command/.
 * Two examples:
 * <ol>
 * <li>Drop collection: <pre>{ drop: 'mycollection' }</pre></li>
 * <li>Drop index: <pre>{ drop: 'mycollection', index = 'myindex' }</pre></li>
 * </ol>
 *
 * @see DbChangesetConfiguration
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DbChangeset {

    /**
     * Who wrote this step. A step which leaves this empty takes the author of its bean.
     *
     * @see DbChangesetConfiguration#author()
     */
    String author() default "";
    
    /**
     * Where this step stands in the order the steps run in. The order is read across all
     * changeset beans of the application, so two steps which declare the same number end the
     * start before any of them runs.
     */
    int order();
    
}
