package io.vanillabp.cockpit.simulator.config;

import io.vanillabp.cockpit.commons.security.jwt.JwtUserDetailsProvider;
import io.vanillabp.cockpit.commons.security.jwt.PassiveJwtSecurityFilter;
import io.vanillabp.cockpit.commons.security.usercontext.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class WebSecurityConfiguration {

    @Autowired
    private ApplicationProperties properties;

    @Bean
    public JwtUserDetailsProvider userDetailsProvider() {

        return new JwtUserDetailsProvider();

    }

    @Bean
    public UserContext userContext(
            final JwtUserDetailsProvider userDetailsProvider) {

        return new UserContext(userDetailsProvider);

    }

    @Bean
    @Order(99)
    protected SecurityFilterChain configure(final HttpSecurity http) throws Exception {

        http
                .csrf().disable()
                .cors().disable()
                .anonymous().disable()
                .addFilterAfter(jwtSecurityFilter(), BasicAuthenticationFilter.class);
            
        return http.build();
        
    }

    private PassiveJwtSecurityFilter jwtSecurityFilter() {

        return new PassiveJwtSecurityFilter(
                properties.getJwt());

    }

}
