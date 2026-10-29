package io.vanillabp.cockpit.commons.utils;

import org.slf4j.Logger;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * Produces an SLF4J logger for injection. The logger is named after the class it is injected
 * into.
 */
@Configuration
public class LoggerFactory {

  /**
   * Creates the configuration. Spring calls this constructor.
   */
  public LoggerFactory() {
  }

  /**
   * Builds a logger named after the class it is injected into.
   *
   * @param injectionPoint Where the logger is injected
   * @return The logger
   */
  @Bean
  @Scope("prototype")
  public Logger logger(
      final InjectionPoint injectionPoint) {

    return org.slf4j.LoggerFactory.getLogger(injectionPoint.getMember().getDeclaringClass());

  }

}
