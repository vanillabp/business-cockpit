package io.vanillabp.cockpit.commons.security.usercontext.reactive;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class ReactiveUserContextConfiguration {

    @Bean
    public ReactiveUserContext reactiveUserContext(
            final ReactiveUserDetailsProvider userDetailsProvider) {

        return new ReactiveUserContext(userDetailsProvider);

    }

}
