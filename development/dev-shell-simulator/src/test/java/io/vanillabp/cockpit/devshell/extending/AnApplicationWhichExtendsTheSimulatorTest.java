package io.vanillabp.cockpit.devshell.extending;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * An application which extends the simulator has an {@code application.yaml} of its own, and Spring
 * Boot reads only the first file of that name on the class path. The simulator's defaults still have
 * to count: the data source, the schema update and the key of the login token. A value the
 * application sets itself wins over them, in its own file or through the builder.
 * <p>
 * The location of the configuration points at the application's file alone. That is what happens
 * when its {@code application.yaml} hides the one of the simulator.
 */
@ExtendWith(SuppressOutputExtension.class)
class AnApplicationWhichExtendsTheSimulatorTest {

  @Configuration
  static class AnApplicationWhichDoesNotExtendTheSimulator {
  }

  @Test
  void theDefaultsOfTheSimulatorCountForASubclass() {

    try (var context = new SpringApplicationBuilder(ExtendingSimulator.class)
        .properties("spring.config.location=classpath:/extending/")
        .run()) {

      final var environment = context.getEnvironment();
      assertThat(environment.getProperty("spring.datasource.url")).isEqualTo("jdbc:h2:mem:devshellsimulator");
      assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
      assertThat(environment.getProperty("application.jwt.hmacSHA256-base64")).isNotBlank();
      assertThat(environment.getProperty("spring.jpa.show-sql"))
          .as("a value of the application wins over the default")
          .isEqualTo("true");

    }

  }

  /**
   * A value the application passes to the builder wins over the default as well. Spring Boot puts
   * those values behind every other property source, so the defaults have to go behind them.
   */
  @Test
  void aValueOfTheBuilderWinsOverTheDefault() {

    try (var context = new SpringApplicationBuilder(ExtendingSimulator.class)
        .properties(
            "spring.config.location=classpath:/extending/",
            "spring.jpa.hibernate.ddl-auto=create-drop")
        .run()) {

      final var environment = context.getEnvironment();
      assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto"))
          .as("the value of the builder wins over the default")
          .isEqualTo("create-drop");
      assertThat(environment.getProperty("spring.datasource.url"))
          .as("a default the builder does not set still counts")
          .isEqualTo("jdbc:h2:mem:devshellsimulator");

    }

  }

  /** The negative twin: another application with the jar on its class path gets no defaults. */
  @Test
  void anotherApplicationGetsNoDefaults() {

    try (var context = new SpringApplicationBuilder(AnApplicationWhichDoesNotExtendTheSimulator.class)
        .web(WebApplicationType.NONE)
        .properties("spring.config.location=classpath:/extending/")
        .run()) {

      assertThat(context.getEnvironment().getProperty("spring.datasource.url")).isNull();

    }

  }

}
