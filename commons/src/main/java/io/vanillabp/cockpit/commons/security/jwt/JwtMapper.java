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

    /**
     * When the user logged in, in seconds since the epoch. The name is the one OpenID Connect
     * uses for the same thing.
     */
    public static final String LOGIN_TIME_CLAIM = "auth_time";

    /**
     * Added to the lifetime of a new token, so a token is not judged expired by a clock which is a
     * few seconds ahead.
     */
    private static final Duration EXPIRY_BUFFER = Duration.ofSeconds(10);

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

    /**
     * The token of a new login. It lives {@code expires-duration}, but never longer than
     * {@code max-login-duration}, and it notes the time of the login in the claim
     * {@value #LOGIN_TIME_CLAIM}, which every renewal keeps.
     */
    public Optional<Map.Entry<String, Instant>> toToken(
            final SecurityContext context) {

        try {

            final var auth = context.getAuthentication();
            if (auth == null) {
                return Optional.empty();
            }

            final var now = Instant.now();
            final var expiresAt = earlierOf(
                    now
                            .plus(expiresDuration())
                            .plus(EXPIRY_BUFFER),
                    now.plus(maxLoginDuration()));

            final var claimsSetBuilder = JwtClaimsSet
                    .builder()
                    .expiresAt(expiresAt)
                    .id(UUID.randomUUID().toString())
                    .issuedAt(now)
                    .claim(LOGIN_TIME_CLAIM, now.getEpochSecond());

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

    /**
     * A new token for a login which is still in use, or empty if the token does not need one yet.
     * <p>
     * A token is renewed once less than half of {@code expires-duration} is left. The new token
     * carries the same claims, so the same user with the same groups, and it lives
     * {@code expires-duration} again. It never lives past the login time plus
     * {@code max-login-duration}. When that limit leaves no more time than the old token has, there
     * is nothing to renew, and the login ends when the old token expires.
     * <p>
     * The login time comes from the claim {@value #LOGIN_TIME_CLAIM}. A token without it was
     * issued before renewal existed, or by an application of its own, and was never renewed, so
     * its issue time is its login time.
     */
    public Optional<Map.Entry<String, Instant>> renew(
            final Jwt jwt) {

        final var loginAt = loginTimeOf(jwt);
        final var expiresAt = jwt.getExpiresAt();
        if (loginAt.isEmpty() || (expiresAt == null)) {
            return Optional.empty();
        }

        final var now = Instant.now();
        final var expiresDuration = expiresDuration();
        if (Duration.between(now, expiresAt).compareTo(expiresDuration.dividedBy(2)) >= 0) {
            return Optional.empty();
        }

        final var renewedExpiresAt = earlierOf(
                now
                        .plus(expiresDuration)
                        .plus(EXPIRY_BUFFER),
                loginAt
                        .get()
                        .plus(maxLoginDuration()));
        if (!renewedExpiresAt.isAfter(expiresAt)) {
            return Optional.empty();
        }

        try {

            final var claimsSet = JwtClaimsSet
                    .builder()
                    .claims(claims -> claims.putAll(jwt.getClaims()))
                    .expiresAt(renewedExpiresAt)
                    .id(UUID.randomUUID().toString())
                    .issuedAt(now)
                    .claim(LOGIN_TIME_CLAIM, loginAt.get().getEpochSecond())
                    .build();

            return Optional.of(Map.entry(
                    buildToken(claimsSet),
                    renewedExpiresAt));

        } catch (Exception e) {
            getLogger().error("Could not renew JWT token", e);
            return Optional.empty();
        }

    }

    private static Optional<Instant> loginTimeOf(
            final Jwt jwt) {

        final var loginTime = jwt.getClaims().get(LOGIN_TIME_CLAIM);
        if (loginTime instanceof Number seconds) {
            return Optional.of(Instant.ofEpochSecond(seconds.longValue()));
        }
        if (loginTime instanceof Instant instant) {
            return Optional.of(instant);
        }
        return Optional.ofNullable(jwt.getIssuedAt());

    }

    private static Instant earlierOf(
            final Instant first,
            final Instant second) {

        return first.isBefore(second) ? first : second;

    }

    private Duration expiresDuration() {

        return Duration.parse(properties.getCookie().getExpiresDuration());

    }

    private Duration maxLoginDuration() {

        return Duration.parse(properties.getCookie().getMaxLoginDuration());

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
