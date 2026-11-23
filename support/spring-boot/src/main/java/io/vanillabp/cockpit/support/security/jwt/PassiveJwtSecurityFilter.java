package io.vanillabp.cockpit.support.security.jwt;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Reads the login token from its cookie and makes the user known to Spring Security for this
 * request. It is passive: it never answers a request on its own. A request without a token, or
 * with a token which is not valid, goes on without a user, and the security rules decide what it
 * may do. So does a request with login cookies of two different users: the cookie of one user must
 * not be taken for a request which carries the cookie of another.
 */
public class PassiveJwtSecurityFilter extends OncePerRequestFilter {

  private final JwtProperties properties;

  private final JwtMapper<? extends JwtAuthenticationToken> jwtMapper;

  /**
   * Creates the filter.
   *
   * @param properties The token settings, the cookie name among them
   * @param jwtMapper  Turns the token into an authentication
   */
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

  /**
   * The token of the request. A request may carry the cookie more than once, and then the one user
   * the cookies agree on is taken, see {@link JwtMapper#oneLoginOf(List)}.
   */
  private String resolveToken(
      final HttpServletRequest request) {

    final var cookies = request.getCookies();
    if (cookies == null) {
      return null;
    }

    final var tokens = Arrays
        .stream(cookies)
        .filter(cookie -> cookie.getName().equals(properties.getCookie().getName()))
        .map(Cookie::getValue)
        .filter(StringUtils::hasText)
        .toList();
    return jwtMapper.oneLoginOf(tokens).orElse(null);

  }

}
