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
import org.springframework.security.core.context.DeferredSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Turns a successful authentication into the JWT cookie the single-page application sends on
 * every later request. It is wired into the HTTP basic configurer, so logging in once by basic
 * authentication is enough. The response to that first request carries the cookie.
 * <p>
 * In the cockpit's own filter chain the cookie is not read back here.
 * {@link PassiveJwtSecurityFilter} reads it, and that filter runs for every request, including
 * the ones no security filter chain protects. The basic authentication filter this repository is
 * handed to only writes. It never asks for a context.
 * <p>
 * Reading is still part of the contract, for an application which makes this repository the
 * context repository of its own chain. That happens in
 * {@link #loadDeferredContext(HttpServletRequest)}, which hands out a
 * {@link DeferredSecurityContext} and not a context. Spring Security resolves it only where
 * something asks who is logged in, so a request which never asks decodes no token. This is the
 * model {@code SecurityContextHolderFilter} works with. It replaced an older one, where a filter
 * wrapped request and response to notice a context being written.
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

    @Override
    public DeferredSecurityContext loadDeferredContext(
            final HttpServletRequest request) {

        return new DeferredCookieContext(request);

    }

    /**
     * The interface still declares this method, so a caller written against the old model keeps
     * working. It delegates the way Spring Security's own repositories delegate, and it is marked
     * deprecated so that nobody takes it for the place to change.
     *
     * @deprecated use {@link #loadDeferredContext(HttpServletRequest)}
     */
    @Deprecated
    @Override
    public SecurityContext loadContext(
            final HttpRequestResponseHolder requestResponseHolder) {

        return loadDeferredContext(requestResponseHolder.getRequest()).get();

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

    /**
     * Decodes the cookie when the context is first asked for and keeps the answer. A request
     * without the cookie gets an empty context and not null, which is what the interface asks
     * for, and {@link #isGenerated()} says which of the two a caller got. A token which is
     * expired, or signed with another key, lets the mapper's exception through where the context
     * is asked for. The deprecated read path let it through in the same place.
     * {@link PassiveJwtSecurityFilter} is what turns such a token into a request without a user.
     * See {@code JwtSecurityContextRepositoryTest}.
     */
    private final class DeferredCookieContext implements DeferredSecurityContext {

        private final HttpServletRequest request;

        private SecurityContext context;

        private boolean generated;

        private DeferredCookieContext(
                final HttpServletRequest request) {

            this.request = request;

        }

        @Override
        public SecurityContext get() {

            if (context == null) {
                context = readToken(request)
                        .map(jwtMapper::toSecurityContext)
                        .orElseGet(this::emptyContext);
            }
            return context;

        }

        @Override
        public boolean isGenerated() {

            get();
            return generated;

        }

        private SecurityContext emptyContext() {

            generated = true;
            return SecurityContextHolder.getContextHolderStrategy().createEmptyContext();

        }

    }

}
