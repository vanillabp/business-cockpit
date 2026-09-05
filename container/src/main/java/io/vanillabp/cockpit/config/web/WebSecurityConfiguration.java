package io.vanillabp.cockpit.config.web;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

import io.vanillabp.cockpit.bpms.BpmsApiWebSecurityConfiguration;
import io.vanillabp.cockpit.commons.security.jwt.JwtAuthenticationToken;
import io.vanillabp.cockpit.commons.security.jwt.JwtAuthenticationTokenMapper;
import io.vanillabp.cockpit.commons.security.jwt.JwtLogoutSuccessHandler;
import io.vanillabp.cockpit.commons.security.jwt.JwtMapper;
import io.vanillabp.cockpit.commons.security.jwt.JwtSecurityContextRepository;
import io.vanillabp.cockpit.commons.security.jwt.JwtUserDetailsProvider;
import io.vanillabp.cockpit.commons.security.jwt.PassiveJwtSecurityFilter;
import io.vanillabp.cockpit.config.properties.ApplicationProperties;
import io.vanillabp.cockpit.users.UserDetailsProvider;
import io.vanillabp.cockpit.workflowmodules.GroupHierarchyService;
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

@Configuration
@Import(BpmsApiWebSecurityConfiguration.class)
public class WebSecurityConfiguration {

    public static final RequestMatcher appInfoRequestMatcher = pathPattern(
            "/gui/api/v1/app/info");

    public static final RequestMatcher currentUserRequestMatcher = pathPattern(
            "/gui/api/v1/app/current-user");

    public static final RequestMatcher assetsRequestMatcher = pathPattern(
            "/assets/**");

    public static final RequestMatcher staticRequestMatcher = pathPattern(
            "/static/**");

    public static final RequestMatcher workflowModulesProxyRequestMatcher = pathPattern(
            "/wm/**");

    @Autowired
    private ApplicationProperties properties;

    /**
     * Authenticates a request from the JWT cookie whenever it carries one. Placed behind the basic
     * authentication filter, so the login request (which has no cookie yet) authenticates by basic
     * auth and every later request by its cookie.
     */
    private PassiveJwtSecurityFilter jwtSecurityFilter(
            final JwtMapper<? extends JwtAuthenticationToken> jwtMapper) {

        return new PassiveJwtSecurityFilter(properties.getJwt(), jwtMapper);

    }

    @Bean
    @ConditionalOnMissingBean
    public JwtMapper<? extends JwtAuthenticationToken> jwtMapper(
            final GroupHierarchyService groupHierarchyService) {

        return new JwtAuthenticationTokenMapper(
                properties.getJwt(),
                groupHierarchyService::resolveGroups);

    }

    @Bean
    public JwtSecurityContextRepository jwtSecurityContextRepository(
            final JwtMapper<? extends AbstractAuthenticationToken> jwtMapper) {

        return new JwtSecurityContextRepository(
                properties.getJwt(),
                jwtMapper);

    }

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
                .addFilterAfter(jwtSecurityFilter(jwtMapper), BasicAuthenticationFilter.class);

        return http.build();

    }

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

    @Bean
    @ConditionalOnMissingBean(name = "userDetailsProvider")
    public io.vanillabp.cockpit.commons.security.usercontext.UserDetailsProvider userDetailsProvider() {

        return new JwtUserDetailsProvider();

    }

}
