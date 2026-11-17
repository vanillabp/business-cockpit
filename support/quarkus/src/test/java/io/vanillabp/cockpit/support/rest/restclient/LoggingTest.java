package io.vanillabp.cockpit.support.rest.restclient;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.cockpit.support.rest.restclient.inventory.InventoryApi;
import io.vanillabp.cockpit.support.rest.restclient.inventory.model.Reservation;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import mockwebserver3.MockWebServer;

/**
 * {@code log: true} logs each request and its answer with headers and bodies at level INFO. The
 * values of the headers which carry a login are hidden. {@code verify-ssl: false} warns at the
 * start.
 */
@ExtendWith(SuppressOutputExtension.class)
public class LoggingTest {

  private static final String STUB = "logging";

  @RegisterExtension
  static final QuarkusExtensionTest application = new QuarkusExtensionTest()
      .withApplicationRoot(TheRestClientsOnQuarkus::withTheRestClients)
      .overrideConfigKey("logged.base-url", Stubs.url(STUB, "/api"))
      .overrideConfigKey("logged.log", "true")
      .overrideConfigKey("logged.verify-ssl", "false")
      .overrideConfigKey("logged.default-header.Authorization", "Basic c2NvdHQ6dGlnZXI=")
      .setLogRecordPredicate(record -> record.getLoggerName().startsWith(RestClients.class.getPackageName()))
      .assertLogRecords(records -> {
        final var messages = records
            .stream()
            .map(record -> record.getLevel()
                + " "
                + formatted(record.getMessage(), record.getParameters()))
            .toList();
        assertThat(messages).anySatisfy(message -> assertThat(message)
            .startsWith("INFO [logged] ---> POST http://127.0.0.1:")
            .contains("/api/items/1/reservations")
            .contains("Authorization:\"<hidden>\"")
            .doesNotContain("c2NvdHQ6dGlnZXI")
            .contains("\"quantity\":3"));
        assertThat(messages).anySatisfy(message -> assertThat(message)
            .startsWith("INFO [logged] <--- 201")
            .contains("\"id\": \"r-1\""));
        assertThat(messages).anySatisfy(message -> assertThat(message)
            .startsWith("WARN The REST clients at 'logged' trust every server"));
      });

  static final MockWebServer server = new MockWebServer();

  private static String formatted(
      final String message,
      final Object[] parameters) {

    var result = message;
    for (final var parameter : parameters == null ? new Object[0] : parameters) {
      result = result.replaceFirst("\\{}", java.util.regex.Matcher.quoteReplacement(String.valueOf(parameter)));
    }
    return result;

  }

  @AfterAll
  static void stop() {
    server.close();
  }

  @Test
  public void request_and_answer_are_logged_with_their_bodies() {

    Stubs.started(server, STUB);
    server.enqueue(Stubs.json(201, """
        {"id": "r-1", "quantity": 3}"""));

    TheRestClientsOnQuarkus.fresh().build(InventoryApi.class, "logged").reserveItem("1", new Reservation().quantity(3));

  }

}
