package io.vanillabp.cockpit.commons.utils;

import org.slf4j.Logger;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * Produces SLF4J logger to be injected. The logger is initialized
 * with the class in which it was injected.
 */
@Configuration
public class LoggerFactory {

    @Bean
    @Scope("prototype")
    public Logger logger(final InjectionPoint injectionPoint) {

        return org.slf4j.LoggerFactory.getLogger(injectionPoint.getMember().getDeclaringClass());

    }

}
