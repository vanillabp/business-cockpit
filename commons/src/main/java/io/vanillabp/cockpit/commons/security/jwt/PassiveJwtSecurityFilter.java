package io.vanillabp.cockpit.commons.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class PassiveJwtSecurityFilter extends OncePerRequestFilter {

    private final JwtProperties properties;

    private final JwtMapper<? extends JwtAuthenticationToken> jwtMapper;

    public PassiveJwtSecurityFilter(
            final JwtProperties properties,
            final JwtMapper<? extends JwtAuthenticationToken> jwtMapper) {

        this.properties = properties;
        this.jwtMapper = jwtMapper;

    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {

        try {

            final var token = resolveToken(request);
            if (StringUtils.hasText(token)) {
                final var auth = jwtMapper.toAuth(token);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }

            filterChain.doFilter(request, response);

        } catch (JwtException e) {

            if (!(e instanceof JwtValidationException)) {
                logger.warn("JWT-error", e);
            } else {
                logger.debug("JWT-Validation", e);
            }

            filterChain.doFilter(request, response);

        } finally {

            SecurityContextHolder.getContext().setAuthentication(null);
            SecurityContextHolder.clearContext();

        }

    }

    private String resolveToken(
            final HttpServletRequest request) {

        final var cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (final Cookie cookie : cookies) {
            if (cookie.getName().equals(properties.getCookie().getName())) {
                return cookie.getValue();
            }
        }

        return null;

    }

}
