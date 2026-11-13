package io.vanillabp.cockpit.users.directory;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * The user directory the cockpit brings, read from {@code business-cockpit.user-directory.*}.
 * Without a {@code type} the cockpit brings none, and the application provides its own bean of type
 * {@code UserDirectoryService}.
 *
 * @param type Which directory the cockpit brings
 * @param cacheDuration How long the directory keeps a user with its groups before it asks again. A
 *          group given or taken away counts after this time at most. Zero switches the cache off
 * @param cacheClearCron When the directory forgets every user it keeps, as a cron expression of
 *          Spring in the time zone of the server. By default every night at 2 a.m., so together with
 *          the 14 hours of {@code cacheDuration} a change in the directory counts the next day at
 *          the latest. {@code -} switches this off
 * @param keycloak How to reach Keycloak, read if {@code type} is {@code keycloak}
 * @param ldap How to reach the LDAP directory, read if {@code type} is {@code ldap}
 */
@ConfigurationProperties(prefix = UserDirectoryProperties.PREFIX)
public record UserDirectoryProperties(
                                      UserDirectoryType type,
                                      @DefaultValue(UserDirectoryProperties.DEFAULT_CACHE_DURATION) Duration cacheDuration,
                                      @DefaultValue(UserDirectoryProperties.DEFAULT_CACHE_CLEAR_CRON) String cacheClearCron,
                                      @DefaultValue KeycloakUserDirectoryProperties keycloak,
                                      @DefaultValue LdapUserDirectoryProperties ldap) {

  /**
   * The prefix of every property of the user directory.
   */
  public static final String PREFIX = "business-cockpit.user-directory";

  /**
   * How long a user is kept if nothing else is configured.
   */
  public static final String DEFAULT_CACHE_DURATION = "PT14H";

  /**
   * When the directory forgets every user if nothing else is configured: every night at 2 a.m.
   */
  public static final String DEFAULT_CACHE_CLEAR_CRON = "0 0 2 * * *";

  /**
   * The value of {@code cacheClearCron} which switches the nightly clearing off. It is Spring's own
   * value for a cron which never runs.
   */
  public static final String CACHE_CLEAR_SWITCHED_OFF = Scheduled.CRON_DISABLED;

}
