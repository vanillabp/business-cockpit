package io.vanillabp.cockpit.users.directory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.vanillabp.cockpit.support.security.usercontext.UserDirectoryService;

/**
 * Registers the Keycloak directory if {@code business-cockpit.user-directory.type} names it.
 * {@link LdapUserDirectoryConfiguration} does the same for LDAP. Without the property nothing is
 * registered, and the application brings its own {@link UserDirectoryService}.
 * <p>
 * The startup check of the cockpit reads the same properties before any bean is built. It stops the
 * start if a value is missing, if the LDAP library is missing, or if the application has a directory
 * of its own as well. So the conditions here only decide which bean is built.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = UserDirectoryProperties.PREFIX, name = "type")
@EnableConfigurationProperties(UserDirectoryProperties.class)
public class UserDirectoryConfiguration {

  /**
   * The bean name of the Keycloak directory.
   */
  public static final String KEYCLOAK_BEAN = "keycloakUserDirectoryService";

  /**
   * The bean name of the LDAP directory.
   */
  public static final String LDAP_BEAN = "ldapUserDirectoryService";

  /**
   * Creates the configuration. Spring calls it while it reads the configuration classes.
   */
  public UserDirectoryConfiguration() {
  }

  /**
   * The users and groups of a Keycloak realm.
   *
   * @param properties The settings of the user directory
   * @return The directory
   */
  @Bean(KEYCLOAK_BEAN)
  @ConditionalOnProperty(prefix = UserDirectoryProperties.PREFIX, name = "type", havingValue = "keycloak")
  public UserDirectoryService keycloakUserDirectoryService(
      final UserDirectoryProperties properties) {

    return new KeycloakUserDirectoryService(properties.keycloak(), properties.cacheDuration());

  }

}
