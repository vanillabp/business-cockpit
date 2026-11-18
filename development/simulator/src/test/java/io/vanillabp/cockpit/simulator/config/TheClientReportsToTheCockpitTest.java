package io.vanillabp.cockpit.simulator.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

import com.sun.net.httpserver.HttpServer;

import io.vanillabp.cockpit.bpms.api.v1_1.RegisterWorkflowModuleEvent;
import io.vanillabp.cockpit.simulator.bpms.BpmsApi;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The client of the profile {@code rest-sync} is built from the keys below
 * {@code business-cockpit}: the address of the cockpit, to which it adds the path of the BPMS API,
 * and the header with the basic authentication of {@code config/application-rest-sync.yaml}. A
 * test server of its own plays the cockpit.
 */
@ExtendWith(SuppressOutputExtension.class)
class TheClientReportsToTheCockpitTest {

  /** One request the test server received. */
  record Request(
                 String path,
                 String authorization,
                 String body) {
  }

  @SpringBootConfiguration
  @EnableAutoConfiguration
  @Import(ClientsConfiguration.class)
  static class TheClientOnly {
  }

  private HttpServer cockpit;

  private final List<Request> received = new ArrayList<>();

  @BeforeEach
  void startTheCockpit() throws IOException {

    cockpit = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    cockpit.createContext("/", exchange -> {
      received.add(new Request(
          exchange.getRequestURI().getPath(), exchange.getRequestHeaders().getFirst(
              "Authorization"), new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
      exchange.sendResponseHeaders(HttpStatus.OK.value(), -1);
      exchange.close();
    });
    cockpit.start();

  }

  @AfterEach
  void stopTheCockpit() {

    cockpit.stop(0);

  }

  private ConfigurableApplicationContext theSimulatorWith(
      final String baseUrl) {

    return new SpringApplicationBuilder(TheClientOnly.class)
        .web(WebApplicationType.NONE)
        .profiles("rest-sync")
        // an argument, so it wins over the address of config/application-rest-sync.yaml
        .run("--business-cockpit.base-url="
            + baseUrl);

  }

  @Test
  void aReportGoesToThePathOfTheApiBelowTheAddressWithTheLogin() {

    try (var simulator = theSimulatorWith("http://localhost:%d/".formatted(cockpit.getAddress().getPort()))) {
      simulator
          .getBean(BpmsApi.class)
          .registerWorkflowModule("simulator", new RegisterWorkflowModuleEvent().uri("http://localhost:8079"));
    }

    assertThat(received).hasSize(1);
    assertThat(received.getFirst().path()).isEqualTo("/bpms/api/v1_1/workflow-module/simulator");
    assertThat(received.getFirst().authorization()).isEqualTo("Basic YWJjOjEyMw==");
    assertThat(received.getFirst().body()).contains("\"uri\":\"http://localhost:8079\"");

  }

  @Test
  void anAddressWithThePathOfTheApiStopsTheStart() {

    assertThatThrownBy(() -> theSimulatorWith(
        "http://localhost:%d/bpms/api/v1_1".formatted(cockpit.getAddress().getPort())))
        .rootCause()
        .hasMessageContaining("business-cockpit.base-url")
        .hasMessageContaining("/bpms/api/v1_1");

  }

}
