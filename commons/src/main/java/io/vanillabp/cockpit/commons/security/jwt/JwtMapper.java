package io.vanillabp.cockpit.commons.security.jwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import org.slf4j.Logger;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;

public abstract class JwtMapper<D extends AbstractAuthenticationToken> {

    public static final String AUTHORITIES_CLAIM = "authorities";

    private final JwtProperties properties;

    public JwtMapper(
            final JwtProperties properties) {

        this.properties = properties;

    }

    protected abstract Logger getLogger();

    protected abstract D buildAuthenticationToken(
            final Jwt jwt,
            final Collection<GrantedAuthority> bcAuthorities);

    public D toAuth(
            final String token) {

        if (token == null) {
            return null;
        }
        final var jwt = buildJwt(token);

        final var authorities = new HashSet<GrantedAuthority>();
        authorities.add(new SimpleGrantedAuthority(JwtUserDetails.USER_AUTHORITY_PREFIX + jwt.getSubject()));

        return buildAuthenticationToken(jwt, authorities);

    }

    public SecurityContext toSecurityContext(
            final String token) {

        final var auth = toAuth(token);
        final var securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(auth);
        return securityContext;

    }

    protected abstract void applyJwtClaimsSet(
            final JwtClaimsSet.Builder claimsSetBuilder,
            final SecurityContext context);

    public Optional<Map.Entry<String, Instant>> toToken(
            final SecurityContext context) {

        try {

            final var auth = context.getAuthentication();
            if (auth == null) {
                return Optional.empty();
            }

            final var expiresDuration = Duration
                    .parse(properties.getCookie().getExpiresDuration());
            final var expiresAt = Instant
                    .now()
                    .plus(expiresDuration)
                    .plusSeconds(10); // buffer

            final var claimsSetBuilder = JwtClaimsSet
                    .builder()
                    .expiresAt(expiresAt)
                    .id(UUID.randomUUID().toString())
                    .issuedAt(Instant.now());

            applyJwtClaimsSet(
                    claimsSetBuilder,
                    context);

            final var claimsSet = claimsSetBuilder.build();

            return Optional.of(Map.entry(
                    buildToken(claimsSet),
                    expiresAt));

        } catch (Exception e) {
            getLogger().error("Could not build JWT token", e);
            return Optional.empty();
        }

    }

    private String buildToken(
            final JwtClaimsSet claimsSet) {

        final var jwk = new OctetSequenceKey
                .Builder(properties.getHmacSHA256())
                .keyID(UUID.randomUUID().toString())
                .algorithm(JWSAlgorithm.HS256)
                .build();

        final var encoder = new NimbusJwtEncoder(
                (jwkSelector, securityContext) -> List.of(jwk));

        final var parameters = JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),
                claimsSet);

        return encoder
                .encode(parameters)
                .getTokenValue();

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
