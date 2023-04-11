package io.vanillabp.cockpit.commons.mongo.changesets;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this annotation to mark beans as MongoDb initialization beans.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ChangesetConfiguration {

    /**
     * Each initialization bean has to declare its author. It may be
     * used as a default author for changeset methods which do not
     * declare an author.
     * 
     * @see DbChangeset#author()
     */
    String author();
    
}
