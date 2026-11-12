package io.vanillabp.cockpit.security;

import static io.vanillabp.cockpit.config.web.WebSecurityConfiguration.appInfoRequestMatcher;
import static io.vanillabp.cockpit.config.web.WebSecurityConfiguration.assetsRequestMatcher;
import static io.vanillabp.cockpit.config.web.WebSecurityConfiguration.currentUserRequestMatcher;
import static io.vanillabp.cockpit.config.web.WebSecurityConfiguration.staticRequestMatcher;
import static io.vanillabp.cockpit.config.web.WebSecurityConfiguration.updatesRequestMatcher;
import static io.vanillabp.cockpit.config.web.WebSecurityConfiguration.workflowModulesProxyRequestMatcher;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import io.vanillabp.cockpit.config.properties.ApplicationProperties;
import io.vanillabp.cockpit.support.security.jwt.JwtAuthenticationToken;
import io.vanillabp.cockpit.support.security.jwt.JwtLogoutSuccessHandler;
import io.vanillabp.cockpit.support.security.jwt.JwtMapper;
import io.vanillabp.cockpit.support.security.jwt.JwtRenewalFilter;
import io.vanillabp.cockpit.support.security.jwt.JwtSecurityContextRepository;
import io.vanillabp.cockpit.support.security.jwt.JwtUserDetailsProvider;
import io.vanillabp.cockpit.support.security.jwt.PassiveJwtSecurityFilter;
import io.vanillabp.cockpit.support.security.usercontext.UserDetailsProvider;
import io.vanillabp.cockpit.support.security.usercontext.UserDirectoryService;

/**
 * How a person logs in to this application: once by HTTP basic authentication, and after that by
 * the JWT (JSON Web Token) cookie which the login hands out.
 * <p>
 * This is the application's part, and the library brings none of it. A custom cockpit writes a
 * chain of its own, for example with the login of an identity provider. The wiki page "Security"
 * says what every chain has to keep: the five open paths, the two JWT filters and the
 * {@link JwtSecurityContextRepository}. The users here all share the password {@code test}, so
 * this chain is an example and nothing to run in production.
 */
@Configuration
public class GuiSecurityConfiguration {

  /**
   * The one password of all users. The users come from the user directory of the profile
   * {@code local}, and that directory knows no passwords.
   */
  private static final String PASSWORD_OF_EVERY_USER = "{noop}test";

  @Autowired
  private ApplicationProperties properties;

  /**
   * Creates the configuration. Spring creates it, because the component scan of the application
   * finds it.
   */
  public GuiSecurityConfiguration() {
  }

  /**
   * The security filter chain of the GUI API and of the user interface. It has no
   * {@code securityMatcher}, so it takes every request which no other chain takes.
   *
   * @param http The builder of the chain
   * @param jwtSecurityContextRepository Stores the login in the JWT cookie
   * @param jwtMapper Turns a token into an authentication and back
   * @param userDirectoryService Provides the users who may log in by basic authentication
   * @return The chain
   * @throws Exception If Spring Security cannot build the chain
   */
  @Bean
  @Order(99)
  public SecurityFilterChain guiHttpSecurity(
      final HttpSecurity http,
      final JwtSecurityContextRepository jwtSecurityContextRepository,
      final JwtMapper<? extends JwtAuthenticationToken> jwtMapper,
      final UserDirectoryService userDirectoryService) throws Exception {

    final var basicEntryPoint = new BasicAuthenticationEntryPoint();
    basicEntryPoint.setRealmName(properties.getTitleShort());
    basicEntryPoint.afterPropertiesSet();

    http
        .csrf(AbstractHttpConfigurer::disable)
        .cors(AbstractHttpConfigurer::disable)
        .anonymous(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(requests -> requests
            .requestMatchers(appInfoRequestMatcher, currentUserRequestMatcher, assetsRequestMatcher,
                staticRequestMatcher, workflowModulesProxyRequestMatcher)
            .permitAll()
            .anyRequest()
            .authenticated())
        // scoped to this chain, so the BPMS API chain keeps its own set of credentials
        .authenticationManager(new ProviderManager(
            new DaoAuthenticationProvider(localUserDetailsService(userDirectoryService))))
        .httpBasic(basic -> basic
            // signing in once by basic auth hands out the JWT cookie all further
            // requests authenticate with
            .securityContextRepository(jwtSecurityContextRepository)
            .authenticationEntryPoint(basicEntryPoint))
        .logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessHandler(jwtLogoutSuccessHandler()))
        // behind the basic authentication filter: the login request has no cookie yet and
        // authenticates by basic authentication, every later request by its cookie
        .addFilterAfter(new PassiveJwtSecurityFilter(properties.getJwt(), jwtMapper),
            BasicAuthenticationFilter.class)
        // reads the authentication the JWT filter restored, so it runs after that one
        .addFilterAfter(new JwtRenewalFilter(properties.getJwt(), jwtMapper, updatesRequestMatcher),
            PassiveJwtSecurityFilter.class);

    return http.build();

  }

  /**
   * Removes the JWT cookie on logout and sends the browser to the start page.
   *
   * @return The handler
   */
  public JwtLogoutSuccessHandler jwtLogoutSuccessHandler() {

    final var handler = new JwtLogoutSuccessHandler(properties.getJwt());
    handler.setDefaultTargetUrl("/");
    return handler;

  }

  /**
   * Provides the details of the user who is logged in, read from the JWT. The library needs a
   * bean of this type and brings none, because only the application knows where its users come
   * from. The JWT names the user but carries no groups. The groups come from the
   * {@link UserDirectoryService} on every request, so a group taken away there is gone on the next
   * request.
   *
   * @return The provider
   */
  @Bean
  public UserDetailsProvider userDetailsProvider() {

    return new JwtUserDetailsProvider();

  }

  private InMemoryUserDetailsManager localUserDetailsService(
      final UserDirectoryService userDirectoryService) {

    final var users = userDirectoryService
        .getAllUsers()
        .stream()
        .map(user -> User.builder()
            .username(user.getId())
            .password(PASSWORD_OF_EVERY_USER)
            .authorities(user
                .getAuthorities()
                .stream()
                .map(SimpleGrantedAuthority::new)
                .toList())
            .build())
        .toList();
    return new InMemoryUserDetailsManager(users);

  }

}
