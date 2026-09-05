package io.vanillabp.cockpit.config.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.config.Customizer.withDefaults;

import io.vanillabp.cockpit.bpms.BpmsApiProperties;
import io.vanillabp.cockpit.bpms.BpmsApiWebSecurityConfiguration;
import io.vanillabp.cockpit.commons.security.jwt.JwtAuthenticationToken;
import io.vanillabp.cockpit.commons.security.jwt.JwtAuthenticationTokenMapper;
import io.vanillabp.cockpit.commons.security.jwt.JwtMapper;
import io.vanillabp.cockpit.commons.security.jwt.JwtProperties;
import io.vanillabp.cockpit.commons.security.jwt.JwtSecurityContextRepository;
import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.config.properties.ApplicationProperties;
import io.vanillabp.cockpit.users.UserDetailsProvider;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * The two servlet security chains of the cockpit: which requests they take, in which order they are
 * consulted, and where the JWT filter sits inside the GUI chain.
 *
 * <p>None of this fails loudly when it breaks. A JWT filter placed after the authorization filter leaves
 * every request unauthenticated, which looks like a plain 401; a GUI chain consulted before the BPMS-API
 * chain answers adapter requests with a login redirect, which looks like a broken adapter. The chains were
 * rewritten from WebFlux to Spring MVC, so the assertions below exist to show that the rewrite kept the
 * semantics.
 *
 * <p>The configurations are exercised directly rather than through a Spring context: the container's context
 * needs MongoDB, Kafka and a workflow module to come up. {@link #httpSecurity()} builds the same
 * {@code HttpSecurity} Spring Boot would inject, including the configurers it applies by default - without
 * them the filter list would be missing exactly the filters these assertions are about.
 */
class WebSecurityChainTest {

    /**
     * Any HMAC key of the right length; the chains are built, not exercised cryptographically.
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
            public <O> O postProcess(final O object) {
                return object;
            }
        };

        final var context = new GenericWebApplicationContext(new MockServletContext());
        context.refresh();

        final var http = new HttpSecurity(
                objectPostProcessor,
                new AuthenticationManagerBuilder(objectPostProcessor),
                Map.of(
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

    private WebSecurityConfiguration guiConfiguration() {

        final var configuration = new WebSecurityConfiguration();
        ReflectionTestUtils.setField(configuration, "properties", applicationProperties());
        return configuration;

    }

    private SecurityFilterChain guiChain() throws Exception {

        final var configuration = guiConfiguration();
        final var jwtProperties = applicationProperties().getJwt();
        final JwtMapper<? extends JwtAuthenticationToken> jwtMapper =
                new JwtAuthenticationTokenMapper(jwtProperties);
        final var jwtRepository = new JwtSecurityContextRepository(jwtProperties, jwtMapper);
        return configuration.guiHttpSecurity(
                httpSecurity(), jwtRepository, jwtMapper, new SingleUserDetailsProvider());

    }

    private SecurityFilterChain bpmsApiChain() throws Exception {

        final var properties = new BpmsApiProperties();
        properties.setRealmName("test");
        properties.setUsername("abc");
        properties.setPassword("{noop}123");

        final var configuration = new BpmsApiWebSecurityConfiguration();
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
     * {@code addFilterAfter(.., BasicAuthenticationFilter.class)} has to land the JWT filter behind the
     * authentication filter - it reads the security context from the JWT cookie - and ahead of the
     * authorization filter, which is what decides on 401. The filter set is Spring's, so a Spring upgrade
     * can move the position without any compile error.
     */
    @Test
    void theJwtFilterSitsBetweenAuthenticationAndAuthorization() throws Exception {

        final var filters = filterClassNames(guiChain());

        assertThat(filters).contains(
                "PassiveJwtSecurityFilter", "BasicAuthenticationFilter", "AuthorizationFilter");
        assertThat(filters.indexOf("PassiveJwtSecurityFilter"))
                .as("the JWT filter must run after authentication, otherwise the security context it "
                        + "restores is overwritten - filters: %s", filters)
                .isGreaterThan(filters.indexOf("BasicAuthenticationFilter"));
        assertThat(filters.indexOf("PassiveJwtSecurityFilter"))
                .as("the JWT filter must run before authorization, otherwise every request is anonymous "
                        + "and answered with 401 - filters: %s", filters)
                .isLessThan(filters.indexOf("AuthorizationFilter"));

    }

    /**
     * Anonymous access is switched off in the GUI chain, so the chain must carry no anonymous filter -
     * otherwise unauthenticated requests would silently get an anonymous principal instead of a 401.
     */
    @Test
    void theGuiChainHasNoAnonymousAuthentication() throws Exception {

        assertThat(filterClassNames(guiChain()))
                .noneMatch(name -> name.contains("Anonymous"));

    }

    @Test
    void theBpmsApiChainOnlyTakesTheBpmsApiPaths() throws Exception {

        final var chain = bpmsApiChain();

        // the two prefixes are "/bpms/api/v1" and "/bpms/api/v1_1" - an underscore, not a dot
        assertThat(matches(chain, "/bpms/api/v1/anything")).isTrue();
        assertThat(matches(chain, "/bpms/api/v1_1/anything")).isTrue();
        assertThat(matches(chain, "/gui/api/v1/app/info")).isFalse();
        assertThat(matches(chain, "/wm/loan-approval/remoteEntry.js")).isFalse();

    }

    /**
     * The GUI chain has no {@code securityMatcher}, so it takes everything - including the BPMS-API paths.
     * That is why the order of the two beans decides who answers an adapter request, and why the assertion
     * below is about {@code @Order} rather than about matchers.
     */
    @Test
    void theGuiChainTakesEverythingIncludingTheBpmsApiPaths() throws Exception {

        final var chain = guiChain();

        assertThat(matches(chain, "/gui/api/v1/app/info")).isTrue();
        assertThat(matches(chain, "/bpms/api/v1/anything")).isTrue();

    }

    /**
     * Both chains match the BPMS-API paths, so Spring Security consults them in bean order. If the GUI chain
     * ever came first, the adapter would receive a login redirect in HTML where it expects JSON.
     */
    @Test
    void theBpmsApiChainIsConsultedBeforeTheGuiChain() throws Exception {

        final var bpmsApiOrder = orderOf(
                BpmsApiWebSecurityConfiguration.class, "bpmsApiHttpSecurity", HttpSecurity.class);
        final var guiOrder = orderOf(
                WebSecurityConfiguration.class, "guiHttpSecurity", HttpSecurity.class,
                JwtSecurityContextRepository.class, JwtMapper.class, UserDetailsProvider.class);

        assertThat(bpmsApiOrder).isLessThan(guiOrder);

    }

    /**
     * Both extension points are declared with {@code @ConditionalOnMissingBean(name = ..)}, which ties them
     * to the <b>bean name</b> - that is, to the method name. Renaming the method would leave the condition
     * pointing at a name nobody defines, so an application's override would stop taking effect without any
     * error. These names are public API for cockpit applications.
     */
    @Test
    void theNamedExtensionPointsStillMatchTheirMethodNames() throws Exception {

        assertThat(WebSecurityConfiguration.class
                .getDeclaredMethod("guiHttpSecurity", HttpSecurity.class,
                        JwtSecurityContextRepository.class, JwtMapper.class, UserDetailsProvider.class)
                .getAnnotation(org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean.class)
                .name())
                .containsExactly("guiHttpSecurity");

        assertThat(WebSecurityConfiguration.class
                .getDeclaredMethod("userDetailsProvider")
                .getAnnotation(org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean.class)
                .name())
                .containsExactly("userDetailsProvider");

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

    private static class SingleUserDetailsProvider implements UserDetailsProvider {

        @Override
        public List<UserDetails> findUsers(final String query) {
            return getAllUsers();
        }

        @Override
        public List<UserDetails> findUsers(final String query, final List<String> excludeUsersIds) {
            return getAllUsers();
        }

        @Override
        public List<UserDetails> getAllUsers() {
            return List.of(new TestUser());
        }

        @Override
        public List<UserDetails> getAllUsers(final List<String> excludeUsersIds) {
            return getAllUsers();
        }

        @Override
        public Optional<UserDetails> getUser(final String id) {
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
