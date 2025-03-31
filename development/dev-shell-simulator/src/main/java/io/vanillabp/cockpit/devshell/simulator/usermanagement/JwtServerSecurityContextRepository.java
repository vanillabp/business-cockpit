package io.vanillabp.cockpit.devshell.simulator.usermanagement;

import io.vanillabp.cockpit.commons.security.jwt.JwtMapper;
import io.vanillabp.cockpit.commons.security.jwt.JwtProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.SecurityContextRepository;

import java.time.Duration;
import java.util.Arrays;

public class JwtServerSecurityContextRepository implements SecurityContextRepository {

  private final JwtProperties properties;

  private final JwtMapper<? extends AbstractAuthenticationToken> jwtMapper;

  public JwtServerSecurityContextRepository(
      JwtProperties properties, JwtMapper<? extends AbstractAuthenticationToken> jwtMapper) {
    this.properties = properties;
    this.jwtMapper = jwtMapper;
  }

  @Override
  public SecurityContext loadContext(final HttpRequestResponseHolder requestResponseHolder) {
    var cookies = requestResponseHolder.getRequest().getCookies();
    if (cookies == null) {
      return null;
    }
    return Arrays.stream(cookies)
        .filter(c -> c.getName().equals(properties.getCookie().getName()))
        .findFirst()
        .map(Cookie::getValue)
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
  public boolean containsContext(final HttpServletRequest request) {
    return Arrays.stream(request.getCookies())
        .filter(c -> c.getName().equals(properties.getCookie().getName()))
        .findFirst()
        .isEmpty();
  }

  private String getSecurityCookieSameSiteFromEnum() {
    final var sameSite = this.properties.getCookie().getSameSite();
    return sameSite == null ? null : sameSite.attributeValue();
  }
}
