package io.vanillabp.cockpit.config.web;

import java.io.IOException;

import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Every answer of the GUI API carries a Content Security Policy which allows nothing: no script,
 * no style, no image, and no frame around it. The GUI API answers JSON and plain text, which a
 * browser never runs, so it loses nothing.
 * <p>
 * The filter is registered for {@code /gui/api/*} only. The user interface and the screens of the
 * workflow modules below {@code /wm/} need their scripts, so they are left alone. It is a servlet
 * filter and not part of a security chain, because the application writes the chains, and the
 * header should be there whatever chain it writes.
 */
@Configuration
public class GuiApiContentSecurityPolicy {

  /**
   * The value of the header {@code Content-Security-Policy} on every answer of the GUI API.
   */
  public static final String POLICY = "default-src 'none'; frame-ancestors 'none'";

  /**
   * The paths the policy is set on.
   */
  public static final String GUI_API_PATHS = "/gui/api/*";

  /**
   * Creates the configuration. Spring creates it because of {@code @Configuration}.
   */
  public GuiApiContentSecurityPolicy() {
  }

  /**
   * Registers the filter for the paths of the GUI API.
   *
   * @return The registration
   */
  @Bean
  public FilterRegistrationBean<PolicyFilter> guiApiContentSecurityPolicyFilter() {

    final var registration = new FilterRegistrationBean<>(new PolicyFilter());
    registration.addUrlPatterns(GUI_API_PATHS);
    // ahead of the security chain, so a 401 or 403 of the chain carries the header as well
    registration.setOrder(SecurityFilterProperties.DEFAULT_FILTER_ORDER - 1);
    return registration;

  }

  /**
   * Sets the header before the request goes on, so an answer written by any later filter, the
   * security chain included, carries it too.
   */
  public static class PolicyFilter extends OncePerRequestFilter {

    /**
     * Creates the filter.
     */
    public PolicyFilter() {
    }

    @Override
    protected void doFilterInternal(
        final HttpServletRequest request,
        final HttpServletResponse response,
        final FilterChain filterChain) throws ServletException, IOException {

      response.setHeader("Content-Security-Policy", POLICY);
      filterChain.doFilter(request, response);

    }

  }

}
