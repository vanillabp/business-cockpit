package io.vanillabp.cockpit.config.web;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
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

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@Order(99)
public class WebSecurityConfiguration {

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    public SecurityWebFilterChain guiHttpSecurity(
            final ServerHttpSecurity http) {
        
        final var basicEntryPoint = new HttpBasicServerAuthenticationEntryPoint();
        basicEntryPoint.setRealm(applicationName);
        
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
                                        userDetailsService()))
                        .authenticationEntryPoint(basicEntryPoint)
                        .and()
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(logoutSuccessHandler));
            
        return http.build();
        
    }

    @Bean
    @Primary
    public MapReactiveUserDetailsService userDetailsService() {
        
        final var user = User.builder()
                .username("test")
                .password("{noop}test")
                .roles()
                .build();
        return new MapReactiveUserDetailsService(user);
        
    }
	
}
