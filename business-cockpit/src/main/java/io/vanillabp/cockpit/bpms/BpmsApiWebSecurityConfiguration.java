package io.vanillabp.cockpit.bpms;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
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

public class BpmsApiWebSecurityConfiguration {

    public static final String BPMS_API_ROLE = "BPMS-API";

    public static final String BPMS_API_AUTHORITY = "ROLE_" + BPMS_API_ROLE;

	@Autowired
	private BpmsApiProperties properties;

    @Bean
    @Order(1)
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
    public InMemoryUserDetailsManager bpmsApiUserDetailsService() {

        final var user = User.builder()
                .username(properties.getUsername())
                .password(properties.getPassword())
                .roles(BPMS_API_ROLE)
                .build();
        return new InMemoryUserDetailsManager(user);

    }

}
