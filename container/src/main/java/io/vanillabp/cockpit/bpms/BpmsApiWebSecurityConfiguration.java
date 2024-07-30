package io.vanillabp.cockpit.bpms;

import io.vanillabp.cockpit.bpms.api.v1.BpmsApiController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpBasicServerAuthenticationEntryPoint;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;

public class BpmsApiWebSecurityConfiguration {

    public static final String BPMS_API_ROLE = "BPMS-API";
    
    public static final String BPMS_API_AUTHORITY = "ROLE_" + BPMS_API_ROLE;
    
	@Autowired
	private BpmsApiProperties properties;
	
    @Bean
    @Order(1)
    public SecurityWebFilterChain bpmsApiHttpSecurity(
            final ServerHttpSecurity http) {
        
        final var basicEntryPoint = new HttpBasicServerAuthenticationEntryPoint();
        basicEntryPoint.setRealm(properties.getRealmName());
        
        http
                .securityMatcher(new PathPatternParserServerWebExchangeMatcher(
                        BpmsApiController.BPMS_API_URL_PREFIX + "/**"))
                .csrf().disable()
                .cors().disable()
                .authorizeExchange()
                        .anyExchange()
                        .authenticated()
                        .and()
                .httpBasic()
                        .securityContextRepository(
                                NoOpServerSecurityContextRepository.getInstance())
                        .authenticationManager(
                                new UserDetailsRepositoryReactiveAuthenticationManager(
                                        bpmsApiUserDetailsService()))
                        .authenticationEntryPoint(basicEntryPoint);
        
        return http.build();
    }

    @Bean
    public MapReactiveUserDetailsService bpmsApiUserDetailsService() {
        
        final var user = User.builder()
                .username(properties.getUsername())
                .password(properties.getPassword())
                .roles(BPMS_API_ROLE)
                .build();
        return new MapReactiveUserDetailsService(user);
        
    }
        
}
