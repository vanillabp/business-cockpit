package io.vanillabp.derived.cockpit;

import static io.vanillabp.cockpit.config.web.WebSecurityConfiguration.appInfoRequestMatcher;
import static io.vanillabp.cockpit.config.web.WebSecurityConfiguration.assetsRequestMatcher;
import static io.vanillabp.cockpit.config.web.WebSecurityConfiguration.currentUserRequestMatcher;
import static io.vanillabp.cockpit.config.web.WebSecurityConfiguration.staticRequestMatcher;
import static io.vanillabp.cockpit.config.web.WebSecurityConfiguration.updatesRequestMatcher;
import static io.vanillabp.cockpit.config.web.WebSecurityConfiguration.workflowModulesProxyRequestMatcher;
import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
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
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;

import io.vanillabp.cockpit.bpms.BpmsApiAuthority;
import io.vanillabp.cockpit.config.properties.ApplicationProperties;
import io.vanillabp.cockpit.support.security.jwt.JwtAuthenticationToken;
import io.vanillabp.cockpit.support.security.jwt.JwtMapper;
import io.vanillabp.cockpit.support.security.jwt.JwtRenewalFilter;
import io.vanillabp.cockpit.support.security.jwt.JwtSecurityContextRepository;
import io.vanillabp.cockpit.support.security.jwt.PassiveJwtSecurityFilter;
import io.vanillabp.cockpit.users.UserDetailsProvider;

/**
 * The two security chains of {@link DerivedCockpitApplication}, written the way the wiki page
 * "Security" shows them. The library brings no chain, so every application writes these. The
 * module {@code container} has the longer version of the same thing.
 * <p>
 * The property {@code derived-cockpit.chains} exists for the test of an application which forgot
 * its chains. A real application has no such switch.
 */
@Configuration
@ConditionalOnBooleanProperty(name = "derived-cockpit.chains", matchIfMissing = true)
public class DerivedCockpitSecurity {

  @Bean
  @Order(1)
  public SecurityFilterChain bpmsApiHttpSecurity(
      final HttpSecurity http,
      @Value("${bpms-api.username}") final String username,
      @Value("${bpms-api.password}") final String password) throws Exception {

    final var reportingUser = User.builder()
        .username(username)
        .password(password)
        .roles(BpmsApiAuthority.BPMS_API_ROLE)
        .build();

    return http
        .securityMatcher(pathPattern("/bpms/api/**"))
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
        .authenticationManager(new ProviderManager(
            new DaoAuthenticationProvider(new InMemoryUserDetailsManager(reportingUser))))
        .httpBasic(basic -> basic.securityContextRepository(new NullSecurityContextRepository()))
        .build();

  }

  @Bean
  @Order(99)
  public SecurityFilterChain guiHttpSecurity(
      final HttpSecurity http,
      final ApplicationProperties properties,
      final JwtSecurityContextRepository jwtSecurityContextRepository,
      final JwtMapper<? extends JwtAuthenticationToken> jwtMapper,
      final UserDetailsProvider users) throws Exception {

    // stands in for the login of an identity provider
    final var persons = new InMemoryUserDetailsManager(users
        .getAllUsers()
        .stream()
        .map(user -> User.builder()
            .username(user.getId())
            .password("{noop}"
                + DerivedCockpitApplication.PASSWORD)
            .authorities(user.getAuthorities().stream().map(SimpleGrantedAuthority::new).toList())
            .build())
        .toList());

    return http
        .csrf(AbstractHttpConfigurer::disable)
        .anonymous(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(requests -> requests
            .requestMatchers(appInfoRequestMatcher, currentUserRequestMatcher, assetsRequestMatcher,
                staticRequestMatcher, workflowModulesProxyRequestMatcher)
            .permitAll()
            .anyRequest()
            .authenticated())
        .authenticationManager(new ProviderManager(new DaoAuthenticationProvider(persons)))
        .httpBasic(basic -> basic.securityContextRepository(jwtSecurityContextRepository))
        .addFilterAfter(new PassiveJwtSecurityFilter(properties.getJwt(), jwtMapper),
            BasicAuthenticationFilter.class)
        .addFilterAfter(new JwtRenewalFilter(properties.getJwt(), jwtMapper, updatesRequestMatcher),
            PassiveJwtSecurityFilter.class)
        .build();

  }

}
