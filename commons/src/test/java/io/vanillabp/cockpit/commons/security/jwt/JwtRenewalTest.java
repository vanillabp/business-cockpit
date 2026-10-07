package io.vanillabp.cockpit.commons.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.crypto.KeyGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * A login which is used stays alive, up to its maximum. A token is renewed in the second half of
 * its lifetime, with the same claims and a lifetime which never goes past the login time plus
 * {@code max-login-duration}.
 */
@ExtendWith(SuppressOutputExtension.class)
class JwtRenewalTest {

    private static final Duration EXPIRES = Duration.ofHours(12);

    private static final Duration MAX_LOGIN = Duration.ofDays(7);

    /** The ten seconds a new token gets on top of its lifetime. */
    private static final Duration BUFFER = Duration.ofSeconds(10);

    private JwtProperties properties;

    private JwtAuthenticationTokenMapper mapper;

    @BeforeEach
    void setUp() throws Exception {

        properties = new JwtProperties();
        properties.setHmacSHA256Base64(Base64
                .getEncoder()
                .encodeToString(KeyGenerator.getInstance("HmacSha256").generateKey().getEncoded()));
        properties.getCookie().setExpiresDuration(EXPIRES.toString());
        properties.getCookie().setMaxLoginDuration(MAX_LOGIN.toString());
        mapper = new JwtAuthenticationTokenMapper(properties);

    }

    @Test
    void aNewLoginNotesWhenItWasMade() {

        final var before = Instant.now().minusSeconds(1);
        final var jwt = decode(login());

        assertThat(loginTimeOf(jwt)).isBetween(before, Instant.now());
        assertThat(jwt.getExpiresAt()).isBetween(
                Instant.now().plus(EXPIRES).minusSeconds(5),
                Instant.now().plus(EXPIRES).plus(BUFFER));

    }

    @Test
    void aNewLoginLivesNoLongerThanTheMaximum() {

        properties.getCookie().setMaxLoginDuration("PT1H");

        final var jwt = decode(login());

        assertThat(jwt.getExpiresAt())
                .as("the first token is cut to the maximum, too")
                .isBefore(Instant.now().plus(Duration.ofHours(1)).plusSeconds(1));

    }

    @Test
    void aTokenInTheFirstHalfOfItsLifetimeIsNotRenewed() {

        assertThat(mapper.renew(decode(login()))).isEmpty();

    }

    @Test
    void aTokenInTheSecondHalfOfItsLifetimeGetsTheFullLifetimeAgain() {

        final var loginAt = Instant.now().minus(Duration.ofHours(7));
        final var old = decode(token(loginAt, loginAt, loginAt.plus(EXPIRES)));

        final var renewed = mapper.renew(old);

        assertThat(renewed).isPresent();
        final var jwt = decode(renewed.get().getKey());
        assertThat(jwt.getExpiresAt())
                .isEqualTo(renewed.get().getValue().truncatedTo(ChronoUnit.SECONDS))
                .isBetween(Instant.now().plus(EXPIRES).minusSeconds(5), Instant.now().plus(EXPIRES).plus(BUFFER));
        assertThat(jwt.getId()).isNotEqualTo(old.getId());
        assertThat(loginTimeOf(jwt))
                .as("a renewal keeps the time of the login, which the maximum counts from")
                .isEqualTo(loginAt.truncatedTo(ChronoUnit.SECONDS));

    }

    @Test
    void aRenewedTokenCarriesTheSameUserAndGroups() {

        final var loginAt = Instant.now().minus(Duration.ofHours(7));
        final var old = decode(token(loginAt, loginAt, loginAt.plus(EXPIRES)));

        final var jwt = decode(mapper.renew(old).orElseThrow().getKey());

        assertThat(jwt.getSubject()).isEqualTo("john");
        assertThat(jwt.getAudience()).containsExactly("bc");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("bc");
        assertThat(jwt.getClaimAsStringList(JwtMapper.AUTHORITIES_CLAIM)).containsExactly("ADMIN");
        assertThat(mapper.toAuth(mapper.renew(old).orElseThrow().getKey()).getAuthorities())
                .extracting(Object::toString)
                .contains("ADMIN", "USER_john");

    }

    @Test
    void aRenewalEndsAtTheMaximumCountedFromTheLogin() {

        final var loginAt = Instant.now().minus(MAX_LOGIN).plus(Duration.ofHours(2));
        final var old = decode(token(loginAt, Instant.now().minus(Duration.ofHours(9)),
                Instant.now().plus(Duration.ofHours(1))));

        final var renewed = mapper.renew(old);

        assertThat(renewed).isPresent();
        assertThat(decode(renewed.get().getKey()).getExpiresAt())
                .as("two hours were left of the login, not twelve")
                .isEqualTo(loginAt.plus(MAX_LOGIN).truncatedTo(ChronoUnit.SECONDS));

    }

    @Test
    void aTokenWhichAlreadyReachesTheMaximumIsNotRenewed() {

        final var loginAt = Instant.now().minus(MAX_LOGIN).plus(Duration.ofMinutes(30));
        final var old = decode(token(loginAt, Instant.now().minus(Duration.ofHours(11)),
                loginAt.plus(MAX_LOGIN)));

        assertThat(mapper.renew(old))
                .as("a new token would expire no later than the old one")
                .isEmpty();

    }

    @Test
    void aTokenWithoutALoginTimeCountsFromWhenItWasIssued() {

        final var issuedAt = Instant.now().minus(Duration.ofHours(7));
        final var old = decode(token(null, issuedAt, issuedAt.plus(EXPIRES)));

        final var jwt = decode(mapper.renew(old).orElseThrow().getKey());

        assertThat(loginTimeOf(jwt))
                .as("such a token was never renewed, so it was issued at the login")
                .isEqualTo(issuedAt.truncatedTo(ChronoUnit.SECONDS));

    }

    @Test
    void theFilterWritesTheRenewedTokenIntoTheCookie() throws Exception {

        final var loginAt = Instant.now().minus(Duration.ofHours(7));
        final var old = decode(token(loginAt, loginAt, loginAt.plus(EXPIRES)));

        final var response = filter(new MockHttpServletRequest("GET", "/gui/api/v1/app/current-user"),
                new JwtAuthenticationToken(old, List.of()));

        final var cookie = response.getCookie(properties.getCookie().getName());
        assertThat(cookie).isNotNull();
        assertThat(decode(cookie.getValue()).getSubject()).isEqualTo("john");
        assertThat(cookie.getMaxAge())
                .as("the cookie lives as long as the token in it")
                .isBetween((int) EXPIRES.toSeconds() - 5, (int) EXPIRES.plus(BUFFER).toSeconds());

    }

    @Test
    void theFilterLeavesAnExcludedRequestAlone() throws Exception {

        final var loginAt = Instant.now().minus(Duration.ofHours(7));
        final var old = decode(token(loginAt, loginAt, loginAt.plus(EXPIRES)));

        final var response = filter(new MockHttpServletRequest("GET", "/gui/api/v1/updates"),
                new JwtAuthenticationToken(old, List.of()));

        assertThat(response.getCookie(properties.getCookie().getName())).isNull();

    }

    @Test
    void theFilterLeavesARequestWithoutATokenAlone() throws Exception {

        final var authorities = List.of(new SimpleGrantedAuthority("ADMIN"));
        final var response = filter(new MockHttpServletRequest("GET", "/gui/api/v1/app/current-user"),
                new UsernamePasswordAuthenticationToken(new User("john", "", authorities), "", authorities));

        assertThat(response.getCookie(properties.getCookie().getName())).isNull();

    }

    private MockHttpServletResponse filter(
            final MockHttpServletRequest request,
            final Authentication authentication) throws Exception {

        final var filter = new JwtRenewalFilter(
                properties, mapper, PathPatternRequestMatcher.pathPattern("/gui/api/v1/updates"));
        final var response = new MockHttpServletResponse();
        SecurityContextHolder.setContext(
                new SecurityContextImpl(authentication));
        try {
            filter.doFilter(request, response, new MockFilterChain());
        } finally {
            SecurityContextHolder.clearContext();
        }
        return response;

    }

    private String login() {

        final var authorities = List.of(new SimpleGrantedAuthority("ADMIN"));
        final var context = new SecurityContextImpl(new UsernamePasswordAuthenticationToken(
                new User("john", "", authorities), "", authorities));
        return mapper.toToken(context).orElseThrow().getKey();

    }

    private Jwt decode(
            final String token) {

        return mapper.toAuth(token).getJwt();

    }

    private static Instant loginTimeOf(
            final Jwt jwt) {

        return Instant.ofEpochSecond(((Number) jwt.getClaims().get(JwtMapper.LOGIN_TIME_CLAIM)).longValue());

    }

    /**
     * A token the way a login some time ago wrote it. The mapper always dates a new token from now
     * on, so the test signs this one itself with the same key.
     */
    private String token(
            final Instant loginAt,
            final Instant issuedAt,
            final Instant expiresAt) {

        final var claims = JwtClaimsSet
                .builder()
                .issuer("bc")
                .subject("john")
                .audience(List.of("bc"))
                .id(UUID.randomUUID().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim(JwtMapper.AUTHORITIES_CLAIM, List.of("ADMIN"));
        if (loginAt != null) {
            claims.claim(JwtMapper.LOGIN_TIME_CLAIM, loginAt.getEpochSecond());
        }

        final var jwk = new OctetSequenceKey
                .Builder(properties.getHmacSHA256())
                .keyID(UUID.randomUUID().toString())
                .algorithm(JWSAlgorithm.HS256)
                .build();

        return new NimbusJwtEncoder((jwkSelector, context) -> List.of(jwk))
                .encode(JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(),
                        claims.build()))
                .getTokenValue();

    }

}
