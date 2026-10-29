package io.vanillabp.cockpit.devshell.simulator.config;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import io.vanillabp.cockpit.commons.security.jwt.JwtAuthenticationTokenMapper;
import io.vanillabp.cockpit.commons.security.jwt.JwtMapper;
import io.vanillabp.cockpit.commons.security.jwt.JwtProperties;
import io.vanillabp.cockpit.devshell.simulator.usermanagement.JwtServerSecurityContextRepository;
import io.vanillabp.cockpit.devshell.simulator.usermanagement.UserController;

/**
 * The security setup of the dev shell simulator. The made-up users log in with one shared
 * password, and the user the dev shell switched to travels in a JSON Web Token (JWT) cookie.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /**
   * The one password all simulated users share. It belongs to this simulator and nowhere else:
   * the users are made up people from a configuration file, and the simulator runs on a
   * developer's machine and ships in no product.
   */
  static final String SIMULATED_USER_PASSWORD = "Secure_123";

  /**
   * Creates the configuration. Spring creates it, nobody else does.
   */
  public SecurityConfig() {
  }

  /**
   * The settings of the JWT and its cookie, read from {@code application.jwt}.
   *
   * @return The JWT settings
   */
  @Bean
  @ConfigurationProperties("application.jwt")
  public JwtProperties jwtProperties() {
    return new JwtProperties();
  }

  /**
   * The filter chain for the official API and the BPMS API. Every request is allowed, because
   * the simulator only runs on a developer's machine.
   *
   * @param http The builder of the filter chain
   * @return The filter chain
   * @throws Exception If the filter chain cannot be built
   */
  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  public SecurityFilterChain simulatorSecurityFilterChain(
      final HttpSecurity http) throws Exception {

    return http.securityMatchers(
        c -> c.requestMatchers(
            "/official-api/**", // Tasklist-API
            "/bpms/api/**" // BPMS-API
        ))
        .csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
        .anonymous(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(c -> c.anyRequest().permitAll())
        .build();

  }

  /**
   * The configured users, kept in memory. Each one gets the shared password, and its groups
   * become its authorities.
   *
   * @param properties The properties which list the users
   * @return The user details service
   */
  @Bean
  public UserDetailsService inMemoryUserDetailsService(
      final Properties properties) {

    // Spring Security deprecated User.withDefaultPasswordEncoder because it encoded the password
    // out of sight of the reader, which made a sample look like something to copy. The encoder
    // is created here instead, so the file shows which one encodes what.
    final var passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    final var users = properties.getUsers().stream()
        .map(
            user -> User.withUsername(user.getId())
                .password(passwordEncoder.encode(SIMULATED_USER_PASSWORD))
                .authorities(
                    Optional.ofNullable(user.getGroups()).orElse(List.of()).stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList())
                .build())
        .toList();
    return new InMemoryUserDetailsManager(users);

  }

  /**
   * Keeps the user the dev shell switched to in a JWT cookie.
   *
   * @param jwtMapper Turns the JWT into an authentication token and back
   * @param jwtProperties The settings of the JWT and its cookie
   * @return The repository
   */
  @Bean
  @ConditionalOnMissingBean
  public JwtServerSecurityContextRepository jwtServerSecurityContextRepository(
      final JwtMapper<? extends AbstractAuthenticationToken> jwtMapper,
      final JwtProperties jwtProperties) {

    return new JwtServerSecurityContextRepository(jwtProperties, jwtMapper);

  }

  /**
   * Turns a JWT into an authentication token and back. Groups are not resolved here, the
   * business application maps them.
   *
   * @param jwtProperties The settings of the JWT
   * @return The mapper
   */
  @Bean
  @ConditionalOnMissingBean
  public JwtMapper<? extends AbstractAuthenticationToken> jwtMapper(
      final JwtProperties jwtProperties) {

    return new JwtAuthenticationTokenMapper(
        jwtProperties,
        // no group resolving in Dev-Shell-Simulator - mapping of groups is done via business application
        Function.identity());

  }


  /**
   * The controller the dev shell uses to list the users and to switch between them.
   *
   * @param properties The properties which list the users
   * @param securityContextRepository Keeps the current user in a JWT cookie
   * @param jwtProperties The settings of the JWT and its cookie
   * @return The controller
   */
  @Bean
  @ConditionalOnMissingBean
  public UserController devShellUserDropdownRestController(
      final Properties properties,
      final JwtServerSecurityContextRepository securityContextRepository,
      final JwtProperties jwtProperties) {

    return new UserController(properties, securityContextRepository, jwtProperties);

  }

}