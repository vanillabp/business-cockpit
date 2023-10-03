package io.vanillabp.cockpit.config.web;

import io.vanillabp.cockpit.commons.security.jwt.JwtLogoutSuccessHandler;
import io.vanillabp.cockpit.commons.security.jwt.JwtSecurityFilter;
import io.vanillabp.cockpit.commons.utils.UserDetailsProvider;
import io.vanillabp.cockpit.config.properties.ApplicationProperties;
import io.vanillabp.cockpit.config.web.security.BasicServerSecurityContextRepository;
import io.vanillabp.cockpit.config.web.security.BasicUserDetailsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpBasicServerAuthenticationEntryPoint;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;

import java.net.URI;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class WebSecurityConfiguration {

    private static final ServerWebExchangeMatcher appInfoWebExchangeMatcher = new PathPatternParserServerWebExchangeMatcher(
            "/gui/api/v1/app/info");

    @Autowired
    private ApplicationProperties properties;

    public JwtSecurityFilter jwtSecurityFilter() {

        return new JwtSecurityFilter(
                properties.getJwt(),
                appInfoWebExchangeMatcher);

    }

    @Bean
    @Order(99)
    @ConditionalOnMissingBean(name = "guiHttpSecurity")
    public SecurityWebFilterChain guiHttpSecurity(
            final ServerHttpSecurity http,
            final MapReactiveUserDetailsService userDetailsService) {
        
        final var basicEntryPoint = new HttpBasicServerAuthenticationEntryPoint();
        basicEntryPoint.setRealm(properties.getTitleShort());

        http
                .csrf().disable()
                .cors().disable()
                .anonymous().disable()
                .authorizeExchange()
                        .matchers(appInfoWebExchangeMatcher)
                                .permitAll()
                        .anyExchange()
                                .authenticated()
                        .and()
                .httpBasic()
                        .securityContextRepository(
                                new BasicServerSecurityContextRepository(properties.getJwt()))
                        .authenticationEntryPoint(basicEntryPoint)
                        .and()
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(jwtLogoutSuccessHandler()))
                .addFilterAfter(jwtSecurityFilter(), SecurityWebFiltersOrder.REACTOR_CONTEXT);
            
        return http.build();
        
    }

    public JwtLogoutSuccessHandler jwtLogoutSuccessHandler() {

        final var handler = new JwtLogoutSuccessHandler(properties.getJwt());
        handler.setLogoutSuccessUrl(URI.create("/"));
        return handler;

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
    public UserDetailsProvider userDetailsProvider(
            final MapReactiveUserDetailsService userDetailsService) {
        
        return new BasicUserDetailsProvider(userDetailsService);
        
    }
    
}
