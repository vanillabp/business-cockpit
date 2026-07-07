package io.vanillabp.cockpit.users;

import io.vanillabp.cockpit.commons.security.usercontext.reactive.ReactiveUserDetailsProvider;
import java.time.Clock;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

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
            final ReactiveMongoTemplate mongoTemplate) {

        return new UserLoginUpsertService(mongoTemplate, UPSERT_THROTTLE, Clock.systemUTC());

    }

    @Bean
    @ConditionalOnMissingBean
    public UserLoginUpsertWebFilter userLoginUpsertWebFilter(
            final ReactiveUserDetailsProvider userDetailsProvider,
            final UserLoginUpsertService userLoginUpsertService) {

        return new UserLoginUpsertWebFilter(userDetailsProvider, userLoginUpsertService);

    }

}
