package io.vanillabp.cockpit.support.rest.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;

import feign.Feign;
import feign.Param;
import feign.RequestLine;
import io.vanillabp.cockpit.support.rest.httpservice.Stubs;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import tools.jackson.databind.json.JsonMapper;

/**
 * An answer read into a {@code String} is taken as text, so an API which answers
 * {@code text/plain} needs no decoder of its own. JSON stays JSON, a JSON string included.
 */
@ExtendWith(SuppressOutputExtension.class)
public class ClientsConfigurationDecodingTest {

  static final MockWebServer server = new MockWebServer();

  @AfterAll
  static void stop() {
    server.close();
  }

  /** A configuration as Spring creates it, with the JSON mapper of the application. */
  private static class ConfigurationWithJson extends ClientsConfigurationBase {

    ConfigurationWithJson() {
      objectMapper = Optional.of(JsonMapper.builder().build());
    }

  }

  interface Labels {

    @RequestLine("GET /items/{itemId}/label")
    String labelOf(
        @Param("itemId") String itemId);

  }

  private static Labels labels() {

    final var client = new Client();
    client.setBaseUrl(Stubs.apiUrl(server));
    final var builder = Feign.builder();
    new ConfigurationWithJson().configureFeignBuilder(Labels.class, builder, client);
    return builder.target(Labels.class, client.getBaseUrl());

  }

  @Test
  public void testAPlainTextAnswerIsReadIntoAString() {

    server.enqueue(new MockResponse.Builder()
        .code(HttpStatus.OK.value())
        .addHeader("Content-Type", "text/plain")
        .body("Shelf 3, left")
        .build());

    assertThat(labels().labelOf("1")).isEqualTo("Shelf 3, left");

  }

  @Test
  public void testAJsonStringReadIntoAStringLosesItsQuotesAsBefore() {

    server.enqueue(Stubs.json(HttpStatus.OK.value(), "\"Shelf 3, left\""));

    assertThat(labels().labelOf("1")).isEqualTo("Shelf 3, left");

  }

}
