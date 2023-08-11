package io.vanillabp.cockpit.commons.security.jwt;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import reactor.core.publisher.Mono;

public class JwtLogoutSuccessHandler implements ServerLogoutSuccessHandler {

    private JwtProperties properties;
    
    public JwtLogoutSuccessHandler(
            final JwtProperties properties) {

        this.properties = properties;

    }

    @Override
    public Mono<Void> onLogoutSuccess(
            final WebFilterExchange exchange,
            final Authentication authentication) {
        
        JwtSecurityFilter.clearCookie(properties, exchange.getExchange());

        return Mono.empty();
        
    }
    
}
