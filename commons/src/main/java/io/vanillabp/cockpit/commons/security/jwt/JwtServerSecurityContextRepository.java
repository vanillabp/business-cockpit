package io.vanillabp.cockpit.commons.security.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class JwtServerSecurityContextRepository implements ServerSecurityContextRepository {

    private static final Logger logger = LoggerFactory.getLogger(JwtServerSecurityContextRepository.class);

    private final JwtProperties properties;
    private final JwtMapper<? extends AbstractAuthenticationToken> jwtMapper;
    
    public JwtServerSecurityContextRepository(
            final JwtProperties properties,
            final JwtMapper<? extends AbstractAuthenticationToken> jwtMapper) {

        this.properties = properties;
        this.jwtMapper = jwtMapper;
        
    }

    @Override
    public Mono<Void> save(
            final ServerWebExchange exchange,
            final SecurityContext context) {

        if (!exchange
                .getRequest()
                .getCookies()
                .getOrDefault(properties.getCookie().getName(), List.of())
                .isEmpty()) {
            return Mono.empty();
        }

        jwtMapper
                .toToken(context)
                .ifPresent(token -> exchange
                    .getResponse()
                    .beforeCommit(() ->
                            Mono.defer(() -> {
                                exchange
                                        .getResponse()
                                        .addCookie(
                                                ResponseCookie
                                                        .from(properties.getCookie().getName())
                                                        .value(token.getKey())
                                                        .maxAge(Duration.between(Instant.now(), token.getValue()))
                                                        .domain(properties.getCookie().getDomain())
                                                        .path(properties.getCookie().getPath())
                                                        .sameSite(getSecurityCookieSameSiteFromEnum())
                                                        .secure(properties.getCookie().isSecure())
                                                        .httpOnly(true)
                                                        .build());
                                return Mono.empty();
                            })));

        return Mono.empty();

    }

    @Override
    public Mono<SecurityContext> load(
            final ServerWebExchange exchange) {

        final var cookies = exchange
                .getRequest()
                .getCookies()
                .get(properties.getCookie().getName());
        if ((cookies == null) || cookies.isEmpty()) {
            return Mono.empty();
        }
        if (cookies.size() > 1) {
            logger.warn("Got more than one cookie named '{}': {}. Will use the first!",
                    cookies,
                    properties.getCookie().getName());
        }
        
        final var token = cookies
                .get(0)
                .getValue();
        final var securityContext = jwtMapper.toSecurityContext(token);

        return Mono.just(securityContext);

    }
    
    private String getSecurityCookieSameSiteFromEnum() {
        
        final var sameSite = properties.getCookie().getSameSite();
        if (sameSite == null) {
            return null;
        }
        return sameSite.attributeValue();
        
    }

}
