package io.vanillabp.cockpit.support.security.jwt;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

import org.eclipse.microprofile.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.security.identity.IdentityProviderManager;
import io.vanillabp.cockpit.support.security.usercontext.UserDirectoryIsMissing;
import io.vanillabp.cockpit.support.security.usercontext.UserDirectoryService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/**
 * Ends the start of an application which could not check the cockpit's login token, so the
 * developer hears about it at once and not from the first user who gets {@code 401}:
 * <ul>
 * <li>a key below {@code business-cockpit.} which nobody reads, with the key that was probably
 * meant. The keys which say how the cookie is written belong to the cockpit alone, and they are
 * refused with that reason;</li>
 * <li>no secret, or one which is no Base64 or too short;</li>
 * <li>no {@link UserDirectoryService}, which gives the groups of a user;</li>
 * <li>a secret in an application without {@code quarkus-security}, which could not check a token
 * with it.</li>
 * </ul>
 * An application without {@code quarkus-security} and without a secret starts. It has the support
 * for its exceptions only, so there is no token to check, and one line in the log says so.
 * The build property {@value JwtSettings#ENABLED_KEY}{@code =false} leaves this check out of the
 * application, together with the check of the token.
 */
@ApplicationScoped
@IfBuildProperty(name = JwtSettings.ENABLED_KEY, stringValue = "true", enableIfMissing = true)
public class JwtStartupCheck {

  private static final Logger logger = LoggerFactory.getLogger(JwtStartupCheck.class);

  private static final String TOP = "business-cockpit.";

  private static final List<String> KNOWN_KEYS = List.of(
      JwtSettings.SECRET_KEY,
      JwtSettings.COOKIE_NAME_KEY,
      JwtSettings.ENABLED_KEY);

  private static final List<String> KEYS_ONLY_THE_COCKPIT_READS = List.of(
      JwtSettings.PREFIX
          + ".cookie.domain",
      JwtSettings.PREFIX
          + ".cookie.path",
      JwtSettings.PREFIX
          + ".cookie.same-site",
      JwtSettings.PREFIX
          + ".cookie.secure",
      JwtSettings.PREFIX
          + ".cookie.expires-duration",
      JwtSettings.PREFIX
          + ".cookie.max-login-duration");

  /**
   * Up to this many single-character edits, a key counts as a misspelling of a known key.
   */
  private static final int NEAREST = 4;

  @Inject
  Config config;

  @Inject
  JwtSettings settings;

  @Inject
  Instance<UserDirectoryService> userDirectory;

  /**
   * A bean of the extension {@code quarkus-security}. Without that extension the application cannot
   * check a token, because nothing would ask the mechanism.
   */
  @Inject
  Instance<IdentityProviderManager> securityLayer;

  /**
   * Creates the check. Quarkus calls this constructor.
   */
  public JwtStartupCheck() {
  }

  void check(
      @Observes final StartupEvent event) {

    refuseKeysNobodyReads();
    if (securityLayer.isUnsatisfied()) {
      refuseASecretWhichNobodyChecks();
      return;
    }
    refuseAMissingSecret();
    refuseAMissingDirectory();

  }

  private void refuseKeysNobodyReads() {

    final var defects = new ArrayList<String>();
    for (final var propertyName : new TreeSet<String>(namesOf(config.getPropertyNames()))) {
      final var key = withoutAProfile(propertyName);
      if (!key.startsWith(TOP) || KNOWN_KEYS.contains(key)) {
        continue;
      }
      defects.add(defectOf(propertyName, key));
    }
    if (defects.isEmpty()) {
      return;
    }
    throw new IllegalStateException("""
        The application sets keys of the Business Cockpit which a workflow module does not read, \
        so it does not start:
        %s
        A workflow module on Quarkus reads only these keys of the cockpit: %s."""
        .formatted(String.join("\n", defects), String.join(", ", KNOWN_KEYS)));

  }

  private static String defectOf(
      final String propertyName,
      final String key) {

    if (KEYS_ONLY_THE_COCKPIT_READS.contains(key)) {
      return """
            - '%s' says how the cookie is written, and only the cockpit writes the cookie. A \
          workflow module reads it only. Remove the key here and keep it in the settings of the \
          cockpit."""
          .formatted(propertyName);
    }
    final var nearest = nearest(key);
    if (nearest != null) {
      return "  - '%s' is no setting of the Business Cockpit. Did you mean '%s'?".formatted(propertyName, nearest);
    }
    return "  - '%s' is no setting of the Business Cockpit.".formatted(propertyName);

  }

  private void refuseASecretWhichNobodyChecks() {

    if (settings.hmacSha256Base64().isPresent()) {
      throw new IllegalStateException("""
          The application sets '%s', but it cannot check the cockpit's login token: the Quarkus \
          extension quarkus-security is missing. Add the dependency 'io.quarkus:quarkus-security' \
          to check the token. An application which does not read the token removes the key and \
          sets the build property '%s=false'."""
          .formatted(JwtSettings.SECRET_KEY, JwtSettings.ENABLED_KEY));
    }
    logger.info(
        "The cockpit's login token is not checked, because the application has no quarkus-security. "
            + "Add the dependency 'io.quarkus:quarkus-security' to check it, or set the build property "
            + "'{}=false' to say that this application does not need it.",
        JwtSettings.ENABLED_KEY);

  }

  private void refuseAMissingSecret() {

    final var secret = settings.hmacSha256Base64();
    if (secret.isEmpty() || secret.get().isBlank()) {
      throw new IllegalStateException(
          LoginTokenVerifier.explainTheMissingSecret(JwtSettings.SECRET_KEY)
              + " "
              + """
                  Set it to the secret of the cockpit, so this workflow module accepts the tokens \
                  the cockpit signs. An application which does not read the cockpit's login token \
                  leaves the check out with the build property '%s=false'."""
                  .formatted(JwtSettings.ENABLED_KEY));
    }
    final byte[] decoded;
    try {
      decoded = Base64.getDecoder().decode(secret.get());
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("""
          The value of '%s' is no Base64: %s. Copy the value the cockpit has.""".formatted(
          JwtSettings.SECRET_KEY, e.getMessage()), e);
    }
    try {
      new LoginTokenVerifier(decoded);
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("'%s': %s".formatted(JwtSettings.SECRET_KEY, e.getMessage()), e);
    }

  }

  private void refuseAMissingDirectory() {

    if (!userDirectory.isUnsatisfied()) {
      return;
    }
    throw new IllegalStateException(
        UserDirectoryIsMissing.whatIsWrong(
            "The check of the cockpit's login token in business-cockpit-quarkus-support needs one.")
            + "\n\n"
            + UserDirectoryIsMissing.whatToDo("""
                @ApplicationScoped
                public class UserDirectory implements UserDirectoryService {
                    ...
                }
                """));

  }

  private static List<String> namesOf(
      final Iterable<String> propertyNames) {

    final var result = new ArrayList<String>();
    propertyNames.forEach(result::add);
    return result;

  }

  /**
   * A key which holds for one profile is the same key: {@code %dev.business-cockpit.jwt.path} is as
   * wrong as {@code business-cockpit.jwt.path}.
   */
  private static String withoutAProfile(
      final String propertyName) {

    if (!propertyName.startsWith("%")) {
      return propertyName;
    }
    final var name = propertyName.indexOf('.');
    return name < 0
        ? propertyName
        : propertyName.substring(name + 1);

  }

  private static String nearest(
      final String key) {

    var nearest = (String) null;
    var distance = NEAREST + 1;
    for (final var candidate : KNOWN_KEYS) {
      final var edits = edits(key.toLowerCase(Locale.ROOT), candidate.toLowerCase(Locale.ROOT));
      if (edits < distance) {
        distance = edits;
        nearest = candidate;
      }
    }
    return nearest;

  }

  /**
   * How many single-character edits turn one key into the other.
   */
  private static int edits(
      final String one,
      final String other) {

    var previous = new int[other.length() + 1];
    for (var column = 0; column <= other.length(); column++) {
      previous[column] = column;
    }
    for (var row = 1; row <= one.length(); row++) {
      final var current = new int[other.length() + 1];
      current[0] = row;
      for (var column = 1; column <= other.length(); column++) {
        current[column] = Math.min(
            Math.min(current[column - 1] + 1, previous[column] + 1),
            previous[column - 1] + (one.charAt(row - 1) == other.charAt(column - 1) ? 0 : 1));
      }
      previous = current;
    }
    return previous[other.length()];

  }

}
