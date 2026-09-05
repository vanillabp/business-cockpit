package io.vanillabp.cockpit.commons.security.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Turns a successful authentication into the JWT cookie the single-page application sends on all
 * subsequent requests. Wired into the HTTP basic configurer, so logging in once by basic auth is
 * enough: the response of that first request carries the cookie.
 * <p>
 * Reading the cookie back is not done here but by {@link PassiveJwtSecurityFilter}, which runs for
 * every request including the ones no security filter chain protects.
 */
public class JwtSecurityContextRepository implements SecurityContextRepository {

    private static final Logger logger = LoggerFactory.getLogger(JwtSecurityContextRepository.class);

    private final JwtProperties properties;

    private final JwtMapper<? extends AbstractAuthenticationToken> jwtMapper;

    public JwtSecurityContextRepository(
            final JwtProperties properties,
            final JwtMapper<? extends AbstractAuthenticationToken> jwtMapper) {

        this.properties = properties;
        this.jwtMapper = jwtMapper;

    }

    /**
     * Spring Security deprecated this method in favour of {@code loadDeferredContext}, but left it
     * as the one abstract method of the interface, and its default {@code loadDeferredContext}
     * delegates here. So this is where reading has to happen, deprecated or not.
     */
    @Deprecated
    @Override
    public SecurityContext loadContext(
            final HttpRequestResponseHolder requestResponseHolder) {

        return readToken(requestResponseHolder.getRequest())
                .map(jwtMapper::toSecurityContext)
                .orElse(null);

    }

    @Override
    public void saveContext(
            final SecurityContext context,
            final HttpServletRequest request,
            final HttpServletResponse response) {

        jwtMapper
                .toToken(context)
                .ifPresent(token -> response.addCookie(
                        buildCookie(token.getKey(), Duration.between(Instant.now(), token.getValue()))));

    }

    @Override
    public boolean containsContext(
            final HttpServletRequest request) {

        return readToken(request).isPresent();

    }

    /**
     * Overwrites the JWT cookie with an already expired one, which makes the browser drop it. Used
     * on logout and whenever a token turns out to be unusable.
     */
    public static void clearCookie(
            final JwtProperties properties,
            final HttpServletResponse response) {

        final var cookie = new Cookie(properties.getCookie().getName(), "");
        cookie.setMaxAge(0);
        cookie.setPath(properties.getCookie().getPath());
        if (properties.getCookie().getDomain() != null) {
            cookie.setDomain(properties.getCookie().getDomain());
        }
        cookie.setSecure(properties.getCookie().isSecure());
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

    }

    private Cookie buildCookie(
            final String token,
            final Duration maxAge) {

        final var cookie = new Cookie(properties.getCookie().getName(), token);
        cookie.setMaxAge((int) maxAge.toSeconds());
        cookie.setPath(properties.getCookie().getPath());
        if (properties.getCookie().getDomain() != null) {
            cookie.setDomain(properties.getCookie().getDomain());
        }
        cookie.setSecure(properties.getCookie().isSecure());
        cookie.setHttpOnly(true);
        final var sameSite = properties.getCookie().getSameSite();
        if (sameSite != null) {
            cookie.setAttribute("SameSite", sameSite.attributeValue());
        }
        return cookie;

    }

    private Optional<String> readToken(
            final HttpServletRequest request) {

        final var cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        final var matching = Arrays
                .stream(cookies)
                .filter(cookie -> cookie.getName().equals(properties.getCookie().getName()))
                .toList();
        if (matching.isEmpty()) {
            return Optional.empty();
        }
        if (matching.size() > 1) {
            logger.warn("Got more than one cookie named '{}'. Will use the first!",
                    properties.getCookie().getName());
        }
        return Optional.of(matching.get(0).getValue());

    }

}
