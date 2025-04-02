package io.vanillabp.cockpit.devshell.simulator.config;

import io.vanillabp.cockpit.commons.security.jwt.JwtAuthenticationTokenMapper;
import io.vanillabp.cockpit.commons.security.jwt.JwtMapper;
import io.vanillabp.cockpit.commons.security.jwt.JwtProperties;
import io.vanillabp.cockpit.devshell.simulator.usermanagement.JwtServerSecurityContextRepository;
import io.vanillabp.cockpit.devshell.simulator.usermanagement.UserController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;
import java.util.Optional;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @ConfigurationProperties("application.jwt")
    public JwtProperties jwtProperties() {
        return new JwtProperties();
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain simulatorSecurityFilterChain(final HttpSecurity http)
            throws Exception {
        return http.securityMatchers(
                        c ->
                                c.requestMatchers(
                                        "/official-api/**", // Tasklist-API
                                        "/bpms/api/**" // BPMS-API
                                ))
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .anonymous(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(c -> c.anyRequest().permitAll())
                .build();
    }

    @Bean
    public UserDetailsService inMemoryUserDetailsService(
            final Properties properties) {
        final var users =
                properties.getUsers().stream()
                        .map(
                                user ->
                                        User.withDefaultPasswordEncoder()
                                                .username(user.getId())
                                                .password("Secure_123")
                                                .authorities(
                                                        Optional.ofNullable(user.getGroups()).orElse(List.of()).stream()
                                                                .map(SimpleGrantedAuthority::new)
                                                                .toList())
                                                .build())
                        .toList();
        return new InMemoryUserDetailsManager(users);
    }


    @Bean
    @ConditionalOnMissingBean
    public JwtServerSecurityContextRepository jwtServerSecurityContextRepository(
            final JwtMapper<? extends AbstractAuthenticationToken> jwtMapper,
            final JwtProperties jwtProperties) {
        return new JwtServerSecurityContextRepository(jwtProperties, jwtMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtMapper<? extends AbstractAuthenticationToken> jwtMapper(
            final JwtProperties jwtProperties) {
        return new JwtAuthenticationTokenMapper(jwtProperties);
    }


    @Bean
    @ConditionalOnMissingBean
    public UserController devShellUserDropdownRestController(
            final Properties properties,
            final JwtServerSecurityContextRepository securityContextRepository,
            final JwtProperties jwtProperties) {
        return new UserController(properties, securityContextRepository, jwtProperties);
    }
}