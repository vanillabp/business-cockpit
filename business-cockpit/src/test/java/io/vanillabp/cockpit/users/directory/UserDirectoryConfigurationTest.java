package io.vanillabp.cockpit.users.directory;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import io.vanillabp.cockpit.autoconfigure.BusinessCockpitUserDirectoryAutoConfiguration;
import io.vanillabp.cockpit.support.security.usercontext.UserDirectoryService;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Which directory the auto-configuration builds for which property. Nothing is asked here, so no
 * Keycloak and no LDAP server is needed.
 */
@ExtendWith(SuppressOutputExtension.class)
class UserDirectoryConfigurationTest {

  private final ApplicationContextRunner runner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(BusinessCockpitUserDirectoryAutoConfiguration.class));

  @Test
  void withoutATypeTheCockpitBringsNoDirectory() {

    runner.run(context -> assertThat(context).doesNotHaveBean(UserDirectoryService.class));

  }

  @Test
  void theTypeKeycloakBringsTheKeycloakDirectory() {

    runner
        .withPropertyValues(
            "business-cockpit.user-directory.type=keycloak",
            "business-cockpit.user-directory.keycloak.server-url=http://localhost:8180",
            "business-cockpit.user-directory.keycloak.realm=business-cockpit",
            "business-cockpit.user-directory.keycloak.client-id=directory",
            "business-cockpit.user-directory.keycloak.client-secret=secret")
        .run(context -> {
          assertThat(context).hasSingleBean(UserDirectoryService.class);
          assertThat(context)
              .getBean(UserDirectoryConfiguration.KEYCLOAK_BEAN)
              .isInstanceOf(KeycloakUserDirectoryService.class);
          final var properties = context.getBean(UserDirectoryProperties.class);
          assertThat(properties.keycloak().userId()).isEqualTo(KeycloakUserId.USERNAME);
          assertThat(properties.cacheDuration()).hasHours(14);
          assertThat(properties.cacheClearCron()).isEqualTo("0 0 2 * * *");
        });

  }

  @Test
  void theTypeLdapBringsTheLdapDirectoryWithItsDefaults() {

    runner
        .withPropertyValues(
            "business-cockpit.user-directory.type=ldap",
            "business-cockpit.user-directory.ldap.url=ldap://localhost:389",
            "business-cockpit.user-directory.cache-duration=1m")
        .run(context -> {
          assertThat(context)
              .getBean(UserDirectoryConfiguration.LDAP_BEAN)
              .isInstanceOf(LdapUserDirectoryService.class);
          final var properties = context.getBean(UserDirectoryProperties.class);
          assertThat(properties.ldap().userObjectClass()).isEqualTo("inetOrgPerson");
          assertThat(properties.ldap().userIdAttribute()).isEqualTo("uid");
          assertThat(properties.ldap().groupMemberAttribute()).isEqualTo("member");
          assertThat(properties.ldap().groupNameAttribute()).isEqualTo("cn");
          assertThat(properties.cacheDuration()).hasMinutes(1);
        });

  }

  /**
   * The startup check stops such a start with a message. Here only the condition is tested: no
   * class of Spring LDAP is loaded.
   */
  @Test
  void withoutSpringLdapTheLdapDirectoryIsNotBuilt() {

    runner
        .withClassLoader(new FilteredClassLoader("org.springframework.ldap"))
        .withPropertyValues(
            "business-cockpit.user-directory.type=ldap",
            "business-cockpit.user-directory.ldap.url=ldap://localhost:389")
        .run(context -> assertThat(context)
            .hasNotFailed()
            .doesNotHaveBean(UserDirectoryService.class));

  }

}
