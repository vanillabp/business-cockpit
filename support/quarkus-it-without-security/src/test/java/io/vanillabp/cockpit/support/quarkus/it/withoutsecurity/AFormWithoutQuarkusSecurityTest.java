package io.vanillabp.cockpit.support.quarkus.it.withoutsecurity;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.junit.QuarkusTest;
import io.vanillabp.cockpit.support.usertask.BcTaskClaimedByAnotherUserException;
import io.vanillabp.cockpit.support.usertask.BcTaskNotOpenException;
import io.vanillabp.cockpit.support.usertask.BcUnknownTaskException;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A Quarkus application with the support and Quarkus REST, but without quarkus-security and
 * without any setting of the cockpit. It builds and starts, and the mappers in the support's jar
 * answer the exceptions of the form. The user context is there as well, and nobody is logged in.
 */
@QuarkusTest
@ExtendWith(SuppressOutputExtension.class)
public class AFormWithoutQuarkusSecurityTest {

  @Test
  public void theFormIsCompleted() {

    given()
        .header("X-User", "john")
        .post("/loan-approval/assess-risk/task-1")
        .then()
        .statusCode(200)
        .body(equalTo("completed by john"));

  }

  @Test
  public void withoutAUserTheFormIsUnauthorized() {

    given()
        .post("/loan-approval/assess-risk/task-1")
        .then()
        .statusCode(401)
        .contentType(startsWith("text/plain"))
        .body(equalTo("No user"));

  }

  @Test
  public void theUserContextKnowsNobody() {

    given()
        .get("/loan-approval/who-am-i")
        .then()
        .statusCode(401)
        .contentType(startsWith("text/plain"))
        .body(equalTo("User anonymous"));

  }

  @Test
  public void anUnknownTaskIsNotFound() {

    given()
        .header("X-User", "john")
        .post("/loan-approval/assess-risk/task-9")
        .then()
        .statusCode(BcUnknownTaskException.HTTP_STATUS_CODE)
        .body(equalTo("There is no such user task 'task-9' in 'loan-approval'"));

  }

  @Test
  public void aTaskWhichIsNotOpenAnyMoreIsNotFound() {

    given()
        .header("X-User", "john")
        .post("/loan-approval/assess-risk/task-2")
        .then()
        .statusCode(BcTaskNotOpenException.HTTP_STATUS_CODE)
        .contentType(startsWith("text/plain"))
        .body(equalTo("The user task 'task-2' of 'loan-approval' is not open any more"));

  }

  @Test
  public void aTaskWhichAnotherUserClaimedIsAConflict() {

    given()
        .header("X-User", "john")
        .post("/loan-approval/assess-risk/task-3")
        .then()
        .statusCode(BcTaskClaimedByAnotherUserException.HTTP_STATUS_CODE)
        .contentType(startsWith("text/plain"))
        .body(equalTo("The user task 'task-3' of 'loan-approval' is claimed by 'mary'"));

  }

}
