package io.vanillabp.cockpit.users;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class UserConfiguration {

    @Bean
    @ConditionalOnMissingBean(UserDetailsProvider.class)
    @Profile("local")
    public UserDetailsProvider bcUserDetailsProvider() {
        return new LocalUserDetailsProviderImpl();
    }

}
