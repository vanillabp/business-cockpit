package io.vanillabp.cockpit.devshell.simulator.mapstruct;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.mapstruct.Qualifier;

/**
 * A MapStruct qualifier ties an ambiguous mapping to one method. No mapping ever names this
 * qualifier, so MapStruct never picks a method which carries it. Put it on a helper method to
 * keep MapStruct from using that method for a mapping.
 * <p>
 * The cockpit has the same annotation in its library module. The simulators stand in for a
 * workflow module and do not depend on that module, so they carry their own copy.
 */
@Qualifier
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface NoMappingMethod {

}
