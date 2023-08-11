package io.vanillabp.cockpit.commons.security.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.crypto.spec.SecretKeySpec;

public class JwtSecurityFilter implements WebFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtSecurityFilter.class);
    
    private JwtProperties properties;
    
    public JwtSecurityFilter(JwtProperties properties) {

        this.properties = properties;

    }

    @Override
    public Mono<Void> filter(
            final ServerWebExchange exchange,
            final WebFilterChain chain) {
        
        try {
            
             final var auth = processJwtToken(exchange.getRequest());

             if (auth != null) {
                 return chain
                         .filter(exchange)
                         .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
             } else {
                 return chain.filter(exchange);
             }

        } catch (Exception e) {
            
            clearCookie(properties, exchange);
            
            return chain
                    .filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(null));

        } finally {
            
            SecurityContextHolder
                    .getContext()
                    .setAuthentication(null);
            SecurityContextHolder
                    .clearContext();
            
        }

    }
    
    public static void clearCookie(
            final JwtProperties properties,
            final ServerWebExchange exchange) {
        
        exchange
                .getResponse()
                .addCookie(
                        ResponseCookie
                                .from(properties.getCookie().getName())
                                .maxAge(0)
                                .domain(properties.getCookie().getDomain())
                                .path(properties.getCookie().getPath())
                                .secure(properties.getCookie().isSecure())
                                .httpOnly(true)
                                .build());
        
    }
    
    private String resolveToken(
            final ServerHttpRequest request) {

        // Common user requests
        final var cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        final var cookieValues = cookies.get(properties.getCookie().getName());
        if ((cookieValues == null) || cookieValues.isEmpty()) {
            return null;
        }
        
        if (cookieValues.size() > 1) {
            logger.warn("Got more than one cookie named '{}': {}. Will use the first!",
                    cookies,
                    properties.getCookie().getName());
        }

        return cookieValues
                .get(0)
                .getValue();

    }
    
    private JwtAuthenticationToken processJwtToken(
            final ServerHttpRequest request) {
        
        final var token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            return null;
        }
        
        final var jwt = buildJwt(token);
        return new JwtAuthenticationToken(jwt, null);
                
    }

    private Jwt buildJwt(
            final String token) {

        final var key = new SecretKeySpec(
                properties.getHmacSHA256(), "HMACSHA256");
        
        final var decoder = NimbusJwtDecoder
                .withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        
        return decoder.decode(token);
        
    }

}
