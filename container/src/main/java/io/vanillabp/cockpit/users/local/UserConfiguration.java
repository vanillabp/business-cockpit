package io.vanillabp.cockpit.users.local;

import io.vanillabp.cockpit.users.UserDetailsProvider;
import io.vanillabp.cockpit.users.model.PersonAndGroupApiMapper;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class UserConfiguration {

    @Value("${dev-shell-simulator.users-uri:http://localhost:8079/dev-shell/user}")
    private String devShellSimulatorUsersUri;

    @Bean
    @ConditionalOnMissingBean(UserDetailsProvider.class)
    @Profile("local")
    public UserDetailsProvider bcUserDetailsProvider() {

        return new LocalUserDetailsProviderImpl(devShellSimulatorUsersUri);

    }

    @Bean
    @ConditionalOnMissingBean(PersonAndGroupApiMapper.class)
    @Profile("local")
    public PersonAndGroupApiMapper bcPersonAndGroupApiMapper(
            final UserDetailsProvider bcUserDetailsProvider) {

        return new LocalPersonAndGroupApiMapperImpl(bcUserDetailsProvider);

    }

    @Bean
    @ConditionalOnMissingBean(PersonAndGroupMapper.class)
    @Profile("local")
    public PersonAndGroupMapper bcPersonAndGroupMapper(
            final UserDetailsProvider bcUserDetailsProvider) {

        return new LocalPersonAndGroupMapperImpl(bcUserDetailsProvider);

    }

}
