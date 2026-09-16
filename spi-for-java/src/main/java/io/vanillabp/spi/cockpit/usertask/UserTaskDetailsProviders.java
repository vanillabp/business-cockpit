package io.vanillabp.spi.cockpit.usertask;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Holds the repetitions of {@link UserTaskDetailsProvider} on one method. Java puts them here
 * itself, so nobody writes this annotation by hand.
 *
 * @see UserTaskDetailsProvider
 */
@Retention(RUNTIME)
@Target(METHOD)
@Inherited
@Documented
public @interface UserTaskDetailsProviders {

    UserTaskDetailsProvider[] value();
    
}
