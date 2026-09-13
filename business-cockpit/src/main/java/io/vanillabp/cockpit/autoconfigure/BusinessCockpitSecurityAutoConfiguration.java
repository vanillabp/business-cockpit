package io.vanillabp.cockpit.autoconfigure;

import io.vanillabp.cockpit.config.web.WebSecurityConfiguration;
import io.vanillabp.cockpit.users.UserLoginUpsertConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * The two security chains of the cockpit, one for the user interface and one for the BPMS API, plus
 * the filter which records a user's login.
 * <p>
 * Ordered ahead of Spring Boot's {@link UserDetailsServiceAutoConfiguration}, and through it ahead
 * of Spring Boot's own web security, because both of those stand aside for what is already there.
 * The cockpit brings a {@code SecurityFilterChain} and a set of users, so being first is what keeps
 * Spring Boot from adding a chain of its own and from inventing a user with a generated password.
 * Being last would produce both, which is a configuration nobody wrote and which is different from
 * one start to the next.
 */
@AutoConfiguration(before = UserDetailsServiceAutoConfiguration.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
@Import({
    WebSecurityConfiguration.class,
    UserLoginUpsertConfiguration.class
})
public class BusinessCockpitSecurityAutoConfiguration {

}
