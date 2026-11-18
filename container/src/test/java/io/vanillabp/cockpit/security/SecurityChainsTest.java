package io.vanillabp.cockpit.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.config.Customizer.withDefaults;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.support.GenericWebApplicationContext;

import io.vanillabp.cockpit.bpms.BpmsApiProperties;
import io.vanillabp.cockpit.config.properties.ApplicationProperties;
import io.vanillabp.cockpit.support.security.jwt.JwtAuthenticationToken;
import io.vanillabp.cockpit.support.security.jwt.JwtAuthenticationTokenMapper;
import io.vanillabp.cockpit.support.security.jwt.JwtMapper;
import io.vanillabp.cockpit.support.security.jwt.JwtProperties;
import io.vanillabp.cockpit.support.security.jwt.JwtSecurityContextRepository;
import io.vanillabp.cockpit.support.security.usercontext.UserDetails;
import io.vanillabp.cockpit.support.security.usercontext.UserDirectoryService;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The servlet security chains of this application: the chain of the BPMS API, and the two GUI chains
 * of which the profile {@code basic-auth} picks one. This asserts which requests they take, in which
 * order they are consulted, where the JWT filter sits inside a GUI chain and what a request without a
 * login gets.
 *
 * <p>None of this fails loudly when it breaks. A JWT filter placed after the authorization filter leaves
 * every request unauthenticated, which looks like a plain 401. A GUI chain consulted before the BPMS-API
 * chain answers adapter requests with a login redirect, which looks like a broken adapter. The chains were
 * rewritten from WebFlux to Spring MVC, so the assertions below exist to show that the rewrite kept the
 * semantics.
 *
 * <p>The test calls the configurations directly and not through a Spring context, because the
 * container's context needs MongoDB, Kafka and a workflow module to come up.
 * {@link #httpSecurity()} builds the same {@code HttpSecurity} Spring Boot would inject, including
 * the configurers it applies by default. Without them the filter list would be missing exactly the
 * filters these assertions are about.
 */
@ExtendWith(SuppressOutputExtension.class)
class SecurityChainsTest {

  /**
   * Any HMAC key of the right length. The chains are built here, not used for cryptography.
   */
  private static final String HMAC_KEY = Base64.getEncoder().encodeToString(new byte[32]);

  private ApplicationProperties applicationProperties() {

    final var jwt = new JwtProperties();
    jwt.setHmacSHA256Base64(HMAC_KEY);

    final var properties = new ApplicationProperties();
    properties.setJwt(jwt);
    properties.setTitleShort("Test Cockpit");
    return properties;

  }

  /**
   * The same set of default configurers Spring Boot's {@code HttpSecurityConfiguration} applies to the
   * prototype it hands to a {@code SecurityFilterChain} bean method.
   */
  private HttpSecurity httpSecurity() {

    final var objectPostProcessor = new ObjectPostProcessor<Object>() {
      @Override
      public <O> O postProcess(
          final O object) {
        // what the post processor of Spring Boot does as well. A filter which is not initialized
        // fails on its first request.
        if (object instanceof InitializingBean bean) {
          try {
            bean.afterPropertiesSet();
          } catch (Exception e) {
            throw new IllegalStateException(e);
          }
        }
        return object;
      }
    };

    final var context = new GenericWebApplicationContext(new MockServletContext());
    context.refresh();

    final var http = new HttpSecurity(
        objectPostProcessor, new AuthenticationManagerBuilder(objectPostProcessor), Map.of(
            ApplicationContext.class, context,
            PathPatternRequestMatcher.Builder.class, PathPatternRequestMatcher.withDefaults()));

    return http
        .csrf(withDefaults())
        .exceptionHandling(withDefaults())
        .headers(withDefaults())
        .sessionManagement(withDefaults())
        .securityContext(withDefaults())
        .requestCache(withDefaults())
        .anonymous(withDefaults())
        .servletApi(withDefaults())
        .logout(withDefaults());

  }

  /**
   * The provider of the profile {@code local}, with addresses nobody answers. Building the chain
   * asks none of them.
   */
  private static ClientRegistrationRepository oneProvider() {

    return new InMemoryClientRegistrationRepository(ClientRegistration
        .withRegistrationId("simulator")
        .clientId("business-cockpit")
        .clientSecret("secret")
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
        .scope("openid")
        .authorizationUri("http://localhost:1/oauth2/authorize")
        .tokenUri("http://localhost:1/oauth2/token")
        .jwkSetUri("http://localhost:1/oauth2/jwks")
        .userNameAttributeName("sub")
        .build());

  }

  private static ObjectProvider<ClientRegistrationRepository> providers(
      final ClientRegistrationRepository... repositories) {

    final var beanFactory = new StaticListableBeanFactory();
    for (var index = 0; index < repositories.length; ++index) {
      beanFactory.addBean("clientRegistrations"
          + index, repositories[index]);
    }
    return beanFactory.getBeanProvider(ClientRegistrationRepository.class);

  }

  private JwtMapper<? extends JwtAuthenticationToken> jwtMapper() {

    return new JwtAuthenticationTokenMapper(applicationProperties().getJwt(), new SingleUserDirectoryService(), Function
        .identity());

  }

  private SecurityFilterChain openIdConnectChain(
      final ObjectProvider<ClientRegistrationRepository> providers) throws Exception {

    final var configuration = new GuiSecurityConfiguration.LoginByOpenIdConnect();
    ReflectionTestUtils.setField(configuration, "properties", applicationProperties());
    final var jwtMapper = jwtMapper();
    final var jwtRepository = new JwtSecurityContextRepository(applicationProperties().getJwt(), jwtMapper);
    final var http = httpSecurity();
    http.setSharedObject(ClientRegistrationRepository.class, providers.getIfAvailable());
    return configuration.guiHttpSecurity(http, jwtRepository, jwtMapper, providers);

  }

  private SecurityFilterChain openIdConnectChain() throws Exception {

    return openIdConnectChain(providers(oneProvider()));

  }

  private SecurityFilterChain httpBasicChain() throws Exception {

    final var configuration = new GuiSecurityConfiguration.LoginByHttpBasic();
    ReflectionTestUtils.setField(configuration, "properties", applicationProperties());
    final var jwtMapper = jwtMapper();
    final var jwtRepository = new JwtSecurityContextRepository(applicationProperties().getJwt(), jwtMapper);
    return configuration.guiHttpSecurity(
        httpSecurity(), jwtRepository, jwtMapper, new SingleUserDirectoryService());

  }

  private List<SecurityFilterChain> bothGuiChains() throws Exception {

    return List.of(openIdConnectChain(), httpBasicChain());

  }

  private SecurityFilterChain bpmsApiChain() throws Exception {

    final var properties = new BpmsApiProperties();
    properties.setRealmName("test");
    properties.setUsername("abc");
    properties.setPassword("{noop}123");

    final var configuration = new BpmsApiSecurityConfiguration();
    ReflectionTestUtils.setField(configuration, "properties", properties);
    return configuration.bpmsApiHttpSecurity(httpSecurity());

  }

  private List<String> filterClassNames(
      final SecurityFilterChain chain) {

    return chain
        .getFilters()
        .stream()
        .map(filter -> filter.getClass().getSimpleName())
        .toList();

  }

  /**
   * The JWT filter has to land behind the filter which logs in, because it reads the security
   * context from the JWT cookie, and ahead of the authorization filter, which decides on 401. The
   * filter set is Spring's, so a Spring upgrade can move the position without any compile error.
   */
  @Test
  void theJwtFilterSitsBetweenTheLoginAndAuthorization() throws Exception {

    assertJwtFilterBetween(filterClassNames(openIdConnectChain()), "OAuth2LoginAuthenticationFilter");
    assertJwtFilterBetween(filterClassNames(httpBasicChain()), "BasicAuthenticationFilter");

  }

  private void assertJwtFilterBetween(
      final List<String> filters,
      final String loginFilter) {

    assertThat(filters).contains("PassiveJwtSecurityFilter", loginFilter, "AuthorizationFilter");
    assertThat(filters.indexOf("PassiveJwtSecurityFilter"))
        .as("the JWT filter must run after the login, otherwise the security context it "
            + "restores is overwritten - filters: %s", filters)
        .isGreaterThan(filters.indexOf(loginFilter));
    assertThat(filters.indexOf("PassiveJwtSecurityFilter"))
        .as("the JWT filter must run before authorization, otherwise every request is anonymous "
            + "and answered with 401 - filters: %s", filters)
        .isLessThan(filters.indexOf("AuthorizationFilter"));

  }

  /**
   * The renewal reads the authentication the JWT filter restored from the cookie, so it has to
   * run after it, and before authorization, which may answer the request.
   */
  @Test
  void theRenewalFilterSitsBehindTheJwtFilter() throws Exception {

    for (final var chain : bothGuiChains()) {
      final var filters = filterClassNames(chain);
      assertThat(filters).contains("JwtRenewalFilter");
      assertThat(filters.indexOf("JwtRenewalFilter"))
          .as("filters: %s", filters)
          .isGreaterThan(filters.indexOf("PassiveJwtSecurityFilter"))
          .isLessThan(filters.indexOf("AuthorizationFilter"));
    }

  }

  /**
   * Anonymous access is switched off in the GUI chains, so a chain must carry no anonymous filter.
   * Otherwise an unauthenticated request would get an anonymous principal instead of a 401, and
   * nothing would say so.
   */
  @Test
  void theGuiChainsHaveNoAnonymousAuthentication() throws Exception {

    for (final var chain : bothGuiChains()) {
      assertThat(filterClassNames(chain))
          .noneMatch(name -> name.contains("Anonymous"));
    }

  }

  @Test
  void theBpmsApiChainOnlyTakesTheBpmsApiPaths() throws Exception {

    final var chain = bpmsApiChain();

    // the two prefixes are "/bpms/api/v1" and "/bpms/api/v1_1", with an underscore and not a dot
    assertThat(matches(chain, "/bpms/api/v1/anything")).isTrue();
    assertThat(matches(chain, "/bpms/api/v1_1/anything")).isTrue();
    assertThat(matches(chain, "/gui/api/v1/app/info")).isFalse();
    assertThat(matches(chain, "/wm/loan-approval/remoteEntry.js")).isFalse();

  }

  /**
   * A GUI chain has no {@code securityMatcher}, so it takes everything, the BPMS-API paths
   * included. That is why the order of the beans decides who answers an adapter request, and why
   * the assertion below is about {@code @Order} and not about matchers.
   */
  @Test
  void theGuiChainsTakeEverythingIncludingTheBpmsApiPaths() throws Exception {

    for (final var chain : bothGuiChains()) {
      assertThat(matches(chain, "/gui/api/v1/app/info")).isTrue();
      assertThat(matches(chain, "/bpms/api/v1/anything")).isTrue();
    }

  }

  /**
   * Both chains match the BPMS-API paths, so Spring Security consults them in bean order. If the GUI chain
   * ever came first, the adapter would receive a login redirect in HTML where it expects JSON.
   */
  @Test
  void theBpmsApiChainIsConsultedBeforeTheGuiChains() throws Exception {

    final var bpmsApiOrder = orderOf(
        BpmsApiSecurityConfiguration.class, "bpmsApiHttpSecurity", HttpSecurity.class);
    final var openIdConnectOrder = orderOf(
        GuiSecurityConfiguration.LoginByOpenIdConnect.class, "guiHttpSecurity", HttpSecurity.class,
        JwtSecurityContextRepository.class, JwtMapper.class, ObjectProvider.class);
    final var httpBasicOrder = orderOf(
        GuiSecurityConfiguration.LoginByHttpBasic.class, "guiHttpSecurity", HttpSecurity.class,
        JwtSecurityContextRepository.class, JwtMapper.class, UserDirectoryService.class);

    assertThat(bpmsApiOrder).isLessThan(openIdConnectOrder);
    assertThat(bpmsApiOrder).isLessThan(httpBasicOrder);

  }

  /**
   * The library's startup check looks for the chains by their bean names, and the wiki shows the
   * same names. The bean name is the method name, so renaming a method here would make the check
   * warn about a chain which is there.
   */
  @Test
  void theChainsCarryTheNamesTheStartupCheckLooksFor() throws Exception {

    assertThat(GuiSecurityConfiguration.LoginByOpenIdConnect.class
        .getDeclaredMethod("guiHttpSecurity", HttpSecurity.class,
            JwtSecurityContextRepository.class, JwtMapper.class, ObjectProvider.class)
        .getAnnotation(Bean.class))
        .isNotNull();
    assertThat(GuiSecurityConfiguration.LoginByHttpBasic.class
        .getDeclaredMethod("guiHttpSecurity", HttpSecurity.class,
            JwtSecurityContextRepository.class, JwtMapper.class, UserDirectoryService.class)
        .getAnnotation(Bean.class))
        .isNotNull();
    assertThat(BpmsApiSecurityConfiguration.class
        .getDeclaredMethod("bpmsApiHttpSecurity", HttpSecurity.class)
        .getAnnotation(Bean.class))
        .isNotNull();

  }

  /**
   * Exactly one of the two GUI chains is there, whatever the profiles are. Two would be two beans
   * of the same name, and none would leave the GUI API to Spring Boot's default chain.
   */
  @Test
  void theProfileBasicAuthSwitchesFromOneChainToTheOther() {

    assertThat(AnnotationUtils.findAnnotation(GuiSecurityConfiguration.LoginByOpenIdConnect.class,
        Profile.class).value())
        .containsExactly("!"
            + GuiSecurityConfiguration.BASIC_AUTH_PROFILE);
    assertThat(AnnotationUtils.findAnnotation(GuiSecurityConfiguration.LoginByHttpBasic.class,
        Profile.class).value())
        .containsExactly(GuiSecurityConfiguration.BASIC_AUTH_PROFILE);

  }

  /**
   * Without a provider, Spring Security would stop the start with a message about a missing bean
   * of its own. The message of the chain says what to configure instead.
   */
  @Test
  void theLoginByOpenIdConnectWithoutAProviderSaysWhatToDo() {

    assertThatThrownBy(() -> openIdConnectChain(providers()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("spring.security.oauth2.client.registration.<id>")
        .hasMessageContaining("'local'")
        .hasMessageContaining("'basic-auth'");

  }

  /**
   * A person who opens a page is sent to the provider. A program which asks the GUI API or the
   * BPMS API gets 401, because a redirect to a login page would not help it.
   */
  @Test
  void aPageGoesToTheProviderAndAnApiAnswers401() throws Exception {

    final var chain = openIdConnectChain();

    final var page = send(chain, "/tasks", "text/html");
    assertThat(page.getStatus()).isEqualTo(HttpStatus.FOUND.value());
    assertThat(page.getRedirectedUrl()).endsWith("/oauth2/authorization/simulator");

    assertThat(send(chain, "/gui/api/v1/usertask", "application/json").getStatus())
        .isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(send(chain, "/gui/api/v1/usertask", "text/html").getStatus())
        .isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(send(chain, "/bpms/api/v1_1/workflow-module/x", "application/json").getStatus())
        .isEqualTo(HttpStatus.UNAUTHORIZED.value());

  }

  /**
   * The first step of the login: the chain sends the browser on to the authorization endpoint of
   * the provider, with the address the provider sends the browser back to, and with PKCE.
   */
  @Test
  void theLoginStartsAtTheAuthorizationEndpointOfTheProvider() throws Exception {

    final var response = send(openIdConnectChain(), "/oauth2/authorization/simulator", "text/html");

    assertThat(response.getStatus()).isEqualTo(HttpStatus.FOUND.value());
    assertThat(response.getRedirectedUrl())
        .startsWith("http://localhost:1/oauth2/authorize?")
        .contains("client_id=business-cockpit")
        .contains("redirect_uri=http://localhost/login/oauth2/code/simulator")
        .contains("code_challenge_method=S256");

  }

  /**
   * The filter which finishes the login at the provider writes the login into the JWT cookie.
   * Spring Security would put the repository of the whole chain there, which keeps the login in
   * the HTTP session. The browser would not notice, but a workflow module behind the proxy would
   * get no cookie and so no user.
   */
  @Test
  void theLoginAtTheProviderIsWrittenIntoTheJwtCookie() throws Exception {

    final var loginFilter = openIdConnectChain()
        .getFilters()
        .stream()
        .filter(filter -> filter instanceof OAuth2LoginAuthenticationFilter)
        .findFirst()
        .orElseThrow();

    assertThat(ReflectionTestUtils.getField(loginFilter, "securityContextRepository"))
        .isInstanceOf(JwtSecurityContextRepository.class);

  }

  private MockHttpServletResponse send(
      final SecurityFilterChain chain,
      final String path,
      final String accept) throws Exception {

    final var request = new MockHttpServletRequest("GET", path);
    request.addHeader("Accept", accept);
    final var response = new MockHttpServletResponse();
    new FilterChainProxy(chain).doFilter(request, response, new MockFilterChain());
    return response;

  }

  private boolean matches(
      final SecurityFilterChain chain,
      final String path) {

    return chain.matches(new MockHttpServletRequest("GET", path));

  }

  private int orderOf(
      final Class<?> configuration,
      final String methodName,
      final Class<?>... parameterTypes) throws Exception {

    final var order = AnnotationUtils.findAnnotation(
        configuration.getDeclaredMethod(methodName, parameterTypes), Order.class);
    assertThat(order).as("%s.%s is expected to declare an order", configuration.getSimpleName(),
        methodName).isNotNull();
    return order.value();

  }

  private static class SingleUserDirectoryService implements UserDirectoryService {

    @Override
    public List<UserDetails> findUsers(
        final String query) {
      return getAllUsers();
    }

    @Override
    public List<UserDetails> findUsers(
        final String query,
        final List<String> excludeUsersIds) {
      return getAllUsers();
    }

    @Override
    public List<UserDetails> getAllUsers() {
      return List.of(new TestUser());
    }

    @Override
    public List<UserDetails> getAllUsers(
        final List<String> excludeUsersIds) {
      return getAllUsers();
    }

    @Override
    public Optional<UserDetails> getUser(
        final String id) {
      return Optional.of(new TestUser());
    }

  }

  private static class TestUser implements UserDetails {

    @Override
    public String getId() {
      return "test";
    }

    @Override
    public String getEmail() {
      return "test@example.com";
    }

    @Override
    public String getDisplay() {
      return "Test User";
    }

    @Override
    public String getDisplayShort() {
      return "Test";
    }

    @Override
    public List<String> getAuthorities() {
      return List.of("ROLE_USER");
    }

  }

}
