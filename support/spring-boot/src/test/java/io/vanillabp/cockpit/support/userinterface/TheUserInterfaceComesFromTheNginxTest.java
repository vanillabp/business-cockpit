package io.vanillabp.cockpit.support.userinterface;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;

/**
 * An application with the gateway and the address of the nginx. The workflow module
 * {@code loan-approval}, named by {@code META-INF/workflow-module} of the test resources, has no
 * class for its route. The files of its user interface come from the nginx, a stub server here,
 * even though the test resources hold a manifest below {@code META-INF/resources/} as a UI jar
 * would, and its API stays in the application. The security of the application covers the route.
 */
@SpringBootTest(classes = TheUserInterfaceComesFromTheNginxTest.Application.class,
    webEnvironment = WebEnvironment.RANDOM_PORT)
@ExtendWith(SuppressOutputExtension.class)
public class TheUserInterfaceComesFromTheNginxTest {

  private static final MockWebServer nginx = new MockWebServer();

  private final HttpClient client = HttpClient.newHttpClient();

  @Autowired
  private Environment environment;

  @Autowired
  private ApplicationContext context;

  @DynamicPropertySource
  static void nginxAddress(
      final DynamicPropertyRegistry registry) throws IOException {

    if (!nginx.getStarted()) {
      nginx.start(0);
    }
    registry.add(UserInterfaceRoute.keyOf("loan-approval"), () -> "http://localhost:"
        + nginx.getPort());

  }

  @AfterAll
  static void stopTheNginx() {

    nginx.close();

  }

  @BeforeEach
  void forgetEarlierRequests() throws InterruptedException {

    while (nginx.takeRequest(10, TimeUnit.MILLISECONDS) != null) {
      // forget it
    }

  }

  @Test
  public void aFileOfTheUserInterfaceComesFromTheNginx() throws Exception {

    nginx.enqueue(new MockResponse.Builder()
        .code(HttpStatus.OK.value())
        .addHeader("Content-Type", "application/json")
        .body("{\"entries\":[]}")
        .build());

    final var response = get("/loan-approval/bc-manifest.json?version=2");

    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(response.body()).isEqualTo("{\"entries\":[]}");
    assertThat(response.headers().firstValue("Content-Type")).hasValue("application/json");
    final RecordedRequest forwarded = nginx.takeRequest(5, TimeUnit.SECONDS);
    assertThat(forwarded).isNotNull();
    assertThat(forwarded.getMethod()).isEqualTo("GET");
    assertThat(forwarded.getTarget()).isEqualTo("/loan-approval/bc-manifest.json?version=2");

  }

  @Test
  public void anAnswerOfTheNginxIsPassedOnAsItIs() throws Exception {

    nginx.enqueue(new MockResponse.Builder()
        .code(HttpStatus.NOT_FOUND.value())
        .body("not here")
        .build());

    final var response = get("/loan-approval/entries/missing.js");

    assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(response.body()).isEqualTo("not here");

  }

  @Test
  public void theApiOfTheModuleStaysInTheApplication() throws Exception {

    final var response = get("/loan-approval/api/ping");

    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(response.body()).isEqualTo("pong");
    assertThat(nginx.takeRequest(200, TimeUnit.MILLISECONDS)).isNull();

  }

  @Test
  public void onlyGetIsForwarded() throws Exception {

    final var response = client.send(
        HttpRequest
            .newBuilder(address("/loan-approval/bc-manifest.json"))
            .POST(HttpRequest.BodyPublishers.ofString("x"))
            .build(),
        HttpResponse.BodyHandlers.ofString());

    assertThat(response.statusCode()).isNotEqualTo(HttpStatus.OK.value());
    assertThat(nginx.takeRequest(200, TimeUnit.MILLISECONDS)).isNull();

  }

  @Test
  public void theSecurityOfTheApplicationCoversTheRoute() throws Exception {

    final var response = get("/loan-approval/protected/file.js");

    assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(nginx.takeRequest(200, TimeUnit.MILLISECONDS)).isNull();

  }

  @Test
  public void theRouteIsABeanNamedAfterTheModule() {

    assertThat(context.containsBean("loan-approval-user-interface")).isTrue();

  }

  private HttpResponse<String> get(
      final String path) throws Exception {

    return client.send(HttpRequest.newBuilder(address(path)).GET().build(), HttpResponse.BodyHandlers.ofString());

  }

  private URI address(
      final String path) {

    return URI.create("http://localhost:"
        + environment.getProperty("local.server.port")
        + path);

  }

  /** The application: the API of the module, and a security which protects one path. */
  @SpringBootConfiguration
  @EnableAutoConfiguration
  static class Application {

    @Bean
    ModuleApi moduleApi() {

      return new ModuleApi();

    }

    @Bean
    SecurityFilterChain security(
        final HttpSecurity http) throws Exception {

      return http
          .authorizeHttpRequests(requests -> requests
              .requestMatchers("/loan-approval/protected/**")
              .authenticated()
              .anyRequest()
              .permitAll())
          .httpBasic(Customizer.withDefaults())
          .csrf(csrf -> csrf.disable())
          .build();

    }

  }

  /** The API of the workflow module, below {@code /loan-approval/api/}. */
  @RestController
  static class ModuleApi {

    @GetMapping("/loan-approval/api/ping")
    String ping() {

      return "pong";

    }

  }

}
