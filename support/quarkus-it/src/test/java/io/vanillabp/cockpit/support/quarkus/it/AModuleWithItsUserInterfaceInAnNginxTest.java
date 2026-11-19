package io.vanillabp.cockpit.support.quarkus.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import jakarta.ws.rs.core.Response;

/**
 * The workflow module {@code loan-approval} of this application is named by its file
 * {@code META-INF/workflow-module} and has no class for its route. The application has
 * {@code vertx-http-proxy} and sets the address of the nginx, see {@link TheUserInterfaceInAnNginx}.
 * Quarkus finds the route in the jar of the support on its own: the files of the user interface come
 * from the nginx, and the API of the module stays in the application.
 */
@QuarkusTest
@TestProfile(TheUserInterfaceInAnNginx.class)
@ExtendWith(SuppressOutputExtension.class)
public class AModuleWithItsUserInterfaceInAnNginxTest {

  @Test
  public void theManifestComesFromTheNginx() {

    given()
        .get("/loan-approval/bc-manifest.json")
        .then()
        .statusCode(Response.Status.OK.getStatusCode())
        .body(startsWith("nginx got GET /loan-approval/bc-manifest.json for 127.0.0.1:"));

  }

  @Test
  public void theApiOfTheModuleStaysInTheApplication() {

    given()
        .get("/loan-approval/api/ping")
        .then()
        .statusCode(Response.Status.OK.getStatusCode())
        .body(equalTo("pong"));

  }

}
