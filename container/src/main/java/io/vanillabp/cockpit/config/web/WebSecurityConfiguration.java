package io.vanillabp.cockpit.config.web;

import io.vanillabp.cockpit.commons.security.jwt.JwtLogoutSuccessHandler;
import io.vanillabp.cockpit.commons.security.jwt.JwtSecurityWebFilter;
import io.vanillabp.cockpit.commons.security.jwt.ReactiveJwtUserDetailsProvider;
import io.vanillabp.cockpit.commons.security.usercontext.reactive.ReactiveUserDetailsProvider;
import io.vanillabp.cockpit.config.properties.ApplicationProperties;
import io.vanillabp.cockpit.config.web.security.BasicServerSecurityContextRepository;
import io.vanillabp.cockpit.users.UserDetailsProvider;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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

    public static final ServerWebExchangeMatcher appInfoWebExchangeMatcher = new PathPatternParserServerWebExchangeMatcher(
            "/gui/api/v1/app/info");

    public static final ServerWebExchangeMatcher currentUserWebExchangeMatcher = new PathPatternParserServerWebExchangeMatcher(
            "/gui/api/v1/app/current-user");

    public static final ServerWebExchangeMatcher assetsWebExchangeMatcher = new PathPatternParserServerWebExchangeMatcher(
            "/assets/**");

    public static final ServerWebExchangeMatcher staticWebExchangeMatcher = new PathPatternParserServerWebExchangeMatcher(
            "/static/**");

    public static final ServerWebExchangeMatcher workflowModulesProxyWebExchangeMatcher = new PathPatternParserServerWebExchangeMatcher(
            "/wm/**");

    @Autowired
    private ApplicationProperties properties;

    private JwtSecurityWebFilter jwtSecurityFilter() {

        return new JwtSecurityWebFilter(
                properties.getJwt(),
                appInfoWebExchangeMatcher, currentUserWebExchangeMatcher, assetsWebExchangeMatcher,
                staticWebExchangeMatcher, workflowModulesProxyWebExchangeMatcher);

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
                        .matchers(appInfoWebExchangeMatcher, currentUserWebExchangeMatcher, assetsWebExchangeMatcher,
                                staticWebExchangeMatcher, workflowModulesProxyWebExchangeMatcher)
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
                .addFilterAfter(jwtSecurityFilter(), SecurityWebFiltersOrder.HTTP_BASIC);
            
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
    public MapReactiveUserDetailsService userDetailsService(
            final UserDetailsProvider userService) {

        final var users = userService

                .getAllUsers()
                .stream()
                .map(user -> User.builder()
                        .username(user.getId())
                        .password("{noop}test")
                        .authorities(user
                                .getRoles()
                                .stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                                .toList())
                        .build())
                .toList();
        return new MapReactiveUserDetailsService(users);
        
    }
    
    @Bean
    @ConditionalOnMissingBean(name = "userDetailsProvider")
    public ReactiveUserDetailsProvider userDetailsProvider() {
        
        return new ReactiveJwtUserDetailsProvider();
        
    }
    
}
