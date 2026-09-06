package io.vanillabp.cockpit.bpms;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

import io.vanillabp.cockpit.config.startup.BpmsApiIsConfigured;
import io.vanillabp.cockpit.config.startup.BpmsApiIsNotConfigured;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

/**
 * Secures the BPMS API by HTTP basic against the one client the application configures for its BPMS
 * adapters.
 * <p>
 * The conditions keep an application which has not configured that client yet startable: the chain
 * and its user are simply absent, requests to the BPMS API paths fall through to the GUI chain and
 * are answered with 401, and the startup check explains on every start what to add. Before that the
 * missing realm or missing credentials ended the start in an exception from Spring Security.
 */
public class BpmsApiWebSecurityConfiguration {

    public static final String BPMS_API_ROLE = "BPMS-API";

    public static final String BPMS_API_AUTHORITY = "ROLE_" + BPMS_API_ROLE;

	@Autowired
	private BpmsApiProperties properties;

    @Bean
    @Order(1)
    @Conditional(BpmsApiIsConfigured.class)
    public SecurityFilterChain bpmsApiHttpSecurity(
            final HttpSecurity http) throws Exception {

        final var basicEntryPoint = new BasicAuthenticationEntryPoint();
        basicEntryPoint.setRealmName(properties.getRealmName());
        basicEntryPoint.afterPropertiesSet();

        return http
                .securityMatcher(new OrRequestMatcher(
                        pathPattern(io.vanillabp.cockpit.bpms.api.v1.BpmsApiController.BPMS_API_URL_PREFIX + "/**"),
                        pathPattern(io.vanillabp.cockpit.bpms.api.v1_1.BpmsApiController.BPMS_API_URL_PREFIX + "/**")))
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(requests -> requests
                        .anyRequest()
                        .authenticated())
                .authenticationManager(new ProviderManager(
                        new DaoAuthenticationProvider(bpmsApiUserDetailsService())))
                .httpBasic(basic -> basic
                        // reporting systems authenticate on every single request, so nothing is
                        // carried over between them
                        .securityContextRepository(new NullSecurityContextRepository())
                        .authenticationEntryPoint(basicEntryPoint))
                .build();

    }

    @Bean
    @Conditional(BpmsApiIsConfigured.class)
    public InMemoryUserDetailsManager bpmsApiUserDetailsService() {

        final var user = User.builder()
                .username(properties.getUsername())
                .password(properties.getPassword())
                .roles(BPMS_API_ROLE)
                .build();
        return new InMemoryUserDetailsManager(user);

    }

    /**
     * Stands in for the BPMS API's user while that one is unconfigured. Without any
     * {@code UserDetailsService} in the context Spring Boot invents a user, prints its generated
     * password and warns about running that way in production - three lines of noise that have
     * nothing to do with the missing configuration the startup check is guiding towards. Nobody
     * authenticates against this one: the GUI chain brings its own users.
     */
    @Bean
    @Conditional(BpmsApiIsNotConfigured.class)
    public InMemoryUserDetailsManager noUsersUntilTheBpmsApiIsConfigured() {

        return new InMemoryUserDetailsManager();

    }

}
