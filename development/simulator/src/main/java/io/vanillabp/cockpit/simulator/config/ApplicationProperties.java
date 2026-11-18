package io.vanillabp.cockpit.simulator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.vanillabp.cockpit.support.security.jwt.JwtProperties;

/**
 * The simulator's own properties, read from the prefix {@code application}.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

  private JwtProperties jwt;

  /**
   * Creates empty properties. Spring Boot fills them by the setters.
   */
  public ApplicationProperties() {
  }

  /**
   * The settings of the JSON Web Tokens (JWT) which authenticate the users of the simulator.
   *
   * @return The JWT settings
   */
  public JwtProperties getJwt() {
    return jwt;
  }

  /**
   * Sets the settings of the JSON Web Tokens.
   *
   * @param jwt The JWT settings
   */
  public void setJwt(
      JwtProperties jwt) {
    this.jwt = jwt;
  }

}
