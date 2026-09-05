package io.vanillabp.cockpit.users;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetailsProvider;
import java.time.Clock;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Wires the chain-independent login user-upsert. Both beans are {@code @ConditionalOnMissingBean}
 * so a derived cockpit application can replace them (e.g. to enrich the user document from its own
 * directory).
 */
@Configuration
public class UserLoginUpsertConfiguration {

    /** At most one login upsert per user per this window and node. */
    private static final Duration UPSERT_THROTTLE = Duration.ofMinutes(5);

    @Bean
    @ConditionalOnMissingBean
    public UserLoginUpsertService userLoginUpsertService(
            final MongoTemplate mongoTemplate) {

        return new UserLoginUpsertService(mongoTemplate, UPSERT_THROTTLE, Clock.systemUTC());

    }

    @Bean
    @ConditionalOnMissingBean
    public UserLoginUpsertFilter userLoginUpsertFilter(
            final UserDetailsProvider userDetailsProvider,
            final UserLoginUpsertService userLoginUpsertService) {

        return new UserLoginUpsertFilter(userDetailsProvider, userLoginUpsertService);

    }

}
