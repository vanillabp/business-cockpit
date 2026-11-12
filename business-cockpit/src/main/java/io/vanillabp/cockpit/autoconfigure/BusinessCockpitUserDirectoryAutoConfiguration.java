package io.vanillabp.cockpit.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

import io.vanillabp.cockpit.users.directory.LdapUserDirectoryConfiguration;
import io.vanillabp.cockpit.users.directory.UserDirectoryConfiguration;

/**
 * The user directories the cockpit brings for Keycloak and for LDAP. One of them is built only if
 * {@code business-cockpit.user-directory.type} names it, so an application with a directory of its
 * own gets nothing from here.
 */
@AutoConfiguration
@Import({
    UserDirectoryConfiguration.class, LdapUserDirectoryConfiguration.class
})
public class BusinessCockpitUserDirectoryAutoConfiguration {

  /**
   * Called by Spring Boot. Everything this configuration adds comes from its annotations.
   */
  public BusinessCockpitUserDirectoryAutoConfiguration() {

  }

}
