package io.vanillabp.cockpit.config.startup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.mock.env.MockEnvironment;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.vanillabp.cockpit.support.security.jwt.JwtUserDetailsProvider;
import io.vanillabp.cockpit.support.security.usercontext.UserDirectoryService;
import io.vanillabp.cockpit.users.directory.KeycloakUserDirectoryService;
import io.vanillabp.cockpit.users.directory.UserDirectoryConfiguration;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * What a cockpit application is told about its user directory, and which of it stops the start.
 * Like the other tests of the startup check, the assertions are about the property names and the
 * examples a message has to carry, not about its wording.
 */
@ExtendWith(SuppressOutputExtension.class)
class UserDirectoryStartupCheckTest {

  private static final String TYPE = "business-cockpit.user-directory.type";

  private static final String KEYCLOAK = "business-cockpit.user-directory.keycloak.";

  private static final String LDAP = "business-cockpit.user-directory.ldap.";

  private static final Map<String, String> COMPLETE_KEYCLOAK = Map.of(
      TYPE, "keycloak",
      KEYCLOAK
          + "server-url",
      "http://localhost:8180",
      KEYCLOAK
          + "realm",
      "business-cockpit",
      KEYCLOAK
          + "client-id",
      "business-cockpit-directory",
      KEYCLOAK
          + "client-secret",
      "secret");

  private final StartupConfigurationCheck check = new StartupConfigurationCheck();

  private ListAppender<ILoggingEvent> recordedLog;

  @BeforeEach
  void recordWhatIsLogged() {

    recordedLog = new ListAppender<>();
    recordedLog.start();
    ((ch.qos.logback.classic.Logger) LoggerFactory
        .getLogger(StartupConfigurationCheck.class))
        .addAppender(recordedLog);

  }

  @AfterEach
  void stopRecording() {

    ((ch.qos.logback.classic.Logger) LoggerFactory
        .getLogger(StartupConfigurationCheck.class))
        .detachAppender(recordedLog);

  }

  /**
   * A configuration with everything the rest of the cockpit needs, plus the given values.
   */
  private static MockEnvironment configurationWith(
      final Map<String, String> values) {

    final var environment = new MockEnvironment()
        .withProperty(CockpitConfiguration.MONGODB_URI, "mongodb://localhost:27017/business-cockpit")
        .withProperty(CockpitConfiguration.TITLE_SHORT, "TestCockpit")
        .withProperty(CockpitConfiguration.JWT_KEY, "0aH1oXQ4wZk5H0nB8mS1uQ8pQ0kZ1x2y3z4A5b6C7d8=");
    values.forEach(environment::withProperty);
    return environment;

  }

  private String warnings() {

    return String.join("\n", recordedLog.list
        .stream()
        .filter(event -> event.getLevel() == Level.WARN)
        .map(ILoggingEvent::getFormattedMessage)
        .toList());

  }

  /**
   * Bean definitions as the check sees them before the first bean is built.
   */
  private static DefaultListableBeanFactory applicationWith(
      final Map<String, Class<?>> beans) {

    final var beanFactory = new DefaultListableBeanFactory();
    beanFactory.registerBeanDefinition(
        "userDetailsProvider", new RootBeanDefinition(JwtUserDetailsProvider.class));
    beans.forEach((
        name,
        type) -> beanFactory.registerBeanDefinition(name, new RootBeanDefinition(type)));
    return beanFactory;

  }

  /**
   * A class loader which does not find Spring LDAP, like the one of an application without the
   * optional dependency.
   */
  private static ClassLoader withoutSpringLdap() {

    return new ClassLoader(UserDirectoryStartupCheckTest.class.getClassLoader()) {
      @Override
      protected Class<?> loadClass(
          final String name,
          final boolean resolve) throws ClassNotFoundException {

        if (name.startsWith("org.springframework.ldap.")) {
          throw new ClassNotFoundException(name);
        }
        return super.loadClass(name, resolve);

      }
    };

  }

  @Test
  void withoutATypeNothingIsSaidAboutTheDirectory() {

    check.checkConfigurationOf(configurationWith(Map.of()));

    assertThat(warnings()).doesNotContain("user-directory");

  }

  @Test
  void aCompleteKeycloakDirectoryIsAccepted() {

    assertThatCode(() -> check.checkConfigurationOf(configurationWith(COMPLETE_KEYCLOAK)))
        .doesNotThrowAnyException();
    assertThat(warnings()).doesNotContain("user-directory");

  }

  @Test
  void aTypeWhichNamesNoDirectoryStopsTheStart() {

    assertThatThrownBy(() -> check.checkConfigurationOf(configurationWith(Map.of(TYPE, "active-directory"))))
        .isInstanceOf(UserDirectoryIsNotUsableException.class)
        .hasMessageContaining(TYPE)
        .hasMessageContaining("'active-directory'")
        .hasMessageContaining("Example: "
            + TYPE
            + ": keycloak");

  }

  @Test
  void theTypeIsReadInAnyCase() {

    final var values = new java.util.HashMap<>(COMPLETE_KEYCLOAK);
    values.put(TYPE, "KeyCloak");

    assertThatCode(() -> check.checkConfigurationOf(configurationWith(values)))
        .doesNotThrowAnyException();

  }

  /**
   * All four values are reported in one message, so nobody has to restart four times.
   */
  @Test
  void everyMissingValueOfKeycloakIsReportedInOneMessage() {

    assertThatThrownBy(() -> check.checkConfigurationOf(configurationWith(Map.of(TYPE, "keycloak"))))
        .isInstanceOf(UserDirectoryIsNotUsableException.class)
        .hasMessageContaining("Example: "
            + KEYCLOAK
            + "server-url: ")
        .hasMessageContaining("Example: "
            + KEYCLOAK
            + "realm: ")
        .hasMessageContaining("Example: "
            + KEYCLOAK
            + "client-id: ")
        .hasMessageContaining("Example: "
            + KEYCLOAK
            + "client-secret: ")
        .hasMessageContaining("'view-users'");

  }

  @Test
  void aUserIdWhichIsNeitherUsernameNorIdStopsTheStart() {

    final var values = new java.util.HashMap<>(COMPLETE_KEYCLOAK);
    values.put(KEYCLOAK
        + "user-id", "email");

    assertThatThrownBy(() -> check.checkConfigurationOf(configurationWith(values)))
        .isInstanceOf(UserDirectoryIsNotUsableException.class)
        .hasMessageContaining(KEYCLOAK
            + "user-id")
        .hasMessageContaining("'email'");

  }

  @ParameterizedTest
  @ValueSource(strings = {
      "soon", "-PT1M"
  })
  void aCacheDurationWhichCannotWorkStopsTheStart(
      final String cacheDuration) {

    final var values = new java.util.HashMap<>(COMPLETE_KEYCLOAK);
    values.put("business-cockpit.user-directory.cache-duration", cacheDuration);

    assertThatThrownBy(() -> check.checkConfigurationOf(configurationWith(values)))
        .isInstanceOf(UserDirectoryIsNotUsableException.class)
        .hasMessageContaining("Example: business-cockpit.user-directory.cache-duration: PT5M");

  }

  @ParameterizedTest
  @ValueSource(strings = {
      "PT1M", "10m", "0"
  })
  void aCacheDurationOfZeroOrMoreIsAccepted(
      final String cacheDuration) {

    final var values = new java.util.HashMap<>(COMPLETE_KEYCLOAK);
    values.put("business-cockpit.user-directory.cache-duration", cacheDuration);

    assertThatCode(() -> check.checkConfigurationOf(configurationWith(values)))
        .doesNotThrowAnyException();

  }

  @Test
  void anLdapDirectoryWithoutSpringLdapStopsTheStartAndShowsTheDependency() {

    check.setBeanClassLoader(withoutSpringLdap());

    assertThatThrownBy(() -> check.checkConfigurationOf(configurationWith(Map.of(
        TYPE, "ldap",
        LDAP
            + "url",
        "ldap://localhost:389"))))
        .isInstanceOf(UserDirectoryIsNotUsableException.class)
        .hasMessageContaining("<groupId>org.springframework.ldap</groupId>")
        .hasMessageContaining("<artifactId>spring-ldap-core</artifactId>");

  }

  @Test
  void anLdapDirectoryWithSpringLdapNeedsTheUrl() {

    assertThatThrownBy(() -> check.checkConfigurationOf(configurationWith(Map.of(TYPE, "ldap"))))
        .isInstanceOf(UserDirectoryIsNotUsableException.class)
        .hasMessageContaining("Example: "
            + LDAP
            + "url: ")
        .hasMessageNotContaining("spring-ldap-core");

  }

  @Test
  void aBindDnWithoutAPasswordStopsTheStart() {

    assertThatThrownBy(() -> check.checkConfigurationOf(configurationWith(Map.of(
        TYPE, "ldap",
        LDAP
            + "url",
        "ldap://localhost:389",
        LDAP
            + "bind-dn",
        "cn=admin,dc=example,dc=com"))))
        .isInstanceOf(UserDirectoryIsNotUsableException.class)
        .hasMessageContaining(LDAP
            + "bind-password");

  }

  @Test
  void anLdapDirectoryWhichReadsAnonymouslyIsAccepted() {

    assertThatCode(() -> check.checkConfigurationOf(configurationWith(Map.of(
        TYPE, "ldap",
        LDAP
            + "url",
        "ldap://localhost:389"))))
        .doesNotThrowAnyException();

  }

  @Test
  void settingsOfADirectoryWhichIsNotChosenAreAWarning() {

    final var values = new java.util.HashMap<>(COMPLETE_KEYCLOAK);
    values.put(LDAP
        + "url", "ldap://localhost:389");

    check.checkConfigurationOf(configurationWith(values));

    assertThat(warnings())
        .contains("business-cockpit.user-directory.ldap")
        .contains("'keycloak'");

  }

  @Test
  void anUnusableDirectoryIsShownAsTheFailureReport() {

    final var exception = catchThrowableOfType(
        UserDirectoryIsNotUsableException.class,
        () -> check.checkConfigurationOf(configurationWith(Map.of(TYPE, "keycloak"))));

    final var report = new UserDirectoryIsNotUsableFailureAnalyzer().analyze(exception);

    assertThat(report.getDescription()).contains(KEYCLOAK
        + "realm");
    assertThat(report.getAction()).contains(TYPE);

  }

  @Test
  void anApplicationWithoutADirectoryIsToldHowToGetOne() {

    assertThatThrownBy(() -> check.checkTheSecurityOf(configurationWith(Map.of()), applicationWith(Map.of())))
        .isInstanceOf(UserDirectoryIsNotUsableException.class)
        .hasMessageContaining(UserDirectoryService.class.getName())
        .hasMessageContaining(TYPE
            + ": keycloak");

  }

  @Test
  void theDirectoryTheCockpitBringsIsEnough() {

    assertThatCode(() -> check.checkTheSecurityOf(configurationWith(COMPLETE_KEYCLOAK), applicationWith(Map.of(
        UserDirectoryConfiguration.KEYCLOAK_BEAN, KeycloakUserDirectoryService.class))))
        .doesNotThrowAnyException();

  }

  @Test
  void aDirectoryOfTheApplicationNextToTheOneTheCockpitBringsStopsTheStart() {

    assertThatThrownBy(() -> check.checkTheSecurityOf(configurationWith(COMPLETE_KEYCLOAK), applicationWith(Map.of(
        UserDirectoryConfiguration.KEYCLOAK_BEAN, KeycloakUserDirectoryService.class,
        "myUserDirectory", UserDirectoryService.class))))
        .isInstanceOf(UserDirectoryIsNotUsableException.class)
        .hasMessageContaining("'myUserDirectory'")
        .hasMessageContaining(TYPE);

  }

}
