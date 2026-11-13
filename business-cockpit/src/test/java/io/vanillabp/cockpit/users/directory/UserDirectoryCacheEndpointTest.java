package io.vanillabp.cockpit.users.directory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.servlet.autoconfigure.actuate.web.ServletManagementContextAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import io.vanillabp.cockpit.autoconfigure.BusinessCockpitUserDirectoryAutoConfiguration;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The actuator endpoint which lets operations empty the cache, and the nightly clearing as the
 * auto-configuration builds it. Nothing is asked here, so no Keycloak is needed.
 */
@ExtendWith(SuppressOutputExtension.class)
class UserDirectoryCacheEndpointTest {

  private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(
          BusinessCockpitUserDirectoryAutoConfiguration.class, EndpointAutoConfiguration.class,
          WebEndpointAutoConfiguration.class, ManagementContextAutoConfiguration.class,
          ServletManagementContextAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
          WebMvcAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
          JacksonAutoConfiguration.class))
      .withPropertyValues(
          "business-cockpit.user-directory.type=keycloak",
          "business-cockpit.user-directory.keycloak.server-url=http://localhost:8180",
          "business-cockpit.user-directory.keycloak.realm=business-cockpit",
          "business-cockpit.user-directory.keycloak.client-id=directory",
          "business-cockpit.user-directory.keycloak.client-secret=secret");

  @Test
  void theEndpointIsNotThereUnlessOperationsExposesIt() {

    runner.run(context -> assertThat(context)
        .hasNotFailed()
        .doesNotHaveBean(UserDirectoryCacheEndpoint.class));

  }

  @Test
  void aDeleteOnTheExposedEndpointEmptiesTheCache() {

    // the directory of the cockpit is replaced by one which counts: with 'ldap' chosen and Spring
    // LDAP hidden, the cockpit builds no directory of its own, and the counting one is the only one
    runner
        .withClassLoader(new FilteredClassLoader("org.springframework.ldap"))
        .withPropertyValues(
            "business-cockpit.user-directory.type=ldap",
            "management.endpoints.web.exposure.include="
                + UserDirectoryCacheEndpoint.ID)
        .withBean(CountingDirectory.class, () -> new CountingDirectory(Duration.ofHours(14), Clock.systemUTC()))
        .run(context -> {
          assertThat(context).hasSingleBean(UserDirectoryCacheEndpoint.class);
          final var directory = context.getBean(CountingDirectory.class);
          directory.getUser("jane");
          directory.getUser("jane");

          MockMvcBuilders
              .webAppContextSetup(context)
              .build()
              .perform(delete("/actuator/"
                  + UserDirectoryCacheEndpoint.ID))
              .andExpect(status().isNoContent());
          directory.getUser("jane");

          assertThat(directory.loads()).isEqualTo(2);
        });

  }

  @Test
  void theNightlyClearingIsBuiltWithItsDefault() {

    runner.run(context -> assertThat(context
        .getBean(NightlyCacheClearing.class)
        .trigger()
        .orElseThrow()
        .getExpression()).isEqualTo(UserDirectoryProperties.DEFAULT_CACHE_CLEAR_CRON));

  }

  @Test
  void theDashLeavesTheNightlyClearingOff() {

    runner
        .withPropertyValues("business-cockpit.user-directory.cache-clear-cron=-")
        .run(context -> assertThat(context
            .getBean(NightlyCacheClearing.class)
            .trigger()).isEmpty());

  }

  @Test
  void anotherCronIsTaken() {

    runner
        .withPropertyValues("business-cockpit.user-directory.cache-clear-cron=0 30 23 * * *")
        .run(context -> assertThat(context
            .getBean(NightlyCacheClearing.class)
            .trigger()
            .orElseThrow()
            .getExpression()).isEqualTo("0 30 23 * * *"));

  }

}
