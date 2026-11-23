package io.vanillabp.cockpit.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.Advised;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The BPMS API keeps its endpoints to one role with {@code @Secured}. An application which takes
 * method security over is told the annotation which keeps {@code @Secured} in force, and a start
 * where it is not in force ends with that annotation to copy.
 */
@ExtendWith(SuppressOutputExtension.class)
class SecuredCountsForTheBpmsApiTest {

  @Test
  void theLogNamesTheAnnotationWhichKeepsSecuredInForce() {

    final var logger = (Logger) LoggerFactory.getLogger(BusinessCockpitMethodSecurityAutoConfiguration.class);
    final var recorded = new ListAppender<ILoggingEvent>();
    recorded.start();
    logger.addAppender(recorded);
    try {
      new BusinessCockpitMethodSecurityAutoConfiguration();
    } finally {
      logger.detachAppender(recorded);
    }

    assertThat(recorded.list)
        .singleElement()
        .extracting(ILoggingEvent::getFormattedMessage)
        .asString()
        .contains("write @EnableMethodSecurity(securedEnabled = true) yourself");

  }

  /** A controller which keeps itself to one role, like the controllers of the BPMS API. */
  @Secured("ROLE_BPMS-API")
  public static class OneRoleOnly {

    public String report() {
      return "stored";
    }

  }

  @Configuration
  @EnableMethodSecurity(securedEnabled = true)
  static class WithSecured {

    @Bean
    OneRoleOnly controller() {
      return new OneRoleOnly();
    }

  }

  @Configuration
  @EnableMethodSecurity
  static class WithoutSecured {

    @Bean
    OneRoleOnly controller() {
      return new OneRoleOnly();
    }

  }

  @Test
  void aControllerIsProtectedWhereSecuredCounts() {

    new ApplicationContextRunner()
        .withUserConfiguration(WithSecured.class)
        .run(context -> {
          final var controller = context.getBean(OneRoleOnly.class);
          assertThat(SecuredCountsForTheBpmsApi.isProtected(controller, OneRoleOnly.class))
              .as("advisors: %s", controller instanceof Advised advised
                  ? Arrays.toString(advised.getAdvisors())
                  : "no proxy")
              .isTrue();
          SecurityContextHolder.clearContext();
          assertThatThrownBy(() -> context.getBean(OneRoleOnly.class).report())
              .as("the check agrees with what a call meets")
              .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        });

  }

  @Test
  void aPlainEnableMethodSecurityEndsTheStartWithTheAnnotationToWrite() {

    new ApplicationContextRunner()
        .withUserConfiguration(WithoutSecured.class)
        .run(context -> {
          assertThat(SecuredCountsForTheBpmsApi.isProtected(context.getBean(OneRoleOnly.class), OneRoleOnly.class))
              .isFalse();
          assertThatThrownBy(() -> new SecuredCountsForTheBpmsApi(List.of(OneRoleOnly.class))
              .onApplicationEvent(
                  new ApplicationReadyEvent(new SpringApplication(), new String[0], (ConfigurableApplicationContext) context
                      .getSourceApplicationContext(), Duration.ZERO)))
              .isInstanceOf(IllegalStateException.class)
              .hasMessageContaining("@EnableMethodSecurity(securedEnabled = true)")
              .hasMessageContaining("business-cockpit.method-security.enabled");
        });

  }

}
