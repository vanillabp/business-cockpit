package io.vanillabp.cockpit.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Import;

import io.vanillabp.cockpit.config.web.WebSecurityConfiguration;
import io.vanillabp.cockpit.users.UserLoginUpsertConfiguration;

/**
 * What the library adds to the security of an application: the JWT cookie a login is carried in,
 * and the filter which records a user's login.
 * <p>
 * There is no security chain and no user here. The application writes those, and the module
 * {@code container} is the reference. Without a chain of the application's own, Spring Boot adds
 * its default chain, which asks for a login on every path with a password it generates. That
 * keeps an application closed which forgot its chains, and the startup check says what to add.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = Type.SERVLET)
@Import({
    WebSecurityConfiguration.class, UserLoginUpsertConfiguration.class
})
public class BusinessCockpitSecurityAutoConfiguration {

  /**
   * Called by Spring Boot. Everything this configuration adds comes from its annotations.
   */
  public BusinessCockpitSecurityAutoConfiguration() {

  }

}
