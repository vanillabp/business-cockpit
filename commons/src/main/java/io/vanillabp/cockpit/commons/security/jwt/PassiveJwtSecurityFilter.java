package io.vanillabp.cockpit.commons.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class PassiveJwtSecurityFilter extends OncePerRequestFilter {

    private final JwtProperties properties;

    public PassiveJwtSecurityFilter(
            final JwtProperties properties) {

        this.properties = properties;

    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {

        try {

            final var token = resolveToken(request);
            if (StringUtils.hasText(token)) {
                final var auth = processJwtToken(token, request, response, filterChain);
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

    private JwtAuthenticationToken processJwtToken(
            final String token,
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain) throws IOException {

        final var jwt = buildJwt(token);

        final Collection<? extends GrantedAuthority> authorities;
        final var authoritiesClaims = jwt
                .getClaimAsStringList("authorities");
        if (authoritiesClaims != null) {
            authorities = authoritiesClaims
                    .stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        } else {
            authorities = List.of();
        }

        return new JwtAuthenticationToken(jwt, authorities);

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
