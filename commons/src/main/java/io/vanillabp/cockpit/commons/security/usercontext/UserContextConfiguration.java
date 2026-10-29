package io.vanillabp.cockpit.commons.security.usercontext;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the {@link UserContext} in a servlet web application.
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class UserContextConfiguration {

    /**
     * Creates the configuration. Spring calls this constructor.
     */
    public UserContextConfiguration() {
    }

    /**
     * The context which tells who is logged in.
     *
     * @param userDetailsProvider Finds the details of the user logged in
     * @return The context
     */
    @Bean
    public UserContext userContext(
            final UserDetailsProvider userDetailsProvider) {

        return new UserContext(userDetailsProvider);

    }

}
