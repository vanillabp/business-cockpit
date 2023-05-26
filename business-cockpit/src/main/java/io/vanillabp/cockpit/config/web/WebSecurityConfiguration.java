package io.vanillabp.cockpit.config.web;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpBasicServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;

import io.vanillabp.cockpit.commons.utils.UserDetails;
import io.vanillabp.cockpit.commons.utils.UserDetailsProvider;
import io.vanillabp.cockpit.config.properties.ApplicationProperties;
import io.vanillabp.cockpit.config.web.security.BasicUserDetails;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@Order(99)
public class WebSecurityConfiguration {

    @Autowired
    private ApplicationProperties properties;

    @Bean
    @ConditionalOnMissingBean(name = "guiHttpSecurity")
    public SecurityWebFilterChain guiHttpSecurity(
            final ServerHttpSecurity http,
            final MapReactiveUserDetailsService userDetailsService) {
        
        final var basicEntryPoint = new HttpBasicServerAuthenticationEntryPoint();
        basicEntryPoint.setRealm(properties.getTitleShort());
        
        final var logoutSuccessHandler = new RedirectServerLogoutSuccessHandler();
        logoutSuccessHandler.setLogoutSuccessUrl(URI.create("/"));
        
        http
                .csrf().disable()
                .cors().disable()
                .anonymous().disable()
                .authorizeExchange()
                        .matchers(new PathPatternParserServerWebExchangeMatcher(
                                "/gui/api/v1/app-info"))
                                .permitAll()
                        .anyExchange()
                                .authenticated()
                        .and()
                .httpBasic()
                        .securityContextRepository(
                                NoOpServerSecurityContextRepository.getInstance())
                        .authenticationManager(
                                new UserDetailsRepositoryReactiveAuthenticationManager(
                                        userDetailsService))
                        .authenticationEntryPoint(basicEntryPoint)
                        .and()
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(logoutSuccessHandler));
            
        return http.build();
        
    }

    @Bean
    @Primary
    @Profile("local")
    public MapReactiveUserDetailsService userDetailsService() {
        
        final var user = User.builder()
                .username("test")
                .password("{noop}test")
                .roles()
                .build();
        return new MapReactiveUserDetailsService(user);
        
    }
    
    @Bean
    @ConditionalOnMissingBean(name = "userDetailsProvider")
    public UserDetailsProvider userDetailsProvider() {
        
        return new UserDetailsProvider() {
                @Override
                public UserDetails getUserDetails(final Object principal) {
                    final var user = (User) principal;
                    return new BasicUserDetails(user);
                }
            };
        
    }
    
}
