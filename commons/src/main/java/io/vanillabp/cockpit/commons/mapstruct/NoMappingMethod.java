package io.vanillabp.cockpit.commons.mapstruct;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.mapstruct.Qualifier;

/**
 * Mapstruct-qualifiers are used to bind ambiguous mapping
 * resolutions to specific mappings. We use this as a
 * workaround to mark helper methods using such a qualifier
 * annotation. But this qualifier is never referenced in
 * any mapping, so the annotated method is ignored for
 * type mapping.
 */
@Qualifier
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface NoMappingMethod {

}
