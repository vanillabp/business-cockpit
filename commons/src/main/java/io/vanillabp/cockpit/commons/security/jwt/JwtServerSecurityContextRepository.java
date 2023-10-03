package io.vanillabp.cockpit.commons.security.jwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import org.slf4j.Logger;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public abstract class JwtServerSecurityContextRepository implements ServerSecurityContextRepository {
    
    protected final JwtProperties properties;
    
    public JwtServerSecurityContextRepository(
            final JwtProperties properties) {

        this.properties = properties;
        
    }
    
    protected abstract Logger getLogger();
    
    @Override
    public Mono<Void> save(
            final ServerWebExchange exchange,
            final SecurityContext context) {

        try {

            final var expiresDuration = Duration
                    .parse(properties.getCookie().getExpiresDuration());
            final var expiresAt = Instant
                    .now()
                    .plus(expiresDuration)
                    .plusSeconds(10); // buffer
            
            final var token = buildToken(context, expiresAt);
            exchange
                    .getResponse()
                    .beforeCommit(() ->
                            Mono.defer(() -> {
                                exchange
                                        .getResponse()
                                        .addCookie(
                                                ResponseCookie
                                                        .from(properties.getCookie().getName())
                                                        .value(token)
                                                        .maxAge(expiresDuration)
                                                        .domain(properties.getCookie().getDomain())
                                                        .path(properties.getCookie().getPath())
                                                        .sameSite(getSecurityCookieSameSiteFromEnum())
                                                        .secure(properties.getCookie().isSecure())
                                                        .httpOnly(true)
                                                        .build());
                                return Mono.empty();
                            }));

            return Mono.empty();

        } catch (Exception e) {
            getLogger().error("Could not build JWT token", e);
            return Mono.error(e);
        }

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
            getLogger().warn("Got more than one cookie named '{}': {}. Will use the first!",
                    cookies,
                    properties.getCookie().getName());
        }
        
        final var token = cookies
                .get(0)
                .getValue();
        final var jwt = buildJwt(token);
        
        final var securityContext = SecurityContextHolder.createEmptyContext();
        
        return Mono.just(securityContext);

    }
    
    private String getSecurityCookieSameSiteFromEnum() {
        
        final var sameSite = properties.getCookie().getSameSite();
        if (sameSite == null) {
            return null;
        }
        return sameSite.attributeValue();
        
    }

    protected abstract JwtEncoderParameters getJwtEncoderParameters(
            final SecurityContext context,
            final Instant expiresAt);
    
    private String buildToken(
            final SecurityContext context,
            final Instant expiresAt) {

        final var jwk = new OctetSequenceKey
                .Builder(properties.getHmacSHA256())
                .keyID(UUID.randomUUID().toString())
                .algorithm(JWSAlgorithm.HS256)
                .build();

        final var encoder = new NimbusJwtEncoder(
                (jwkSelector, securityContext) -> List.of(jwk));
        
        return encoder
                .encode(getJwtEncoderParameters(context, expiresAt))
                .getTokenValue();
        
    }
    
    private Jwt buildJwt(
            final String token) {

        final var jwk = new OctetSequenceKey
                .Builder(properties.getHmacSHA256())
                .keyID(UUID.randomUUID().toString())
                .algorithm(JWSAlgorithm.HS256)
                .build();

        final var key = new SecretKeySpec(
                properties.getHmacSHA256(), "HMACSHA256");
        
        final var decoder = NimbusJwtDecoder
                .withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        
        return decoder.decode(token);
        
    }

}
