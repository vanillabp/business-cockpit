package io.vanillabp.cockpit.users.directory;

import java.time.ZoneId;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.vanillabp.cockpit.support.security.usercontext.UserDirectoryService;

/**
 * Empties the cache of the directory the cockpit brings every night, by {@link NightlyCacheClearing}.
 * {@link UserDirectoryCacheEndpointConfiguration} adds the endpoint which empties it on request of
 * operations.
 * <p>
 * With a {@code type} the startup check makes sure the directory of the cockpit is the only
 * {@link UserDirectoryService} of the application, so the one which is injected here is the one with
 * the cache. Without Spring LDAP an LDAP directory is not built, and the startup check stops the
 * start with a message. So nothing here asks for a directory which is not there.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = UserDirectoryProperties.PREFIX, name = "type")
@EnableConfigurationProperties(UserDirectoryProperties.class)
public class UserDirectoryCacheConfiguration {

  /**
   * Creates the configuration. Spring calls it while it reads the configuration classes.
   */
  public UserDirectoryCacheConfiguration() {
  }

  /**
   * Empties the cache at the time {@code cache-clear-cron} names, in the time zone of the server.
   *
   * @param directory The directory the cockpit brings
   * @param properties The settings of the user directory
   * @return The clearing, which registers itself with Spring's scheduler
   */
  @Bean
  @ConditionalOnBean(UserDirectoryService.class)
  public NightlyCacheClearing userDirectoryNightlyCacheClearing(
      final UserDirectoryService directory,
      final UserDirectoryProperties properties) {

    return new NightlyCacheClearing(withCache(directory), properties.cacheClearCron(), ZoneId.systemDefault());

  }

  static CachingUserDirectoryService withCache(
      final UserDirectoryService directory) {

    if (directory instanceof CachingUserDirectoryService caching) {
      return caching;
    }
    // the startup check stops the start before, so this is a bug of the cockpit
    throw new IllegalStateException("The user directory of the cockpit was expected, but the application has '"
        + directory.getClass().getName()
        + "'");

  }

}
