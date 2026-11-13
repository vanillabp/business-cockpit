package io.vanillabp.cockpit.users.directory;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.vanillabp.cockpit.support.security.usercontext.UserDirectoryService;

/**
 * Registers {@link UserDirectoryCacheEndpoint}, which lets operations empty the cache of the directory
 * the cockpit brings. It is a class of its own because Spring Boot Actuator is an optional dependency:
 * the condition on the class keeps every class of Actuator unloaded unless the application has it.
 * The endpoint is built only if the application also exposes it.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(
    name = "org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint")
@ConditionalOnProperty(prefix = UserDirectoryProperties.PREFIX, name = "type")
public class UserDirectoryCacheEndpointConfiguration {

  /**
   * Creates the configuration. Spring calls it while it reads the configuration classes.
   */
  public UserDirectoryCacheEndpointConfiguration() {
  }

  /**
   * The endpoint {@code userdirectorycache}.
   *
   * @param directory The directory the cockpit brings
   * @return The endpoint
   */
  @Bean
  @ConditionalOnBean(UserDirectoryService.class)
  @ConditionalOnAvailableEndpoint(UserDirectoryCacheEndpoint.class)
  public UserDirectoryCacheEndpoint userDirectoryCacheEndpoint(
      final UserDirectoryService directory) {

    return new UserDirectoryCacheEndpoint(UserDirectoryCacheConfiguration.withCache(directory));

  }

}
