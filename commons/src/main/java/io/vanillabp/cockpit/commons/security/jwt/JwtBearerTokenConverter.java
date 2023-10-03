package io.vanillabp.cockpit.commons.security.jwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.spec.SecretKeySpec;
import java.util.UUID;

public class JwtBearerTokenConverter implements ServerAuthenticationConverter {

    private static final Logger logger = LoggerFactory.getLogger(JwtBearerTokenConverter.class);
    
    protected final JwtProperties properties;
    
    public JwtBearerTokenConverter(
            final JwtProperties properties) {

        this.properties = properties;
        
    }

    @Override
    public Mono<Authentication> convert(
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
        final var jwt = buildJwt(token);
        
        final var securityContext = SecurityContextHolder.createEmptyContext();

        return Mono.just(securityContext.getAuthentication());
        
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
