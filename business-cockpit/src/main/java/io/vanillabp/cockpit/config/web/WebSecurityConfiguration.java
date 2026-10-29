package io.vanillabp.cockpit.config.web;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AbstractAuthenticationToken;
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
import org.springframework.security.web.util.matcher.RequestMatcher;

import io.vanillabp.cockpit.bpms.BpmsApiWebSecurityConfiguration;
import io.vanillabp.cockpit.commons.security.jwt.JwtAuthenticationToken;
import io.vanillabp.cockpit.commons.security.jwt.JwtAuthenticationTokenMapper;
import io.vanillabp.cockpit.commons.security.jwt.JwtLogoutSuccessHandler;
import io.vanillabp.cockpit.commons.security.jwt.JwtMapper;
import io.vanillabp.cockpit.commons.security.jwt.JwtRenewalFilter;
import io.vanillabp.cockpit.commons.security.jwt.JwtSecurityContextRepository;
import io.vanillabp.cockpit.commons.security.jwt.JwtUserDetailsProvider;
import io.vanillabp.cockpit.commons.security.jwt.PassiveJwtSecurityFilter;
import io.vanillabp.cockpit.config.properties.ApplicationProperties;
import io.vanillabp.cockpit.users.UserDetailsProvider;
import io.vanillabp.cockpit.workflowmodules.GroupHierarchyService;

/**
 * The security of the GUI API and of the files of the user interface. A user logs in once by HTTP
 * basic authentication and gets a JWT (JSON Web Token) cookie, which every later request is
 * authenticated with. The security of the BPMS API is a chain of its own, imported from
 * {@link BpmsApiWebSecurityConfiguration}.
 */
@Configuration
@Import(BpmsApiWebSecurityConfiguration.class)
public class WebSecurityConfiguration {

  /**
   * The information about the application, like its title and version. The browser needs it
   * before anybody logs in, so it is open to everybody.
   */
  public static final RequestMatcher appInfoRequestMatcher = pathPattern(
      "/gui/api/v1/app/info");

  /**
   * The user who is logged in. It is open to everybody, so the browser can find out whether
   * somebody is logged in at all.
   */
  public static final RequestMatcher currentUserRequestMatcher = pathPattern(
      "/gui/api/v1/app/current-user");

  /**
   * The update stream of a browser tab. It connects again by itself, so it is no sign of somebody
   * using the cockpit and does not renew the login.
   */
  public static final RequestMatcher updatesRequestMatcher = pathPattern(
      "/gui/api/v1/updates");

  /**
   * The files the user interface is built of. They are open to everybody.
   */
  public static final RequestMatcher assetsRequestMatcher = pathPattern(
      "/assets/**");

  /**
   * Further static files of the user interface. They are open to everybody.
   */
  public static final RequestMatcher staticRequestMatcher = pathPattern(
      "/static/**");

  /**
   * The requests the cockpit forwards to the user interfaces of the workflow modules. They are
   * open to everybody, so the cockpit itself does not ask for a login here.
   */
  public static final RequestMatcher workflowModulesProxyRequestMatcher = pathPattern(
      "/wm/**");

  @Autowired
  private ApplicationProperties properties;

  /**
   * Creates the configuration. Spring creates it because of {@code @Configuration}.
   */
  public WebSecurityConfiguration() {
  }

  /**
   * Authenticates a request from the JWT cookie whenever it carries one. It sits behind the
   * basic authentication filter. So the login request, which has no cookie yet, authenticates
   * by basic authentication, and every later request authenticates by its cookie.
   */
  private PassiveJwtSecurityFilter jwtSecurityFilter(
      final JwtMapper<? extends JwtAuthenticationToken> jwtMapper) {

    return new PassiveJwtSecurityFilter(properties.getJwt(), jwtMapper);

  }

  /**
   * Renews the token of a login which is in use. It sits behind {@link #jwtSecurityFilter}, whose
   * authentication it reads.
   */
  private JwtRenewalFilter jwtRenewalFilter(
      final JwtMapper<? extends JwtAuthenticationToken> jwtMapper) {

    return new JwtRenewalFilter(properties.getJwt(), jwtMapper, updatesRequestMatcher);

  }

  /**
   * Turns a login token into the authentication of a user and back. The groups of the user are
   * completed by the group hierarchy. An application can bring its own mapper instead.
   *
   * @param groupHierarchyService Resolves the groups a group belongs to
   * @return The mapper
   */
  @Bean
  @ConditionalOnMissingBean
  public JwtMapper<? extends JwtAuthenticationToken> jwtMapper(
      final GroupHierarchyService groupHierarchyService) {

    return new JwtAuthenticationTokenMapper(
        properties.getJwt(), groupHierarchyService::resolveGroups);

  }

  /**
   * Stores the authentication of a login in the JWT cookie, so later requests find it there.
   *
   * @param jwtMapper Turns the authentication into a token
   * @return The repository
   */
  @Bean
  public JwtSecurityContextRepository jwtSecurityContextRepository(
      final JwtMapper<? extends AbstractAuthenticationToken> jwtMapper) {

    return new JwtSecurityContextRepository(
        properties.getJwt(), jwtMapper);

  }

  /**
   * The security filter chain of the GUI API and of the user interface. An application can
   * bring its own chain named {@code guiHttpSecurity} instead.
   *
   * @param http The builder of the chain
   * @param jwtSecurityContextRepository Stores the login in the JWT cookie
   * @param jwtMapper Turns a token into an authentication and back
   * @param userService Provides the users who may log in by basic authentication
   * @return The chain
   * @throws Exception If Spring Security cannot build the chain
   */
  @Bean
  @Order(99)
  @ConditionalOnMissingBean(name = "guiHttpSecurity")
  public SecurityFilterChain guiHttpSecurity(
      final HttpSecurity http,
      final JwtSecurityContextRepository jwtSecurityContextRepository,
      final JwtMapper<? extends JwtAuthenticationToken> jwtMapper,
      final UserDetailsProvider userService) throws Exception {

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
            new DaoAuthenticationProvider(localUserDetailsService(userService))))
        .httpBasic(basic -> basic
            // signing in once by basic auth hands out the JWT cookie all further
            // requests authenticate with
            .securityContextRepository(jwtSecurityContextRepository)
            .authenticationEntryPoint(basicEntryPoint))
        .logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessHandler(jwtLogoutSuccessHandler()))
        .addFilterAfter(jwtSecurityFilter(jwtMapper), BasicAuthenticationFilter.class)
        .addFilterAfter(jwtRenewalFilter(jwtMapper), PassiveJwtSecurityFilter.class);

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

  private InMemoryUserDetailsManager localUserDetailsService(
      final UserDetailsProvider userService) {

    final var users = userService
        .getAllUsers()
        .stream()
        .map(user -> User.builder()
            .username(user.getId())
            .password("{noop}test")
            .authorities(user
                .getAuthorities()
                .stream()
                .map(SimpleGrantedAuthority::new)
                .toList())
            .build())
        .toList();
    return new InMemoryUserDetailsManager(users);

  }

  /**
   * Provides the details of the user who is logged in, read from the JWT. An application can
   * bring its own bean named {@code userDetailsProvider} instead.
   *
   * @return The provider
   */
  @Bean
  @ConditionalOnMissingBean(name = "userDetailsProvider")
  public io.vanillabp.cockpit.commons.security.usercontext.UserDetailsProvider userDetailsProvider() {

    return new JwtUserDetailsProvider();

  }

}
