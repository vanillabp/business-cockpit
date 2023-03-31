package io.vanillabp.cockpit.config.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class WebSecurityConfiguration {

	@Configuration
	@Order(99)
	public static class GuiWebSecurityConfiguration {
		
	    @Value("${spring.application.name}")
	    private String applicationName;

	    @Bean
	    protected SecurityFilterChain guiSecurityFilterChain(
	            final HttpSecurity http) throws Exception {

	        http
	                .csrf().disable()
	                .cors().disable()
                    .anonymous().disable()
	                .headers()
	                        .contentTypeOptions().disable()
	                        .frameOptions().disable()
	                        .and()
	                .sessionManagement()
	                        .sessionCreationPolicy(SessionCreationPolicy.NEVER)
	                        .and()
	                .authorizeHttpRequests(authorize -> authorize
	                                .requestMatchers("/gui/api/v1/app-info").permitAll()
	                                .anyRequest().authenticated())
	                .httpBasic()
	                        .realmName(applicationName)
	                        .and()
	                .logout(logout -> logout
	                        .logoutUrl("/logout")
	                        .logoutSuccessUrl("/"));

	        return http.build();
	        
	    }

	}
	
}
