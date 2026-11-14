package io.vanillabp.cockpit.support.security.usercontext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.diagnostics.LoggingFailureAnalysisReporter;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.vanillabp.cockpit.support.security.jwt.JwtAuthenticationTokenMapper;
import io.vanillabp.cockpit.support.security.jwt.JwtProperties;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A workflow module which reads the login token of the cockpit needs a user directory of its own.
 * Where it has none, the start fails with a report which says what to add, and not only that a
 * bean is missing.
 */
@ExtendWith(SuppressOutputExtension.class)
class AMissingDirectoryIsExplainedTest {

  /** A workflow module which reads the token and has no directory. */
  @Configuration(proxyBeanMethods = false)
  static class AModuleWithoutADirectory {

    @Bean
    JwtAuthenticationTokenMapper jwtMapper(
        final UserDirectoryService userDirectory) {

      return new JwtAuthenticationTokenMapper(new JwtProperties(), userDirectory, groups -> groups);

    }

  }

  /** A bean which needs something else, to show that the analyzer leaves it to Spring Boot. */
  @Configuration(proxyBeanMethods = false)
  static class AModuleWithoutSomethingElse {

    @Bean
    String needsSomethingElse(
        final Runnable somethingElse) {

      return "never built";

    }

  }

  private ListAppender<ILoggingEvent> reported;

  @BeforeEach
  void listenToTheFailureReport() {

    // Spring Boot would set up logback again while it starts and drop the appender below
    System.setProperty(LoggingSystem.SYSTEM_PROPERTY, LoggingSystem.NONE);
    reported = new ListAppender<>();
    reported.start();
    reporter().addAppender(reported);

  }

  @AfterEach
  void stopListening() {

    reporter().detachAppender(reported);
    System.clearProperty(LoggingSystem.SYSTEM_PROPERTY);

  }

  private static Logger reporter() {

    return (Logger) LoggerFactory.getLogger(LoggingFailureAnalysisReporter.class);

  }

  private String start(
      final Class<?> module) {

    assertThatThrownBy(() -> new SpringApplicationBuilder(module)
        .web(WebApplicationType.NONE)
        .logStartupInfo(false)
        .run()
        .close())
        .isNotNull();
    return reported.list
        .stream()
        .map(ILoggingEvent::getFormattedMessage)
        .collect(Collectors.joining("\n"));

  }

  @Test
  void theReportNamesTheBeanToAddAndTheCache() {

    final var report = start(AModuleWithoutADirectory.class);

    assertThat(report)
        .contains(UserDirectoryService.class.getName())
        .contains("The bean 'jwtMapper' needs one.")
        .contains("public UserDirectoryService userDirectory()")
        .contains("cache the users")
        .contains("a directory of its own");

  }

  @Test
  void anotherMissingBeanKeepsTheReportOfSpringBoot() {

    final var report = start(AModuleWithoutSomethingElse.class);

    assertThat(report)
        .contains(Runnable.class.getName())
        .doesNotContain(UserDirectoryService.class.getSimpleName());

  }

}
