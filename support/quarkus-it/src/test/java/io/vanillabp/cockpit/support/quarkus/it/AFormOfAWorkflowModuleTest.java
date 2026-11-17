package io.vanillabp.cockpit.support.quarkus.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.junit.QuarkusTest;
import io.vanillabp.cockpit.support.security.jwt.LoginTokens;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A Quarkus application with the support as a plain dependency, the way a workflow module has it.
 * Quarkus finds the check of the login token, the user context and the mappers in the jar on its
 * own. The browser sends the cockpit's cookie, and the form answers with the user of the token, or
 * with 401, 403, 404 or 409.
 * <p>
 * The secret comes as an environment variable, see the Surefire configuration of this module.
 */
@QuarkusTest
@ExtendWith(SuppressOutputExtension.class)
public class AFormOfAWorkflowModuleTest {

  private static final byte[] SECRET = Base64.getDecoder().decode("9kt7o2AC1AF/tV55Txl2MV5cnjtVr6azrL7I2ID0BUw=");

  @Test
  public void aClerkOpensAndCompletesTheForm() {

    given()
        .cookie("bc", LoginTokens.signed(SECRET, "john"))
        .get("/loan-approval/assess-risk/task-1")
        .then()
        .statusCode(200)
        .body(equalTo("Assess the risk, Doe, John"));

    given()
        .cookie("bc", LoginTokens.signed(SECRET, "john"))
        .post("/loan-approval/assess-risk/task-1")
        .then()
        .statusCode(200)
        .body(equalTo("completed by john"));

  }

  @Test
  public void withoutTheCookieTheFormIsUnauthorized() {

    given()
        .get("/loan-approval/assess-risk/task-1")
        .then()
        .statusCode(401);

  }

  @Test
  public void withATokenOfAnotherSecretTheFormIsUnauthorized() {

    given()
        .cookie("bc", LoginTokens.signed(LoginTokens.newSecret(), "john"))
        .get("/loan-approval/assess-risk/task-1")
        .then()
        .statusCode(401);

  }

  @Test
  public void theUserContextOfAnAnonymousRequestIsUnauthorized() {

    given()
        .get("/loan-approval/who-am-i")
        .then()
        .statusCode(401)
        .contentType(startsWith("text/plain"))
        .body(equalTo("User anonymous"));

  }

  @Test
  public void aUserWhoIsNoClerkIsForbidden() {

    given()
        .cookie("bc", LoginTokens.signed(SECRET, "jane"))
        .get("/loan-approval/assess-risk/task-1")
        .then()
        .statusCode(403);

  }

  @Test
  public void anUnknownTaskIsNotFound() {

    given()
        .cookie("bc", LoginTokens.signed(SECRET, "john"))
        .post("/loan-approval/assess-risk/task-9")
        .then()
        .statusCode(404)
        .body(equalTo("There is no such user task 'task-9' in 'loan-approval'"));

  }

  @Test
  public void aTaskWhichIsNotOpenAnyMoreIsAConflict() {

    given()
        .cookie("bc", LoginTokens.signed(SECRET, "john"))
        .post("/loan-approval/assess-risk/task-2")
        .then()
        .statusCode(409)
        .body(equalTo("The user task 'task-2' of 'loan-approval' is not open any more"));

  }

}
