package io.vanillabp.cockpit.commons.security.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class JwtSecurityWebFilter implements WebFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtSecurityWebFilter.class);
    
    private final JwtProperties properties;

    private final List<ServerWebExchangeMatcher> unprotectedExchangeMatchers;
    
    public JwtSecurityWebFilter(
            final JwtProperties properties,
            final ServerWebExchangeMatcher... unprotectedExchangeMatchers) {

        this.properties = properties;
        this.unprotectedExchangeMatchers = Arrays.asList(unprotectedExchangeMatchers);

    }

    @Override
    public Mono<Void> filter(
            final ServerWebExchange exchange,
            final WebFilterChain chain) {

        JwtAuthenticationToken auth = null;
        try {
            auth = processJwtToken(exchange);
        } catch (Exception e) {
            logger.error("Cannot process JWT token", e);
            clearCookie(properties, exchange);
        }
        if (auth == null) {
            return chain.filter(exchange);
        }

        final var securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(auth);
        return chain
                .filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));

        /*
        return unprotectedExchangeMatchers
                .get(unprotectedExchangeMatchers.size() - 1)
                .matches(exchange)
                .filter(ServerWebExchangeMatcher.MatchResult::isMatch)
                .flatMap(result -> chain.filter(exchange))
                .switchIfEmpty(chain.filter(exchange));
*/
                            /*
        return Flux
                .fromIterable(unprotectedExchangeMatchers)  // for each matcher
                .flatMap(matcher -> matcher.matches(exchange))
                .filter(ServerWebExchangeMatcher.MatchResult::isMatch)
                .flatMap(found -> chain.filter(exchange))
                .next()
                .switchIfEmpty(chain.filter(exchange).then(Mono.empty()));
                //.flatMap(unprotectedExchangeMapper -> chain.filter(exchange)) // proceed with chain for unprotected URLs
                .switchIfEmpty(                             // set authentication for protected URLs
                        Mono.defer(() -> {
                            JwtAuthenticationToken auth = null;
                            try {
                                auth = processJwtToken(exchange.getRequest());
                            } catch (Exception e) {
                                logger.error("Cannot process JWT token", e);
                                clearCookie(properties, exchange);
                            }
                            if (auth != null) {
                                // https://stackoverflow.com/questions/77219897/spring-webflux-avoid-reauthentication-by-basic-after-custom-auth-filter
//                                final var newRequest = exchange
//                                        .getRequest()
//                                        .mutate()
//                                        .headers(h -> h.remove(HttpHeaders.AUTHORIZATION))
//                                        .build();
//                                final var newExchange = exchange
//                                        .mutate()
//                                        .request(newRequest)
//                                        .build();
                                final var securityContext = new SecurityContextImpl();
                                securityContext.setAuthentication(auth);
                                return chain
                                        .filter(exchange)
                                        .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));
                            }
                            return chain.filter(exchange);
                        }));
                            */
/*
                .doFinally(signal -> {                      // cleanup authentication after chain processing
                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(null);
                    SecurityContextHolder
                            .clearContext();
                });
*/

    }

    public static void clearCookie(
            final JwtProperties properties,
            final ServerWebExchange exchange) {
        
        exchange
                .getResponse()
                .beforeCommit(() -> Mono.defer(() -> {
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
                        return Mono.empty();
                    }));

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

    private String resolveToken(
            final ServerHttpResponse request) {

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
            final ServerWebExchange exchange) {

        String token = resolveToken(exchange.getRequest());
        if (!StringUtils.hasText(token)) {
            token = resolveToken(exchange.getResponse());
        }
        if (!StringUtils.hasText(token)) {
            return null;
        }

        final var jwt = buildJwt(token);

        final Collection<? extends GrantedAuthority> authorities;
        final var authoritiesClaims = jwt
                .getClaimAsStringList("authorities");
        if (authoritiesClaims != null) {
            authorities = authoritiesClaims
                    .stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        } else {
            authorities = List.of();
        }

        return new JwtAuthenticationToken(jwt, authorities);
                
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
