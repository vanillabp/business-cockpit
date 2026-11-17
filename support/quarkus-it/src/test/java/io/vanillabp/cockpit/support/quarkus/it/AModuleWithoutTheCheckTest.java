package io.vanillabp.cockpit.support.quarkus.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.vanillabp.cockpit.support.security.jwt.LoginTokens;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The same application with quarkus-security, but with the check of the login token switched off
 * by {@code business-cockpit.jwt.enabled=false}. It starts, the cookie is not read, and the
 * mappers still answer the exceptions of a user task.
 */
@QuarkusTest
@TestProfile(TheCheckSwitchedOff.class)
@ExtendWith(SuppressOutputExtension.class)
public class AModuleWithoutTheCheckTest {

  private static final byte[] SECRET = Base64.getDecoder().decode("9kt7o2AC1AF/tV55Txl2MV5cnjtVr6azrL7I2ID0BUw=");

  @Test
  public void theFormIsCompleted() {

    given()
        .post("/without-the-check/assess-risk/task-1")
        .then()
        .statusCode(200)
        .body(equalTo("completed"));

  }

  @Test
  public void theCookieIsNotReadSoTheUserContextIsUnauthorized() {

    given()
        .cookie("bc", LoginTokens.signed(SECRET, "john"))
        .get("/loan-approval/who-am-i")
        .then()
        .statusCode(401)
        .contentType(startsWith("text/plain"))
        .body(equalTo("User anonymous"));

  }

  @Test
  public void anUnknownTaskIsNotFound() {

    given()
        .post("/without-the-check/assess-risk/task-9")
        .then()
        .statusCode(404)
        .body(equalTo("There is no such user task 'task-9' in 'loan-approval'"));

  }

  @Test
  public void aTaskWhichIsNotOpenAnyMoreIsNotFound() {

    given()
        .post("/without-the-check/assess-risk/task-2")
        .then()
        .statusCode(404)
        .contentType(startsWith("text/plain"))
        .body(equalTo("The user task 'task-2' of 'loan-approval' is not open any more"));

  }

  @Test
  public void aTaskWhichAnotherUserClaimedIsAConflict() {

    given()
        .post("/without-the-check/assess-risk/task-3")
        .then()
        .statusCode(409)
        .contentType(startsWith("text/plain"))
        .body(equalTo("The user task 'task-3' of 'loan-approval' is claimed by 'mary'"));

  }

}
