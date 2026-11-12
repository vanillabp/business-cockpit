package io.vanillabp.cockpit.users.directory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.vanillabp.cockpit.support.security.usercontext.UserDirectoryService;

/**
 * Registers the LDAP directory if {@code business-cockpit.user-directory.type} names it. It is a
 * class of its own because Spring LDAP is an optional dependency: the condition on the class keeps
 * every class of Spring LDAP unloaded unless the application has it. The startup check stops the
 * start with a message if {@code ldap} is chosen without it.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.springframework.ldap.core.LdapTemplate")
@ConditionalOnProperty(prefix = UserDirectoryProperties.PREFIX, name = "type", havingValue = "ldap")
@EnableConfigurationProperties(UserDirectoryProperties.class)
public class LdapUserDirectoryConfiguration {

  /**
   * Creates the configuration. Spring calls it while it reads the configuration classes.
   */
  public LdapUserDirectoryConfiguration() {
  }

  /**
   * The users and groups of an LDAP directory.
   *
   * @param properties The settings of the user directory
   * @return The directory
   */
  @Bean(UserDirectoryConfiguration.LDAP_BEAN)
  public UserDirectoryService ldapUserDirectoryService(
      final UserDirectoryProperties properties) {

    return new LdapUserDirectoryService(properties.ldap(), properties.cacheDuration());

  }

}
