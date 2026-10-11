package io.vanillabp.cockpit.commons.mapstruct;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.mapstruct.Qualifier;

/**
 * A MapStruct qualifier ties an ambiguous mapping to one method. No mapping ever names this
 * qualifier, so MapStruct never picks a method which carries it. Put it on a helper method to
 * keep MapStruct from using that method for a mapping.
 */
@Qualifier
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface NoMappingMethod {

}
