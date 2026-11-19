package io.vanillabp.cockpit.support.userinterface;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

/**
 * An application with {@code vertx-http-proxy} and the address of the nginx. The workflow module
 * {@code loan-approval} has no class for its route, only its file {@code META-INF/workflow-module}.
 * The files of its user interface come from the nginx, even though the application has a manifest
 * of a UI jar as well, and its API stays in the application. A permission of the application covers
 * the route. The twin of the test of the same name in the
 * Spring Boot module.
 */
@ExtendWith(SuppressOutputExtension.class)
public class TheUserInterfaceComesFromTheNginxTest {

  @RegisterExtension
  static final QuarkusExtensionTest application = new QuarkusExtensionTest()
      .withApplicationRoot(jar -> jar
          .addClasses(UserInterfaceRoutesOnQuarkus.class, VertxProxyRoute.class, ModuleApi.class, EchoingNginx.class)
          .addAsResource(new StringAsset("loan-approval"), UserInterfaceRoutes.WORKFLOW_MODULE_FILE)
          .addAsResource(new StringAsset("from the UI jar"), "META-INF/resources/loan-approval/bc-manifest.json"))
      .overrideConfigKey(UserInterfaceRoute.keyOf("loan-approval"), EchoingNginx.address())
      .overrideConfigKey("quarkus.http.auth.permission.protected.paths", "/loan-approval/protected/*")
      .overrideConfigKey("quarkus.http.auth.permission.protected.policy", "authenticated");

  /** The API of the workflow module, below {@code /loan-approval/api/}. */
  @Path("/loan-approval")
  public static class ModuleApi {

    @GET
    @Path("/api/ping")
    public String ping() {

      return "pong";

    }

  }

  @Test
  public void aFileOfTheUserInterfaceComesFromTheNginx() {

    given()
        .get("/loan-approval/bc-manifest.json?version=2")
        .then()
        .statusCode(Response.Status.OK.getStatusCode())
        .contentType(startsWith("text/plain"))
        .body(equalTo("nginx got GET /loan-approval/bc-manifest.json?version=2 for "
            + EchoingNginx.address().substring("http://".length())));

  }

  /**
   * The JDK's client asks Quarkus to switch to HTTP/2 without TLS, and Quarkus does. A header of a
   * single connection, like {@code Connection}, is not allowed in HTTP/2, and the client refuses an
   * answer which has one. So the route leaves out every such header of the nginx.
   */
  @Test
  public void aFileComesOverHttp2WithoutTheHeadersOfTheConnectionToTheNginx() throws Exception {

    final var response = fetch(HttpClient.Version.HTTP_2, "/loan-approval/bc-manifest.json");

    assertThat(response.version()).isEqualTo(HttpClient.Version.HTTP_2);
    assertThat(response.statusCode()).isEqualTo(Response.Status.OK.getStatusCode());
    assertThat(response.body()).startsWith("nginx got GET /loan-approval/bc-manifest.json");
    assertThat(response.headers().map().keySet())
        .doesNotContain("connection", "keep-alive", "x-hop", "proxy-connection", "transfer-encoding", "upgrade");
    assertThat(response.headers().firstValue("content-type")).hasValue("text/plain");

  }

  @Test
  public void aFileComesOverHttp1WithoutTheHeadersOfTheConnectionToTheNginx() throws Exception {

    final var response = fetch(HttpClient.Version.HTTP_1_1, "/loan-approval/bc-manifest.json");

    assertThat(response.version()).isEqualTo(HttpClient.Version.HTTP_1_1);
    assertThat(response.statusCode()).isEqualTo(Response.Status.OK.getStatusCode());
    assertThat(response.body()).startsWith("nginx got GET /loan-approval/bc-manifest.json");
    assertThat(response.headers().map().keySet()).doesNotContain("keep-alive", "x-hop");
    assertThat(response.headers().allValues("connection")).noneMatch(value -> value.contains("X-Hop"));

  }

  private static HttpResponse<String> fetch(
      final HttpClient.Version version,
      final String path) throws Exception {

    try (var client = HttpClient
        .newBuilder()
        .version(version)
        .build()) {
      return client.send(
          HttpRequest
              .newBuilder(URI.create("http://localhost:"
                  + RestAssured.port
                  + path))
              .GET()
              .build(),
          HttpResponse.BodyHandlers.ofString());
    }

  }

  @Test
  public void anAnswerOfTheNginxIsPassedOnAsItIs() {

    given()
        .get("/loan-approval/entries/missing.js")
        .then()
        .statusCode(Response.Status.NOT_FOUND.getStatusCode())
        .body(startsWith("nginx got GET /loan-approval/entries/missing.js"));

  }

  @Test
  public void theApiOfTheModuleStaysInTheApplication() {

    given()
        .get("/loan-approval/api/ping")
        .then()
        .statusCode(Response.Status.OK.getStatusCode())
        .body(equalTo("pong"));

  }

  @Test
  public void onlyGetIsForwarded() {

    given()
        .body("x")
        .post("/loan-approval/bc-manifest.json")
        .then()
        .body(not(startsWith("nginx got")));

  }

  @Test
  public void aPathWhichCouldLeaveTheModuleIsNotForwarded() {

    given()
        .urlEncodingEnabled(false)
        .get("/loan-approval/%2e%2e/secret")
        .then()
        .body(not(startsWith("nginx got")));

  }

  /**
   * The application of the test has no way to log in, so Quarkus refuses the anonymous request
   * with 403 and not with 401. What counts is that the nginx never sees it.
   */
  @Test
  public void thePermissionsOfTheApplicationCoverTheRoute() {

    given()
        .get("/loan-approval/protected/file.js")
        .then()
        .statusCode(Response.Status.FORBIDDEN.getStatusCode())
        .body(not(startsWith("nginx got")));

  }

}
