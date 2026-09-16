package io.vanillabp.cockpit.commons.mongo.changesets;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a Spring bean as a holder of MongoDb migration steps. The auto-configuration asks the
 * application context for the beans which carry this annotation and reads their methods.
 * <p>
 * The class name of such a bean is half of the identity of every step it holds, so renaming the
 * class makes its steps run again on a database which already has them.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DbChangesetConfiguration {

    /**
     * Who wrote the steps of this bean. A step which names no author of its own takes this one.
     *
     * @see DbChangeset#author()
     */
    String author();
    
}
