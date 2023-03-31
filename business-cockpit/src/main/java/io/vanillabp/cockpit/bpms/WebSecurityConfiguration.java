package io.vanillabp.cockpit.bpms;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration("BpmsApiWebSecurityConfiguration")
@Order(1)
public class WebSecurityConfiguration {

    public static final String BPMS_API_ROLE = "BPMS-API";
    
    public static final String BPMS_API_AUTHORITY = "ROLE_" + BPMS_API_ROLE;
    
	@Autowired
	private BpmsApiProperties properties;
	
    @Bean
    protected SecurityFilterChain bpmsApiSecurityFilterChain(
            final HttpSecurity http) throws Exception {

        http
                .csrf().disable()
                .cors().disable()
                .headers()
                        .contentTypeOptions().disable()
                        .frameOptions().disable()
                        .and()
                .sessionManagement()
                        .sessionCreationPolicy(SessionCreationPolicy.NEVER)
                        .and()
//                .authorizeHttpRequests(authorize -> authorize
//                                .requestMatchers(BpmsApiController.BPMS_API_URL_PREFIX)
//                                		.hasRole(BPMS_API_ROLE))
                .httpBasic()
                        .realmName(properties.getRealmName())
                        .and()
                .userDetailsService(bpmsApiUserDetailsService());

        return http.build();
	        
	}
    
    @Bean
    public UserDetailsService bpmsApiUserDetailsService() {
        
        final var user = User.builder()
            .username(properties.getUsername())
            .password(properties.getPassword())
            .roles(BPMS_API_ROLE)
            .build();
        return new InMemoryUserDetailsManager(user);

    }
    
}
