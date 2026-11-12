package io.vanillabp.cockpit.config.startup;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;

import io.vanillabp.cockpit.users.directory.KeycloakUserId;
import io.vanillabp.cockpit.users.directory.UserDirectoryProperties;
import io.vanillabp.cockpit.users.directory.UserDirectoryType;

/**
 * Checks the settings of the user directory the cockpit brings, {@code business-cockpit.user-directory.*},
 * before any bean is built.
 * <p>
 * Without these checks a missing value would show up only at the first request of a logged-in
 * user, because only then the cockpit asks the directory. A missing LDAP library would not show up
 * at all: the directory would just not be built, and the start would fail with a message about a
 * missing bean.
 */
final class UserDirectorySettings {

  static final String TYPE = UserDirectoryProperties.PREFIX
      + ".type";

  static final String CACHE_DURATION = UserDirectoryProperties.PREFIX
      + ".cache-duration";

  static final String KEYCLOAK = UserDirectoryProperties.PREFIX
      + ".keycloak";

  static final String LDAP = UserDirectoryProperties.PREFIX
      + ".ldap";

  /**
   * A class of Spring LDAP, which the LDAP directory needs and the cockpit does not bring along.
   */
  static final String LDAP_LIBRARY_CLASS = "org.springframework.ldap.core.LdapTemplate";

  private static final String LDAP_DEPENDENCY = """
      the dependency, without a version, because the Spring Boot BOM names it:

              <dependency>
                <groupId>org.springframework.ldap</groupId>
                <artifactId>spring-ldap-core</artifactId>
              </dependency>""";

  private UserDirectorySettings() {
  }

  /**
   * The directory the configuration asks for.
   *
   * @param environment The configuration of the application
   * @return The type, or empty if none is set or the value names no directory
   */
  static Optional<UserDirectoryType> typeIn(
      final Environment environment) {

    return CockpitConfiguration
        .valueOf(environment, TYPE)
        .flatMap(UserDirectorySettings::typeOf);

  }

  private static Optional<UserDirectoryType> typeOf(
      final String value) {

    return Arrays
        .stream(UserDirectoryType.values())
        .filter(type -> type.name().equalsIgnoreCase(value.trim()))
        .findFirst();

  }

  /**
   * Everything which keeps the configured directory from working.
   *
   * @param environment The configuration of the application
   * @param classLoader The class loader of the application, to look for the LDAP library
   * @return One block per problem, empty if there is none or no directory is asked for
   */
  static List<MissingConfiguration> whatIsWrongWith(
      final Environment environment,
      final ClassLoader classLoader) {

    final var configured = CockpitConfiguration.valueOf(environment, TYPE);
    if (configured.isEmpty()) {
      return List.of();
    }

    final var type = typeOf(configured.get());
    if (type.isEmpty()) {
      return List.of(new MissingConfiguration(
          TYPE, "The user directory the cockpit brings. The value '"
              + configured.get()
              + "' names none. Write 'keycloak' or 'ldap', or leave the property out and add a bean "
              + "of type 'UserDirectoryService' of your own.", TYPE
                  + ": keycloak"));
    }

    final var wrong = new ArrayList<MissingConfiguration>();
    cacheDurationProblem(environment).ifPresent(wrong::add);
    switch (type.get()) {
      case KEYCLOAK -> wrong.addAll(keycloakProblems(environment));
      case LDAP -> wrong.addAll(ldapProblems(environment, classLoader));
    }
    return wrong;

  }

  private static Optional<MissingConfiguration> cacheDurationProblem(
      final Environment environment) {

    final var configured = CockpitConfiguration.valueOf(environment, CACHE_DURATION);
    if (configured.isEmpty()) {
      return Optional.empty();
    }
    try {
      final var duration = Binder
          .get(environment)
          .bind(ConfigurationPropertyName.adapt(CACHE_DURATION, '.'), Bindable.of(Duration.class))
          .get();
      if (!duration.isNegative()) {
        return Optional.empty();
      }
    } catch (RuntimeException e) {
      // not a duration, reported below
    }
    return Optional.of(new MissingConfiguration(
        CACHE_DURATION, "How long the directory keeps a user with its groups before it asks again. The value '"
            + configured.get()
            + "' is no duration of zero or more. Zero switches the cache off.", CACHE_DURATION
                + ": "
                + UserDirectoryProperties.DEFAULT_CACHE_DURATION));

  }

  private static List<MissingConfiguration> keycloakProblems(
      final Environment environment) {

    final var wrong = new ArrayList<MissingConfiguration>();
    missing(environment, KEYCLOAK
        + ".server-url", "The address of Keycloak, without the realm.", "https://login.example.com")
        .ifPresent(wrong::add);
    missing(environment, KEYCLOAK
        + ".realm", "The realm which holds the users of the cockpit.", "business-cockpit")
        .ifPresent(wrong::add);
    missing(environment, KEYCLOAK
        + ".client-id",
        "The client the cockpit logs in with to read the users. It needs 'Client "
            + "authentication' and 'Service accounts roles' switched on, and its service account the "
            + "role 'view-users' of the client 'realm-management'. Newer releases of Keycloak also "
            + "need the client scope 'kc-admin-api' among its default client scopes.",
        "business-cockpit-directory")
        .ifPresent(wrong::add);
    missing(environment, KEYCLOAK
        + ".client-secret",
        "The secret of that client. Take it from the environment, not from a file "
            + "in the repository.",
        "${KEYCLOAK_DIRECTORY_SECRET}")
        .ifPresent(wrong::add);

    final var userId = CockpitConfiguration.valueOf(environment, KEYCLOAK
        + ".user-id");
    if (userId.isPresent() && Arrays
        .stream(KeycloakUserId.values())
        .noneMatch(value -> value.name().equalsIgnoreCase(userId.get().trim()))) {
      wrong.add(new MissingConfiguration(
          KEYCLOAK
              + ".user-id", "Which value of a Keycloak user is its id in the cockpit: 'username' if the "
                  + "login names the user by 'preferred_username', 'id' if it names the user by 'sub'. "
                  + "The value '"
                  + userId.get()
                  + "' is neither.", KEYCLOAK
                      + ".user-id: username"));
    }
    return wrong;

  }

  private static List<MissingConfiguration> ldapProblems(
      final Environment environment,
      final ClassLoader classLoader) {

    final var wrong = new ArrayList<MissingConfiguration>();
    if (!ClassUtils.isPresent(LDAP_LIBRARY_CLASS, classLoader)) {
      wrong.add(new MissingConfiguration(
          TYPE, "The LDAP directory reads with Spring LDAP, and the cockpit does not bring it along, so "
              + "nobody gets it who does not use it. Add "
              + LDAP_DEPENDENCY, TYPE
                  + ": ldap, together with the dependency"));
    }
    missing(environment, LDAP
        + ".url", "The address of the LDAP directory.", "ldaps://ldap.example.com:636")
        .ifPresent(wrong::add);

    final var bindDn = CockpitConfiguration.valueOf(environment, LDAP
        + ".bind-dn");
    final var bindPassword = CockpitConfiguration.valueOf(environment, LDAP
        + ".bind-password");
    if (bindDn.isPresent() && bindPassword.isEmpty()) {
      wrong.add(new MissingConfiguration(
          LDAP
              + ".bind-password", "The password of '"
                  + bindDn.get()
                  + "', which the cockpit logs in with. Take it from the environment, not from a "
                  + "file in the repository.", LDAP
                      + ".bind-password: ${LDAP_BIND_PASSWORD}"));
    }
    if (bindDn.isEmpty() && bindPassword.isPresent()) {
      wrong.add(new MissingConfiguration(
          LDAP
              + ".bind-dn", "The DN the cockpit logs in with. A password is set, but no DN to use "
                  + "it with. Leave both out to read the directory anonymously.", LDAP
                      + ".bind-dn: cn=cockpit,ou=services,dc=example,dc=com"));
    }
    return wrong;

  }

  private static Optional<MissingConfiguration> missing(
      final Environment environment,
      final String propertyName,
      final String purpose,
      final String exampleValue) {

    if (!CockpitConfiguration.isMissing(environment, propertyName)) {
      return Optional.empty();
    }
    return Optional.of(new MissingConfiguration(
        propertyName, purpose, propertyName
            + ": "
            + exampleValue));

  }

  /**
   * The sections of the directories which are configured but not read, because the type names
   * another directory or none. Somebody who wrote them most likely expects them to be used.
   *
   * @param environment The configuration of the application
   * @return The prefixes of the sections which are not read
   */
  static List<String> sectionsWhichAreNotRead(
      final Environment environment) {

    final var type = typeIn(environment);
    final var notRead = new ArrayList<String>();
    if (hasValuesBelow(environment, KEYCLOAK) && (type.orElse(null) != UserDirectoryType.KEYCLOAK)) {
      notRead.add(KEYCLOAK);
    }
    if (hasValuesBelow(environment, LDAP) && (type.orElse(null) != UserDirectoryType.LDAP)) {
      notRead.add(LDAP);
    }
    return notRead;

  }

  private static boolean hasValuesBelow(
      final Environment environment,
      final String prefix) {

    return Binder
        .get(environment)
        .bind(ConfigurationPropertyName.adapt(prefix, '.'), Bindable.mapOf(String.class, Object.class))
        .map(Map::isEmpty)
        .map(empty -> !empty)
        .orElse(false);

  }

  /**
   * The value of {@link #TYPE} as somebody would write it.
   */
  static String nameOf(
      final UserDirectoryType type) {

    return type
        .name()
        .toLowerCase(Locale.ROOT);

  }

}
