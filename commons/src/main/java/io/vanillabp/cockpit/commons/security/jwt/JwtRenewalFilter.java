package io.vanillabp.cockpit.commons.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Keeps a login alive while it is used. A request whose token has less than half of its lifetime
 * left gets a new cookie with a new token, see {@link JwtMapper#renew}. The browser sends the new
 * cookie from the next request on.
 * <p>
 * It has to run after {@link PassiveJwtSecurityFilter}, which turns the cookie into the
 * authentication this filter reads. A request without a valid token is passed on unchanged, so an
 * expired login stays expired.
 * <p>
 * Some requests are not a sign that somebody uses the application. The update stream of a tab is
 * one: it connects again by itself, and if it renewed the token, a tab nobody looks at would stay
 * logged in for as long as {@code max-login-duration} allows. Such requests are named by the
 * matcher passed in, and they keep their token as it is.
 */
public class JwtRenewalFilter extends OncePerRequestFilter {

    private final JwtProperties properties;

    private final JwtMapper<? extends JwtAuthenticationToken> jwtMapper;

    private final RequestMatcher requestsWhichDoNotRenew;

    public JwtRenewalFilter(
            final JwtProperties properties,
            final JwtMapper<? extends JwtAuthenticationToken> jwtMapper,
            final RequestMatcher requestsWhichDoNotRenew) {

        this.properties = properties;
        this.jwtMapper = jwtMapper;
        this.requestsWhichDoNotRenew = requestsWhichDoNotRenew;

    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {

        // the cookie is a header, so it has to be added before the response is committed
        if (!requestsWhichDoNotRenew.matches(request)
                && (SecurityContextHolder.getContext().getAuthentication()
                        instanceof JwtAuthenticationToken authentication)) {
            jwtMapper
                    .renew(authentication.getJwt())
                    .ifPresent(token -> JwtSecurityContextRepository.addCookie(
                            properties, response, token.getKey(), token.getValue()));
        }

        filterChain.doFilter(request, response);

    }

}
