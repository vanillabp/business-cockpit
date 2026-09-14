package io.vanillabp.cockpit.extension.springboot.writinghandler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation of a second extension in the test application, one whose methods may write the
 * workflow aggregate.
 * <p>
 * The contracts of the Business Cockpit say that none of their methods writes, so a test which
 * wants to read the warning about a writing handler brings an extension of its own.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface NoteOnTheCase {

}
