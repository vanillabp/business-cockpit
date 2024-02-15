package io.vanillabp.cockpit.commons.security.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

public class JwtSecurityWebFilter implements WebFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtSecurityWebFilter.class);
    
    private final JwtProperties properties;

    private final List<ServerWebExchangeMatcher> unprotectedExchangeMatchers;

    private final JwtServerSecurityContextRepository jwtSecurityContextRepository;
    
    public JwtSecurityWebFilter(
            final JwtProperties properties,
            final JwtServerSecurityContextRepository jwtSecurityContextRepository,
            final ServerWebExchangeMatcher... unprotectedExchangeMatchers) {

        this.properties = properties;
        this.jwtSecurityContextRepository = jwtSecurityContextRepository;
        this.unprotectedExchangeMatchers = Arrays.asList(unprotectedExchangeMatchers);

    }

    @Override
    public Mono<Void> filter(
            final ServerWebExchange exchange,
            final WebFilterChain chain) {

        try {
            final var securityContext = jwtSecurityContextRepository.load(exchange);
            return securityContext
                    .hasElement()
                    .flatMap(hasSecurityContext -> hasSecurityContext
                            ? chain.filter(exchange)
                                    .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(securityContext))
                            : chain.filter(exchange));
        } catch (Exception e) {
            logger.error("Cannot process JWT token", e);
            clearCookie(properties, exchange);
            return chain.filter(exchange);
        }

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

}
