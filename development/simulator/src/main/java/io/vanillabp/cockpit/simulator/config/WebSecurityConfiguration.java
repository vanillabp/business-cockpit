package io.vanillabp.cockpit.simulator.config;

import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import io.vanillabp.cockpit.devshell.simulator.config.Properties;
import io.vanillabp.cockpit.devshell.simulator.usermanagement.SimulatedUserDirectoryService;
import io.vanillabp.cockpit.devshell.simulator.usermanagement.UserController;
import io.vanillabp.cockpit.support.security.jwt.JwtAuthenticationToken;
import io.vanillabp.cockpit.support.security.jwt.JwtAuthenticationTokenMapper;
import io.vanillabp.cockpit.support.security.jwt.JwtMapper;
import io.vanillabp.cockpit.support.security.jwt.JwtUserDetailsProvider;
import io.vanillabp.cockpit.support.security.jwt.PassiveJwtSecurityFilter;
import io.vanillabp.cockpit.support.security.usercontext.UserContext;
import io.vanillabp.cockpit.support.security.usercontext.UserDetailsProvider;
import io.vanillabp.cockpit.support.security.usercontext.UserDirectoryService;

/**
 * The security setup of the simulator. If a request carries a JSON Web Token (JWT), the token is
 * checked, and the code gets the user it names through the {@link UserContext}.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class WebSecurityConfiguration {

  @Autowired
  private ApplicationProperties properties;

  /**
   * Creates the configuration. Spring creates it, nobody else does.
   */
  public WebSecurityConfiguration() {
  }

  /**
   * Reads the details of the current user from the JWT and the groups from the user directory.
   *
   * @return The provider
   */
  @Bean
  public UserDetailsProvider userDetailsProvider() {

    return new JwtUserDetailsProvider();

  }

  /**
   * Gives the code access to the user who sent the current request.
   *
   * @param userDetailsProvider Reads the details of the user
   * @return The user context
   */
  @Bean
  public UserContext userContext(
      final UserDetailsProvider userDetailsProvider) {

    return new UserContext(userDetailsProvider);

  }

  /**
   * The filter chain for all requests. CSRF, CORS and anonymous access are switched off, and
   * the JWT filter runs after basic authentication.
   *
   * @param http The builder of the filter chain
   * @param jwtSecurityFilter The filter which reads the JWT
   * @return The filter chain
   * @throws Exception If the filter chain cannot be built
   */
  @Bean
  @Order(99)
  protected SecurityFilterChain configure(
      final HttpSecurity http,
      final PassiveJwtSecurityFilter jwtSecurityFilter) throws Exception {

    http
        // Spring Security 7 removed the non-lambda DSL variants; pure syntax conversion.
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.disable())
        .anonymous(anonymous -> anonymous.disable())
        .addFilterAfter(jwtSecurityFilter, BasicAuthenticationFilter.class);

    return http.build();

  }

  /**
   * The users of the dev shell simulator as a user directory. The JWT carries no groups, so the
   * groups of a user come from here.
   *
   * @param devShellProperties The properties of the dev shell simulator, which list the users
   * @return The directory
   */
  @Bean
  public UserDirectoryService userDirectoryService(
      final Properties devShellProperties) {

    return new SimulatedUserDirectoryService(devShellProperties);

  }

  /**
   * Turns a JWT into an authentication token for Spring Security. The groups of the user come
   * from the directory.
   *
   * @param userDirectoryService Gives the groups of the user
   * @return The mapper
   */
  @Bean
  public JwtMapper<? extends JwtAuthenticationToken> jwtMapper(
      final UserDirectoryService userDirectoryService) {

    return new JwtAuthenticationTokenMapper(properties.getJwt(), userDirectoryService, Function.identity());

  }

  /**
   * The filter which reads the JWT of a request. It is passive: a request without a token
   * is not rejected.
   *
   * @param jwtMapper Turns the JWT into an authentication token
   * @return The filter
   */
  @Bean
  public PassiveJwtSecurityFilter jwtSecurityFilter(
      final JwtMapper<? extends JwtAuthenticationToken> jwtMapper) {

    return new PassiveJwtSecurityFilter(
        properties.getJwt(), jwtMapper);

  }

  /**
   * The user controller of the dev shell simulator. Here it only loads the users when the
   * Business Cockpit starts with the local Spring Boot profile.
   *
   * @param properties The properties of the dev shell simulator, which list the users
   * @return The controller
   */
  @Bean
  public UserController devShellUserDropdownRestController(
      final Properties properties) {

    // passing null, null because controller is only used to load users on startup
    // of Business Cockpit when running with local Spring Boot profile
    return new UserController(properties, null, null);

  }

}
