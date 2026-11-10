package io.vanillabp.cockpit.devshell.simulator.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Without a user nobody can log in to the dev shell, so the simulator refuses to start. Its message
 * names the property key and shows a user to copy.
 */
@ExtendWith(SuppressOutputExtension.class)
class TheSimulatorDoesNotStartWithoutUsersTest {

  @Configuration
  @EnableConfigurationProperties(Properties.class)
  static class OnlyTheProperties {
  }

  private final ApplicationContextRunner simulator = new ApplicationContextRunner()
      .withUserConfiguration(OnlyTheProperties.class);

  @Test
  void withoutUsersTheStartFailsAndNamesTheKey() {

    simulator.run(context -> assertThat(context)
        .hasFailed()
        .getFailure()
        .rootCause()
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(Properties.NO_USERS_MESSAGE)
        .extracting(Throwable::getMessage, InstanceOfAssertFactories.STRING)
        .contains("'dev-shell-simulator.users'", "- id: john", "--spring.config.additional-location"));

  }

  @Test
  void oneUserIsEnoughToStart() {

    simulator
        .withPropertyValues("dev-shell-simulator.users[0].id=john")
        .run(context -> assertThat(context)
            .hasNotFailed()
            .getBean(Properties.class)
            .extracting(properties -> properties.getUser("john"))
            .isNotNull());

  }

}
