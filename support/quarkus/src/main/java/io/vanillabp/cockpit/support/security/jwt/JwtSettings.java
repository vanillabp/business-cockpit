package io.vanillabp.cockpit.support.security.jwt;

import java.util.Optional;

import org.eclipse.microprofile.config.Config;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * The settings a Quarkus workflow module needs to check the cockpit's login token. The keys are
 * those of the cockpit, below {@code business-cockpit.jwt}, so the same values work on both sides.
 * As everywhere in Quarkus, an environment variable sets a key as well, for example
 * {@code BUSINESS_COCKPIT_JWT_HMACSHA256_BASE64}.
 * <p>
 * Only the keys which check a token are here. The cockpit writes the cookie, so the keys which say
 * how it is written, for example {@code cookie.secure}, belong to the cockpit alone.
 * {@link JwtStartupCheck} refuses them.
 * <p>
 * The keys are read one by one and not by a {@code @ConfigMapping}. A mapping makes Quarkus refuse
 * every other key below its prefix while it reads the configuration, before any bean starts, and
 * with a message which does not say what was meant. So {@link JwtStartupCheck} could not say that a
 * key is written by the cockpit only, nor which key a misspelled one was meant to be.
 */
@ApplicationScoped
public class JwtSettings {

  /**
   * Where the keys of the login token start.
   */
  public static final String PREFIX = "business-cockpit.jwt";

  /**
   * The full name of the key which holds the secret the cockpit signs its tokens with, Base64
   * encoded.
   */
  public static final String SECRET_KEY = PREFIX
      + ".hmacSHA256-base64";

  /**
   * The full name of the key which holds the name of the cookie.
   */
  public static final String COOKIE_NAME_KEY = PREFIX
      + ".cookie.name";

  /**
   * The name of the cookie if the key is not set, the same as in the cockpit.
   */
  public static final String DEFAULT_COOKIE_NAME = "bc";

  /**
   * The full name of the build property which switches the check of the login token off.
   */
  public static final String ENABLED_KEY = PREFIX
      + ".enabled";

  @Inject
  Config config;

  private Optional<String> secret;

  private String cookieName;

  /**
   * Creates the settings. Quarkus calls this constructor.
   */
  public JwtSettings() {
  }

  @PostConstruct
  void read() {

    secret = config
        .getOptionalValue(SECRET_KEY, String.class)
        .filter(value -> !value.isBlank());
    cookieName = config
        .getOptionalValue(COOKIE_NAME_KEY, String.class)
        .orElse(DEFAULT_COOKIE_NAME);

  }

  /**
   * The secret the cockpit signs its tokens with, Base64 encoded. It has no default. Without it, the
   * application does not start, see {@link JwtStartupCheck}.
   *
   * @return The secret, Base64 encoded, or empty if it is not set
   */
  public Optional<String> hmacSha256Base64() {

    return secret;

  }

  /**
   * The name of the cookie which carries the token, {@value #DEFAULT_COOKIE_NAME} as in the cockpit
   * if it is not set.
   *
   * @return The name
   */
  public String cookieName() {

    return cookieName;

  }

}
