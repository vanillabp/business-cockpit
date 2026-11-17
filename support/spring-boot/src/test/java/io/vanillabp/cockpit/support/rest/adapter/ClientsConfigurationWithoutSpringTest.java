package io.vanillabp.cockpit.support.rest.adapter;

import static org.assertj.core.api.Assertions.assertThat;

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

/**
 * A configuration created with {@code new}, as a test of a client module does, builds a working
 * client. Without Spring there is no JSON mapper, so Feign keeps its own decoder, which reads text.
 */
@ExtendWith(SuppressOutputExtension.class)
public class ClientsConfigurationWithoutSpringTest {

  static final MockWebServer server = new MockWebServer();

  @AfterAll
  static void stop() {
    server.close();
  }

  private static class ConfigurationCreatedWithNew extends ClientsConfigurationBase {
  }

  interface Labels {

    @RequestLine("GET /items/{itemId}/label")
    String labelOf(
        @Param("itemId") String itemId);

  }

  @Test
  public void testAConfigurationCreatedWithoutSpringBuildsAWorkingClient() {

    final var client = new Client();
    client.setBaseUrl(Stubs.apiUrl(server));
    final var builder = Feign.builder();

    new ConfigurationCreatedWithNew().configureFeignBuilder(Labels.class, builder, client);

    server.enqueue(new MockResponse.Builder()
        .code(HttpStatus.OK.value())
        .addHeader("Content-Type", "text/plain")
        .body("Shelf 3, left")
        .build());
    assertThat(builder.target(Labels.class, client.getBaseUrl()).labelOf("1")).isEqualTo("Shelf 3, left");

  }

}
