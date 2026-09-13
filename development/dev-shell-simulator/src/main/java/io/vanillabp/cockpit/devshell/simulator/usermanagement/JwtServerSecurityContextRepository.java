package io.vanillabp.cockpit.devshell.simulator.usermanagement;

import io.vanillabp.cockpit.commons.security.jwt.JwtMapper;
import io.vanillabp.cockpit.commons.security.jwt.JwtProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.DeferredSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Holds the user the dev shell switched to in a JWT cookie: written when the switch happens, read
 * back on every request after it.
 *
 * <p>Reading is done by {@link #loadDeferredContext(HttpServletRequest)}, which hands out a
 * {@link DeferredSecurityContext} rather than a context. Spring Security resolves that only where
 * something asks who is logged in, so a request which never asks does not decode a token. This is
 * the model {@code SecurityContextHolderFilter} works with, and it replaced the older one where a
 * filter wrapped request and response to notice a context being written.
 */
public class JwtServerSecurityContextRepository implements SecurityContextRepository {

    private final JwtProperties properties;

    private final JwtMapper<? extends AbstractAuthenticationToken> jwtMapper;

    public JwtServerSecurityContextRepository(
            JwtProperties properties, JwtMapper<? extends AbstractAuthenticationToken> jwtMapper) {

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
                .ifPresent(
                        token -> {
                            final var cookie = new Cookie(properties.getCookie().getName(), token.getKey());
                            final var maxAge =
                                    (int)
                                            Duration.parse(this.properties.getCookie().getExpiresDuration()).toSeconds();
                            cookie.setMaxAge(maxAge);
                            cookie.setPath(properties.getCookie().getPath());
                            cookie.setDomain(properties.getCookie().getDomain());
                            cookie.setSecure(properties.getCookie().isSecure());
                            cookie.setHttpOnly(true);
                            final var sameSite = getSecurityCookieSameSiteFromEnum();
                            if (sameSite != null) {
                                cookie.setAttribute("SameSite", sameSite);
                            }
                            response.addCookie(cookie);
                        });

    }

    @Override
    public boolean containsContext(
            final HttpServletRequest request) {

        return readToken(request).isPresent();

    }

    private Optional<String> readToken(
            final HttpServletRequest request) {

        final var cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> cookie.getName().equals(properties.getCookie().getName()))
                .findFirst()
                .map(Cookie::getValue);

    }

    private String getSecurityCookieSameSiteFromEnum() {

        final var sameSite = this.properties.getCookie().getSameSite();
        return sameSite == null ? null : sameSite.attributeValue();

    }

    /**
     * Decodes the cookie when the context is first asked for and keeps the answer. A request
     * without the cookie gets an empty context rather than null, which is what the interface asks
     * for, and {@link #isGenerated()} says which of the two a caller got.
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
                context =
                        readToken(request)
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
