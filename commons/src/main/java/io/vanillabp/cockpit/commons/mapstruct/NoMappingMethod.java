package io.vanillabp.cockpit.commons.mapstruct;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.mapstruct.Qualifier;

/**
 * A MapStruct qualifier binds an ambiguous mapping to one method. No mapping ever names this
 * one, so MapStruct leaves a method carrying it out of the type mapping. That is how a helper
 * method is kept out of its way.
 */
@Qualifier
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface NoMappingMethod {

}
