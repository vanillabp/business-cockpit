package io.vanillabp.cockpit.devshell.simulator.config;

import io.vanillabp.cockpit.commons.security.jwt.JwtProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;

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
    public SecurityFilterChain anyRequestPermittedSecurityFilterChain(final HttpSecurity http)
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
}