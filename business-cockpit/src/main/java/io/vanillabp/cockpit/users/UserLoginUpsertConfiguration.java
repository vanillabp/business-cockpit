package io.vanillabp.cockpit.users;

import java.time.Clock;
import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetailsProvider;

/**
 * Wires the upsert of a user at login, which does not depend on a security filter chain. Both
 * beans are {@code @ConditionalOnMissingBean}, so a derived cockpit application can replace them,
 * for example to fill the user document from its own directory.
 */
@Configuration
public class UserLoginUpsertConfiguration {

  /** At most one login upsert per user per this window and node. */
  private static final Duration UPSERT_THROTTLE = Duration.ofMinutes(5);

  /**
   * Called by Spring.
   */
  public UserLoginUpsertConfiguration() {

  }

  /**
   * The service which writes the user document, at most once per user every five minutes.
   *
   * @param mongoTemplate Used to write the user document
   * @return The service
   */
  @Bean
  @ConditionalOnMissingBean
  public UserLoginUpsertService userLoginUpsertService(
      final MongoTemplate mongoTemplate) {

    return new UserLoginUpsertService(mongoTemplate, UPSERT_THROTTLE, Clock.systemUTC());

  }

  /**
   * The filter which calls the service when a user logs in.
   *
   * @param userDetailsProvider Gives the details of the logged-in user
   * @param userLoginUpsertService Writes the user document
   * @return The filter
   */
  @Bean
  @ConditionalOnMissingBean
  public UserLoginUpsertFilter userLoginUpsertFilter(
      final UserDetailsProvider userDetailsProvider,
      final UserLoginUpsertService userLoginUpsertService) {

    return new UserLoginUpsertFilter(userDetailsProvider, userLoginUpsertService);

  }

}
