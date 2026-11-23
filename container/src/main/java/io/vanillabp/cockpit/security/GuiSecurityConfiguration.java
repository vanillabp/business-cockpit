package io.vanillabp.cockpit.security;

import static io.vanillabp.cockpit.config.web.WebSecurityConfiguration.appInfoRequestMatcher;
import static io.vanillabp.cockpit.config.web.WebSecurityConfiguration.assetsRequestMatcher;
import static io.vanillabp.cockpit.config.web.WebSecurityConfiguration.currentUserRequestMatcher;
import static io.vanillabp.cockpit.config.web.WebSecurityConfiguration.staticRequestMatcher;
import static io.vanillabp.cockpit.config.web.WebSecurityConfiguration.updatesRequestMatcher;
import static io.vanillabp.cockpit.config.web.WebSecurityConfiguration.workflowModulesProxyRequestMatcher;
import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

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
import jakarta.servlet.DispatcherType;

/**
 * How a person logs in to this application. A person logs in once, and after that every request
 * carries the JWT (JSON Web Token) cookie which the login hands out.
 * <p>
 * This is the application's part, and the library brings none of it. There are two chains, and
 * the Spring profile decides which one runs:
 * <ul>
 * <li>{@link LoginByOpenIdConnect} is the default. It sends the browser to the OpenID Connect
 * provider of the Spring Boot keys {@code spring.security.oauth2.client.*}. The profile
 * {@code local} sets them for the simulator of the development tools, which plays the provider on
 * port 8079.</li>
 * <li>{@link LoginByHttpBasic} runs with the profile {@value #BASIC_AUTH_PROFILE}. It asks for a
 * user name and a password, and every user has the password {@code test}. It is meant for
 * scripts, for the tests of this module, and for a run without the simulator.</li>
 * </ul>
 * Both chains end in the same cookie, so the rest of the cockpit does not know which one logged
 * the user in. The logout of {@link LoginByOpenIdConnect} ends the session at the provider as
 * well, see {@link LogoutAtTheProvider}. The wiki page "Security" says what every chain has to
 * keep: the five open paths, the rule for a request dispatched again, the two JWT filters and the
 * {@link JwtSecurityContextRepository}. Neither chain is meant for
 * production as it is. The provider of the profile {@code local} and the password are for a
 * developer's machine.
 */
@Configuration
public class GuiSecurityConfiguration {

  /**
   * The Spring profile which switches the login from OpenID Connect to HTTP basic.
   */
  public static final String BASIC_AUTH_PROFILE = "basic-auth";

  /**
   * The paths a program asks for and no person opens in the browser. A request without a login
   * gets 401 there. A redirect to the login page would not help a program: a {@code fetch} of the
   * user interface or a BPMS adapter would follow it and fail at the provider.
   */
  static final RequestMatcher API_REQUEST_MATCHER = new OrRequestMatcher(
      pathPattern("/gui/api/**"), pathPattern("/bpms/api/**"));

  /**
   * Creates the configuration. Spring creates it, because the component scan of the application
   * finds it.
   */
  public GuiSecurityConfiguration() {
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

  /**
   * The login at an OpenID Connect provider, the default of this application.
   */
  @Configuration
  @Profile("!"
      + BASIC_AUTH_PROFILE)
  public static class LoginByOpenIdConnect {

    @Autowired
    private ApplicationProperties properties;

    /**
     * Creates the configuration. Spring creates it, because it is nested in a configuration.
     */
    public LoginByOpenIdConnect() {
    }

    /**
     * The security filter chain of the GUI API and of the user interface. It has no
     * {@code securityMatcher}, so it takes every request which no other chain takes.
     *
     * @param http The builder of the chain
     * @param jwtSecurityContextRepository Stores the login in the JWT cookie
     * @param jwtMapper Turns a token into an authentication and back
     * @param clientRegistrations The providers of the Spring Boot keys
     *     {@code spring.security.oauth2.client.*}
     * @return The chain
     * @throws Exception If Spring Security cannot build the chain
     */
    @Bean
    @Order(99)
    public SecurityFilterChain guiHttpSecurity(
        final HttpSecurity http,
        final JwtSecurityContextRepository jwtSecurityContextRepository,
        final JwtMapper<? extends JwtAuthenticationToken> jwtMapper,
        final ObjectProvider<ClientRegistrationRepository> clientRegistrations) throws Exception {

      final var providers = clientRegistrations.getIfAvailable();
      if (providers == null) {
        throw new IllegalStateException("""
            The login of this application needs an OpenID Connect provider, and none is \
            configured. The profile 'local' configures the simulator of the development tools, \
            which answers on port 8079. So start with the profile 'local', or name your provider \
            with the keys 'spring.security.oauth2.client.registration.<id>.*' and \
            'spring.security.oauth2.client.provider.<id>.*', or add the profile '%s' to log in \
            with a user name and a password.""".formatted(BASIC_AUTH_PROFILE));
      }

      // PKCE (proof key for code exchange) ties the code to the browser which asked for it.
      // Spring Security adds it on its own only for a client without a secret, but the current
      // advice is to add it for every client, and Spring Authorization Server asks for it.
      final var authorizationRequests = new DefaultOAuth2AuthorizationRequestResolver(providers, OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);
      authorizationRequests.setAuthorizationRequestCustomizer(
          OAuth2AuthorizationRequestCustomizers.withPkce());

      keepWhatEveryChainNeeds(http, properties, jwtMapper)
          // added before oauth2Login adds its redirect to the provider, so it is asked first
          .exceptionHandling(exceptions -> exceptions
              .defaultAuthenticationEntryPointFor(
                  new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                  API_REQUEST_MATCHER))
          .oauth2Login(login -> login
              .authorizationEndpoint(endpoint -> endpoint
                  .authorizationRequestResolver(authorizationRequests))
              .successHandler(new KeepTheIdTokenForTheLogout())
              .withObjectPostProcessor(writeTheLoginInto(jwtSecurityContextRepository)))
          // the login at the provider has to end as well, so this replaces the logout of
          // keepWhatEveryChainNeeds
          .logout(logout -> logout
              .logoutSuccessHandler(new LogoutAtTheProvider(properties.getJwt(), providers)));

      return http.build();

    }

  }

  /**
   * Makes the login at the provider end in the JWT cookie, like every login of the cockpit.
   * Nothing needs the tokens of the provider after that.
   * <p>
   * {@code oauth2Login(login -> login.securityContextRepository(..))} looks like the place for
   * this, but it has no effect. While Spring Security builds the chain, it replaces that
   * repository with the one of the whole chain, which keeps the login in the HTTP session. The
   * login would then work in the browser, but without the cookie, and a workflow module behind
   * the proxy would not know the user. A post processor runs after that replacement.
   *
   * @param jwtSecurityContextRepository Writes the login into the JWT cookie
   * @return The post processor of the filter which finishes the login
   */
  static ObjectPostProcessor<OAuth2LoginAuthenticationFilter> writeTheLoginInto(
      final JwtSecurityContextRepository jwtSecurityContextRepository) {

    return new ObjectPostProcessor<>() {
      @Override
      public <O extends OAuth2LoginAuthenticationFilter> O postProcess(
          final O filter) {
        filter.setSecurityContextRepository(jwtSecurityContextRepository);
        return filter;
      }
    };

  }

  /**
   * The login with a user name and a password, for the profile {@value #BASIC_AUTH_PROFILE}.
   */
  @Configuration
  @Profile(BASIC_AUTH_PROFILE)
  public static class LoginByHttpBasic {

    /**
     * The one password of all users. The users come from the user directory of the profile
     * {@code local}, and that directory knows no passwords.
     */
    private static final String PASSWORD_OF_EVERY_USER = "{noop}test";

    @Autowired
    private ApplicationProperties properties;

    /**
     * Creates the configuration. Spring creates it, because it is nested in a configuration.
     */
    public LoginByHttpBasic() {
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

      keepWhatEveryChainNeeds(http, properties, jwtMapper)
          // scoped to this chain, so the BPMS API chain keeps its own set of credentials
          .authenticationManager(new ProviderManager(
              new DaoAuthenticationProvider(localUserDetailsService(userDirectoryService))))
          .httpBasic(basic -> basic
              // signing in once by basic auth hands out the JWT cookie all further
              // requests authenticate with
              .securityContextRepository(jwtSecurityContextRepository)
              .authenticationEntryPoint(basicEntryPoint));

      return http.build();

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

  /**
   * What both chains share. Every chain of a cockpit keeps this, whatever its login is.
   *
   * @param http The builder of the chain
   * @param properties Holds the settings of the JWT cookie
   * @param jwtMapper Turns a token into an authentication and back
   * @return The builder, for the login to be added
   * @throws Exception If Spring Security refuses a setting
   */
  static HttpSecurity keepWhatEveryChainNeeds(
      final HttpSecurity http,
      final ApplicationProperties properties,
      final JwtMapper<? extends JwtAuthenticationToken> jwtMapper) throws Exception {

    return http
        .csrf(AbstractHttpConfigurer::disable)
        .cors(AbstractHttpConfigurer::disable)
        .anonymous(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(requests -> requests
            // a request dispatched again, for the update stream when it answers from another
            // thread or when the browser has gone away. Its first dispatch passed this chain
            // already. PassiveJwtSecurityFilter reads the cookie once per request, so on this
            // dispatch there is no login, and the check would only log an error
            .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR)
            .permitAll()
            .requestMatchers(appInfoRequestMatcher, currentUserRequestMatcher, assetsRequestMatcher,
                staticRequestMatcher, workflowModulesProxyRequestMatcher)
            .permitAll()
            // the actuator is for operations, not for the users of the cockpit. An application
            // which exposes an endpoint writes a chain of its own for it, with a lower order
            .requestMatchers(pathPattern("/actuator/**"))
            .denyAll()
            .anyRequest()
            .authenticated())
        .logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessHandler(jwtLogoutSuccessHandler(properties)))
        // behind the filters which log in: the login request has no cookie yet and
        // authenticates by the login, every later request by its cookie
        .addFilterAfter(new PassiveJwtSecurityFilter(properties.getJwt(), jwtMapper),
            BasicAuthenticationFilter.class)
        // reads the authentication the JWT filter restored, so it runs after that one
        .addFilterAfter(new JwtRenewalFilter(properties.getJwt(), jwtMapper, updatesRequestMatcher),
            PassiveJwtSecurityFilter.class);

  }

  /**
   * Removes the JWT cookie on logout and sends the browser to the start page.
   *
   * @param properties Holds the settings of the JWT cookie
   * @return The handler
   */
  static JwtLogoutSuccessHandler jwtLogoutSuccessHandler(
      final ApplicationProperties properties) {

    final var handler = new JwtLogoutSuccessHandler(properties.getJwt());
    handler.setDefaultTargetUrl("/");
    return handler;

  }

}
