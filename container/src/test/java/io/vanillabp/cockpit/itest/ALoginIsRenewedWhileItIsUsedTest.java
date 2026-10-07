package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Somebody who works with the cockpit is not logged out after the twelve hours a token lives. A
 * request in the second half of a token's lifetime gets a new cookie, up to the maximum of seven
 * days counted from the login. The update stream does not count as working, and an expired token
 * stays expired.
 * <p>
 * The tokens are signed here with the key of the 'local' profile and dated into the past, because
 * a login made by the test is always fresh.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class ALoginIsRenewedWhileItIsUsedTest extends ItestBase {

    private static final Duration EXPIRES = Duration.ofHours(12);

    private static final Duration MAX_LOGIN = Duration.ofDays(7);

    @Value("${business-cockpit.jwt.hmacSHA256-base64}")
    private String signingKey;

    @Test
    void aFreshLoginIsNotRenewed() {

        final var response = currentUser(loginToGui(USER_MARTIN));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(renewedCookieOf(response)).isEmpty();

    }

    @Test
    void aRequestInTheSecondHalfOfTheTokenGetsANewOne() {

        final var loginAt = Instant.now().minus(Duration.ofHours(7));
        final var response = currentUser(cookieOf(token(loginAt, loginAt, loginAt.plus(EXPIRES))));

        assertThat(response.statusCode()).isEqualTo(200);
        final var renewed = renewedCookieOf(response);
        assertThat(renewed).as("the response carries a new cookie").isPresent();
        final var claims = claimsOf(renewed.get());
        assertThat(Instant.ofEpochSecond(claims.read("$.exp", Long.class)))
                .as("the new token lives twelve hours from now")
                .isAfter(Instant.now().plus(EXPIRES).minusSeconds(30));
        assertThat(claims.read("$.auth_time", Long.class))
                .as("the time of the login stays")
                .isEqualTo(loginAt.getEpochSecond());

        final var withTheNewCookie = currentUser(renewed.get());
        assertThat(withTheNewCookie.statusCode()).isEqualTo(200);
        assertThat(JsonPath.parse(withTheNewCookie.body()).read("$.id", String.class))
                .as("the new cookie is the same user")
                .isEqualTo(USER_MARTIN);

    }

    @Test
    void aRenewalStopsAtTheMaximumCountedFromTheLogin() {

        final var loginAt = Instant.now().minus(MAX_LOGIN).plus(Duration.ofHours(2));
        final var response = currentUser(cookieOf(token(
                loginAt, Instant.now().minus(Duration.ofHours(10)), Instant.now().plus(Duration.ofHours(1)))));

        final var renewed = renewedCookieOf(response);
        assertThat(renewed).isPresent();
        assertThat(claimsOf(renewed.get()).read("$.exp", Long.class))
                .as("two hours were left of the login, not twelve")
                .isEqualTo(loginAt.plus(MAX_LOGIN).getEpochSecond());

    }

    @Test
    void aLoginWhichReachedTheMaximumIsNotRenewed() {

        final var loginAt = Instant.now().minus(MAX_LOGIN).plus(Duration.ofMinutes(30));
        final var response = currentUser(cookieOf(token(
                loginAt, Instant.now().minus(Duration.ofHours(11)), loginAt.plus(MAX_LOGIN))));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(renewedCookieOf(response)).isEmpty();

    }

    @Test
    void anExpiredTokenIsNotRenewed() {

        final var loginAt = Instant.now().minus(Duration.ofHours(13));
        final var cookie = cookieOf(token(loginAt, loginAt, loginAt.plus(EXPIRES)));

        final var currentUser = currentUser(cookie);
        assertThat(currentUser.body())
                .as("an expired token is nobody")
                .doesNotContain(USER_MARTIN);
        assertThat(renewedCookieOf(currentUser)).isEmpty();

        final var updates = openUpdates(cookie);
        assertThat(updates.statusCode()).isEqualTo(401);
        assertThat(renewedCookieOf(updates)).isEmpty();

    }

    @Test
    void theUpdateStreamDoesNotRenewTheToken() throws IOException {

        final var loginAt = Instant.now().minus(Duration.ofHours(7));
        final var response = openUpdates(cookieOf(token(loginAt, loginAt, loginAt.plus(EXPIRES))));

        try (var body = response.body()) {
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(renewedCookieOf(response))
                    .as("a tab nobody looks at must not stay logged in")
                    .isEmpty();
        }

    }

    private HttpResponse<String> currentUser(
            final String cookie) {

        return guiGet(cookie, "/app/current-user");

    }

    /**
     * Returns once the headers are there. The stream itself never ends, so its body is closed by
     * the caller.
     */
    private HttpResponse<InputStream> openUpdates(
            final String cookie) {

        try {
            return HTTP.send(HttpRequest
                    .newBuilder(URI.create(url("/gui/api/v1/updates")))
                    .header("Cookie", cookie)
                    .header("Accept", "text/event-stream")
                    .GET()
                    .build(), HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException e) {
            throw new IllegalStateException("HTTP request failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("HTTP request interrupted", e);
        }

    }

    /**
     * The "bc=..." pair of a cookie the response sets, unless it only drops the cookie.
     */
    private static Optional<String> renewedCookieOf(
            final HttpResponse<?> response) {

        return response
                .headers()
                .allValues("set-cookie")
                .stream()
                .filter(cookie -> cookie.startsWith("bc="))
                .map(cookie -> cookie.substring(0, cookie.indexOf(';')))
                .filter(cookie -> !cookie.equals("bc="))
                .findFirst();

    }

    private static String cookieOf(
            final String token) {

        return "bc=" + token;

    }

    private static DocumentContext claimsOf(
            final String cookie) {

        final var payload = cookie.substring("bc=".length()).split("\\.")[1];
        return JsonPath.parse(new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8));

    }

    /**
     * A token the way the cockpit writes it at a login, but dated into the past.
     */
    private String token(
            final Instant loginAt,
            final Instant issuedAt,
            final Instant expiresAt) {

        final var claims = JwtClaimsSet
                .builder()
                .issuer("bc")
                .subject(USER_MARTIN)
                .audience(List.of("bc"))
                .id(UUID.randomUUID().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("auth_time", loginAt.getEpochSecond())
                .claim("authorities", List.of(GROUP_OF_MARTIN, "bc-users"))
                .build();

        final var jwk = new OctetSequenceKey
                .Builder(Base64.getDecoder().decode(signingKey))
                .keyID(UUID.randomUUID().toString())
                .algorithm(JWSAlgorithm.HS256)
                .build();

        return new NimbusJwtEncoder((jwkSelector, context) -> List.of(jwk))
                .encode(JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(),
                        claims))
                .getTokenValue();

    }

}
