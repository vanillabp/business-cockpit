package io.vanillabp.cockpit.simulator.config;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import io.vanillabp.cockpit.devshell.simulator.config.Properties;

/**
 * The simulator plays the OpenID Connect provider of the Business Cockpit on a developer's
 * machine. The module {@code container} logs a person in here in its profile {@code local}.
 * <p>
 * The persons are the users of the dev shell simulator, from {@code dev-shell-simulator.users},
 * and each of them logs in with the password {@value #PASSWORD_OF_EVERY_USER}. The client the
 * cockpit logs in with is registered by the Spring Boot keys
 * {@code spring.security.oauth2.authorizationserver.client.*} in {@code application.yaml}. Spring
 * Boot also brings the key which signs the tokens. It makes a new one at every start, which is
 * enough here: the cockpit keeps its login in a cookie of its own and reads the tokens of the
 * provider only once, at the login.
 */
@Configuration
public class AuthorizationServerConfiguration {

  /**
   * The password of every person. It is the one the same people log in to the dev shell
   * simulator with, so a developer has one password to remember.
   */
  static final String PASSWORD_OF_EVERY_USER = "test";

  /**
   * The page with the form a person logs in with. Spring Security writes it.
   */
  static final String LOGIN_PAGE = "/login";

  /**
   * Creates the configuration. Spring creates it, nobody else does.
   */
  public AuthorizationServerConfiguration() {
  }

  /**
   * The chain of the endpoints of the provider: authorization, token, keys and the OpenID Connect
   * discovery. It comes before every other chain of the simulator.
   *
   * @param http The builder of the chain
   * @return The chain
   * @throws Exception If Spring Security cannot build the chain
   */
  @Bean
  @Order(1)
  public SecurityFilterChain authorizationServerSecurityFilterChain(
      final HttpSecurity http) throws Exception {

    http
        .oauth2AuthorizationServer(server -> {
          // this chain takes only the endpoints of the provider
          http.securityMatcher(server.getEndpointsMatcher());
          server
              .oidc(Customizer.withDefaults())
              .authorizationEndpoint(endpoint -> endpoint
                  .authorizationResponseHandler(forgetThePersonOnceTheCodeIsIssued()));
        })
        .authorizeHttpRequests(requests -> requests
            .anyRequest()
            .authenticated())
        // a browser which is not logged in yet gets the login form
        .exceptionHandling(exceptions -> exceptions
            .defaultAuthenticationEntryPointFor(
                new LoginUrlAuthenticationEntryPoint(LOGIN_PAGE),
                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));

    return http.build();

  }

  /**
   * The chain of the login form. After the login it sends the browser back to the authorization
   * endpoint, which the browser came from.
   *
   * @param http The builder of the chain
   * @param simulatedPersons The persons who may log in
   * @return The chain
   * @throws Exception If Spring Security cannot build the chain
   */
  @Bean
  @Order(2)
  public SecurityFilterChain loginFormSecurityFilterChain(
      final HttpSecurity http,
      final UserDetailsService simulatedPersons) throws Exception {

    http
        .securityMatcher(LOGIN_PAGE)
        .authorizeHttpRequests(requests -> requests
            .anyRequest()
            .permitAll())
        .userDetailsService(simulatedPersons)
        .formLogin(Customizer.withDefaults());

    return http.build();

  }

  /**
   * The users of the dev shell simulator, as the persons who may log in. Their groups become their
   * authorities, but the cockpit does not read them from here: it asks its user directory.
   *
   * @param properties The properties of the dev shell simulator, which list the users
   * @return The persons
   */
  @Bean
  public UserDetailsService simulatedPersons(
      final Properties properties) {

    final var persons = properties
        .getUsers()
        .stream()
        .map(user -> User
            .withUsername(user.getId())
            .password("{noop}"
                + PASSWORD_OF_EVERY_USER)
            .authorities(Optional
                .ofNullable(user.getGroups())
                .orElse(List.of())
                .stream()
                .map(SimpleGrantedAuthority::new)
                .toList())
            .build())
        .toList();
    return new InMemoryUserDetailsManager(persons);

  }

  /**
   * Sends the browser back to the cockpit with the code, as the default handler does, and ends
   * the session of the person at the simulator before. So the next login asks for a user name
   * and a password again. A developer can then log in as another user, and a logout at the
   * cockpit is not undone by the session which is still open here.
   */
  private static AuthenticationSuccessHandler forgetThePersonOnceTheCodeIsIssued() {

    final var redirectStrategy = new DefaultRedirectStrategy();
    return (
        request,
        response,
        authentication) -> {

      final var codeRequest = (OAuth2AuthorizationCodeRequestAuthenticationToken) authentication;
      final var redirect = UriComponentsBuilder
          .fromUriString(codeRequest.getRedirectUri())
          .queryParam(OAuth2ParameterNames.CODE, codeRequest.getAuthorizationCode().getTokenValue());
      if (StringUtils.hasText(codeRequest.getState())) {
        redirect.queryParam(OAuth2ParameterNames.STATE,
            UriUtils.encode(codeRequest.getState(), StandardCharsets.UTF_8));
      }

      final var session = request.getSession(false);
      if (session != null) {
        session.invalidate();
      }

      redirectStrategy.sendRedirect(request, response, redirect.build(true).toUriString());

    };

  }

}
