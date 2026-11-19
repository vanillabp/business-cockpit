package io.vanillabp.cockpit.support.quarkus.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.vanillabp.cockpit.support.security.jwt.LoginTokens;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import jakarta.ws.rs.core.Response;

/**
 * A workflow module with a login of its own, HTTP basic authentication here, keeps the check of the
 * cockpit's cookie. Both work side by side: a request from the cockpit's user interface carries the
 * cookie, and a request of another client logs in the module's own way. The cookie's check is
 * passive, so a request without the cookie goes on to the other login. Nothing has to be switched
 * off, see {@link ALoginOfItsOwnNextToTheCookie}.
 */
@QuarkusTest
@TestProfile(ALoginOfItsOwnNextToTheCookie.class)
@ExtendWith(SuppressOutputExtension.class)
public class ALoginOfItsOwnNextToTheCookieTest {

  private static final byte[] SECRET = Base64.getDecoder().decode("9kt7o2AC1AF/tV55Txl2MV5cnjtVr6azrL7I2ID0BUw=");

  @Test
  public void theUserOfTheCookieIsLoggedIn() {

    given()
        .cookie("bc", LoginTokens.signed(SECRET, "john"))
        .get("/loan-approval/who-am-i")
        .then()
        .statusCode(Response.Status.OK.getStatusCode())
        .body(equalTo("john"));

  }

  @Test
  public void theUserOfTheModulesOwnLoginIsLoggedIn() {

    given()
        .auth()
        .preemptive()
        .basic("ann", "secret")
        .post("/loan-approval/assess-risk/task-1")
        .then()
        .statusCode(Response.Status.OK.getStatusCode())
        .body(equalTo("completed by ann"));

  }

  @Test
  public void aWrongPasswordIsUnauthorized() {

    given()
        .auth()
        .preemptive()
        .basic("ann", "wrong")
        .post("/loan-approval/assess-risk/task-1")
        .then()
        .statusCode(Response.Status.UNAUTHORIZED.getStatusCode());

  }

  @Test
  public void withoutAnyLoginTheFormIsUnauthorized() {

    given()
        .post("/loan-approval/assess-risk/task-1")
        .then()
        .statusCode(Response.Status.UNAUTHORIZED.getStatusCode());

  }

}
