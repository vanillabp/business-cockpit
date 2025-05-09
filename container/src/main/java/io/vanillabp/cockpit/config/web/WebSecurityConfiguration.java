package io.vanillabp.cockpit.config.web;

import io.vanillabp.cockpit.bpms.BpmsApiWebSecurityConfiguration;
import io.vanillabp.cockpit.commons.security.jwt.JwtAuthenticationToken;
import io.vanillabp.cockpit.commons.security.jwt.JwtAuthenticationTokenMapper;
import io.vanillabp.cockpit.commons.security.jwt.JwtLogoutSuccessHandler;
import io.vanillabp.cockpit.commons.security.jwt.JwtMapper;
import io.vanillabp.cockpit.commons.security.jwt.JwtSecurityWebFilter;
import io.vanillabp.cockpit.commons.security.jwt.JwtServerSecurityContextRepository;
import io.vanillabp.cockpit.commons.security.jwt.ReactiveJwtUserDetailsProvider;
import io.vanillabp.cockpit.commons.security.usercontext.reactive.ReactiveUserDetailsProvider;
import io.vanillabp.cockpit.config.properties.ApplicationProperties;
import io.vanillabp.cockpit.users.UserDetailsProvider;
import java.net.URI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpBasicServerAuthenticationEntryPoint;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;

@Configuration
@Import(BpmsApiWebSecurityConfiguration.class)
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

    private JwtSecurityWebFilter jwtSecurityFilter(
            final JwtServerSecurityContextRepository jwtServerSecurityContextRepository) {

        return new JwtSecurityWebFilter(
                properties.getJwt(),
                jwtServerSecurityContextRepository,
                appInfoWebExchangeMatcher, currentUserWebExchangeMatcher, assetsWebExchangeMatcher,
                staticWebExchangeMatcher, workflowModulesProxyWebExchangeMatcher);

    }

    @Bean
    @ConditionalOnMissingBean
    public JwtMapper<? extends JwtAuthenticationToken> jwtMapper() {

        return new JwtAuthenticationTokenMapper(properties.getJwt());

    }

    @Bean
    public JwtServerSecurityContextRepository jwtServerSecurityContextRepository(
            final JwtMapper<? extends AbstractAuthenticationToken> jwtMapper) {

        return new JwtServerSecurityContextRepository(
                properties.getJwt(),
                jwtMapper);

    }

    @Bean
    @Order(99)
    @ConditionalOnMissingBean(name = "guiHttpSecurity")
    public SecurityWebFilterChain guiHttpSecurity(
            final ServerHttpSecurity http,
            final JwtServerSecurityContextRepository jwtServerSecurityContextRepository,
            final JwtMapper<? extends JwtAuthenticationToken> jwtMapper,
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
                        .securityContextRepository(jwtServerSecurityContextRepository)
                        .authenticationEntryPoint(basicEntryPoint)
                        .and()
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(jwtLogoutSuccessHandler()))
                .addFilterAfter(jwtSecurityFilter(jwtServerSecurityContextRepository), SecurityWebFiltersOrder.HTTP_BASIC);
            
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
                                .getAuthorities()
                                .stream()
                                .map(SimpleGrantedAuthority::new)
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
