package io.vanillabp.derived.cockpit;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.mongodb.MongoDBContainer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * An application built on the library which forgot its security chains. The library brings none,
 * and it does not refuse to start either. What keeps the application closed is Spring Boot: with
 * no chain at all, its default chain asks for a login on every path, with a password it
 * generates. The startup check says on every start which chains are missing.
 * <p>
 * If this test fails at the 401, the application is open to everybody, and that is what the
 * test exists to show.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class AnApplicationWithoutChainsTest {

  private static final MongoDBContainer MONGODB = new MongoDBContainer("mongo:7.0").withReplicaSet();

  private static final HttpClient HTTP = HttpClient
      .newBuilder()
      .connectTimeout(Duration.ofSeconds(10))
      .build();

  private static ConfigurableApplicationContext application;

  private static int port;

  private static String startupWarnings;

  @BeforeAll
  static void startTheApplication() {

    MONGODB.start();
    final var recorded = new ListAppender<ILoggingEvent>();
    recorded.start();
    application = new SpringApplicationBuilder(DerivedCockpitApplication.class)
        // the recorder waits for this event, because Spring Boot replaces the logging setup
        // while it prepares the environment
        .listeners(event -> {
          if (event instanceof ApplicationPreparedEvent) {
            ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("io.vanillabp.cockpit"))
                .addAppender(recorded);
          }
        })
        .run(
            "--derived-cockpit.chains=false",
            "--server.port=0",
            "--spring.main.banner-mode=off",
            "--spring.mongodb.uri="
                + MONGODB.getReplicaSetUrl("cockpit-without-chains"),
            "--business-cockpit.title-short=Derived",
            "--business-cockpit.title-long=A cockpit without chains",
            "--business-cockpit.application-version=1.2.3",
            "--business-cockpit.application-uri=http://localhost",
            "--business-cockpit.jwt.hmacSHA256-base64="
                + Base64.getEncoder().encodeToString(new byte[32]),
            "--bpms-api.realm-name=BPMS-API",
            "--bpms-api.username=reporting",
            "--bpms-api.password={noop}secret");
    port = ((WebServerApplicationContext) application).getWebServer().getPort();
    ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("io.vanillabp.cockpit"))
        .detachAppender(recorded);
    startupWarnings = recorded.list
        .stream()
        .filter(event -> event.getLevel() == Level.WARN)
        .map(ILoggingEvent::getFormattedMessage)
        .reduce((
            left,
            right) -> left
                + "\n"
                + right)
        .orElse("");

  }

  @AfterAll
  static void stopTheApplication() {

    if (application != null) {
      application.close();
    }
    MONGODB.stop();

  }

  @Test
  void theStartupLogNamesBothMissingChainsAndTheWiki() {

    assertThat(startupWarnings)
        .contains("'guiHttpSecurity'")
        .contains("'bpmsApiHttpSecurity'")
        .contains("https://github.com/vanillabp/business-cockpit/wiki/Security");

  }

  @Test
  void theGuiApiAsksForALogin() {

    final var response = send(HttpRequest
        .newBuilder(URI.create(url("/gui/api/v1/workflow-module")))
        .GET()
        .build());

    assertThat(response.statusCode()).isEqualTo(401);

  }

  /**
   * The three values are set, as an application of an older release had them for the library's
   * chain. Nothing is built from them now, so the credentials do not open the BPMS API.
   */
  @Test
  void theBpmsApiAsksForALoginEvenWithTheFormerCredentials() {

    final var response = send(HttpRequest
        .newBuilder(URI.create(url("/bpms/api/v1_1/workflow-module/any-module")))
        .header("Authorization", "Basic "
            + Base64.getEncoder().encodeToString("reporting:secret".getBytes()))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString("{ \"id\": \"any-module\" }"))
        .build());

    assertThat(response.statusCode()).isEqualTo(401);

  }

  private static String url(
      final String path) {

    return "http://localhost:"
        + port
        + path;

  }

  private static HttpResponse<String> send(
      final HttpRequest request) {

    try {
      return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (IOException e) {
      throw new IllegalStateException("HTTP request failed", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("HTTP request interrupted", e);
    }

  }

}
