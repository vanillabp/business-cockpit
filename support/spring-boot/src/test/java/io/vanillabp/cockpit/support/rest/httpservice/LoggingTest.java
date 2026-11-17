package io.vanillabp.cockpit.support.rest.httpservice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.service.registry.ImportHttpServices;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.vanillabp.cockpit.support.rest.httpservice.inventory.InventoryApi;
import io.vanillabp.cockpit.support.rest.httpservice.inventory.model.Reservation;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import mockwebserver3.MockWebServer;

/**
 * {@code log: true} logs each request and its answer with headers and bodies at level INFO. The
 * values of headers which carry a login are hidden.
 */
@ExtendWith(SuppressOutputExtension.class)
@SpringBootTest(
    classes = {
        TestApplication.class, LoggingTest.Clients.class
    },
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "spring.http.serviceclient.inventory.log=true", "spring.http.serviceclient.inventory.default-header.Authorization=Basic c2NvdHQ6dGlnZXI=",
    })
@DirtiesContext
public class LoggingTest {

  static final MockWebServer server = new MockWebServer();

  @DynamicPropertySource
  static void urls(
      final DynamicPropertyRegistry registry) {
    registry.add("spring.http.serviceclient.inventory.base-url", () -> Stubs.apiUrl(server));
  }

  @AfterAll
  static void stop() {
    server.close();
  }

  interface Inventory extends InventoryApi {
  }

  @Configuration
  @ImportHttpServices(group = "inventory", types = Inventory.class)
  static class Clients {
  }

  @Autowired
  Inventory inventory;

  private final Logger logger = (Logger) LoggerFactory.getLogger(LoggingInterceptor.class);

  private final ListAppender<ILoggingEvent> logged = new ListAppender<>();

  @BeforeEach
  void listen() {
    logged.start();
    logger.addAppender(logged);
  }

  @AfterEach
  void stopListening() {
    logger.detachAppender(logged);
  }

  @Test
  public void request_and_answer_are_logged_with_their_bodies() {

    server.enqueue(Stubs.json(201, """
        {"id": "r-1", "quantity": 3}"""));

    final var answer = inventory.reserveItem("1", new Reservation().quantity(3));

    // the logging read the body, the converters could still read it
    assertThat(answer.getBody().getId()).isEqualTo("r-1");
    assertThat(logged.list).hasSize(2).allMatch(event -> event.getLevel() == Level.INFO);
    final var request = logged.list.get(0).getFormattedMessage();
    assertThat(request)
        .contains("[inventory] ---> POST "
            + Stubs.apiUrl(server)
            + "/items/1/reservations")
        .contains("\"quantity\":3")
        .contains("Authorization:\"<hidden>\"")
        .doesNotContain("c2NvdHQ6dGlnZXI=");
    assertThat(logged.list.get(1).getFormattedMessage())
        .contains("[inventory] <--- 201")
        .contains("\"id\": \"r-1\"");

  }

}
