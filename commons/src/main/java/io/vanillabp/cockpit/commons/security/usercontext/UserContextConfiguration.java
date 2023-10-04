package io.vanillabp.cockpit.commons.security.usercontext;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class UserContextConfiguration {

    @Bean
    public UserContext userContext(
            final UserDetailsProvider userDetailsProvider) {

        return new UserContext(userDetailsProvider);

    }

}
