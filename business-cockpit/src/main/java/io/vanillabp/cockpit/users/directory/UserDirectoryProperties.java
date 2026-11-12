package io.vanillabp.cockpit.users.directory;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * The user directory the cockpit brings, read from {@code business-cockpit.user-directory.*}.
 * Without a {@code type} the cockpit brings none, and the application provides its own bean of type
 * {@code UserDirectoryService}.
 *
 * @param type Which directory the cockpit brings
 * @param cacheDuration How long the directory keeps a user with its groups before it asks again. A
 *          group taken away is gone after this time at most. Zero switches the cache off
 * @param keycloak How to reach Keycloak, read if {@code type} is {@code keycloak}
 * @param ldap How to reach the LDAP directory, read if {@code type} is {@code ldap}
 */
@ConfigurationProperties(prefix = UserDirectoryProperties.PREFIX)
public record UserDirectoryProperties(
                                      UserDirectoryType type,
                                      @DefaultValue(UserDirectoryProperties.DEFAULT_CACHE_DURATION) Duration cacheDuration,
                                      @DefaultValue KeycloakUserDirectoryProperties keycloak,
                                      @DefaultValue LdapUserDirectoryProperties ldap) {

  /**
   * The prefix of every property of the user directory.
   */
  public static final String PREFIX = "business-cockpit.user-directory";

  /**
   * How long a user is kept if nothing else is configured.
   */
  public static final String DEFAULT_CACHE_DURATION = "PT5M";

}
