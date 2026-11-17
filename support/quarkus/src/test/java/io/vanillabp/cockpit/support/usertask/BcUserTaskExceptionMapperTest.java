package io.vanillabp.cockpit.support.usertask;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.cockpit.support.security.BcUnauthorizedException;
import io.vanillabp.cockpit.support.security.BcUnauthorizedExceptionMapper;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

/**
 * A REST endpoint of a workflow module throws one of the exceptions, and the mappers turn it into
 * the status and the message a form can show. It is the twin of the Spring Boot test
 * {@code BcUserTaskExceptionHandlerTest}, and it expects the same answers.
 * <p>
 * The test framework builds the application only out of what the test names, and leaves the main
 * classes of this module out. So the mappers are named here. That an application finds them in the
 * jar of this module on its own is what {@link TheMappersAreInTheJandexIndexTest} checks.
 */
@ExtendWith(SuppressOutputExtension.class)
public class BcUserTaskExceptionMapperTest {

  /**
   * Stands in for the form endpoint of a workflow module.
   */
  @Path("/")
  public static class FormResource {

    @POST
    @Path("/unknown/{workflowId}/{taskId}")
    public String unknown(
        @PathParam("workflowId") final String workflowId,
        @PathParam("taskId") final String taskId) {

      throw new BcUnknownTaskException(workflowId, taskId);

    }

    @POST
    @Path("/done/{workflowId}/{taskId}")
    public String done(
        @PathParam("workflowId") final String workflowId,
        @PathParam("taskId") final String taskId) {

      throw new BcTaskNotOpenException(workflowId, taskId);

    }

    @POST
    @Path("/claimed/{workflowId}/{taskId}/{claimedBy}")
    public String claimed(
        @PathParam("workflowId") final String workflowId,
        @PathParam("taskId") final String taskId,
        @PathParam("claimedBy") final String claimedBy) {

      throw new BcTaskClaimedByAnotherUserException(workflowId, taskId, claimedBy);

    }

    @POST
    @Path("/anonymous")
    public String anonymous() {

      throw new BcUnauthorizedException("Nobody is logged in");

    }

  }

  @RegisterExtension
  static final QuarkusExtensionTest application = new QuarkusExtensionTest()
      .withApplicationRoot(jar -> jar
          .addClass(FormResource.class)
          .addClass(BcUserTaskExceptionMapper.class)
          .addClass(BcUnauthorizedExceptionMapper.class));

  @Test
  public void anUnknownTaskIsNotFound() {

    when()
        .post("/unknown/loan-1/task-9")
        .then()
        .statusCode(404)
        .contentType(startsWith("text/plain"))
        .body(equalTo("There is no such user task 'task-9' in 'loan-1'"));

  }

  @Test
  public void aTaskWhichIsNotOpenAnyMoreIsNotFound() {

    when()
        .post("/done/loan-1/task-9")
        .then()
        .statusCode(404)
        .contentType(startsWith("text/plain"))
        .body(equalTo("The user task 'task-9' of 'loan-1' is not open any more"));

  }

  @Test
  public void aTaskWhichAnotherUserClaimedIsAConflict() {

    when()
        .post("/claimed/loan-1/task-9/mary")
        .then()
        .statusCode(409)
        .contentType(startsWith("text/plain"))
        .body(equalTo("The user task 'task-9' of 'loan-1' is claimed by 'mary'"));

  }

  @Test
  public void aRequestWithoutALoginIsUnauthorized() {

    when()
        .post("/anonymous")
        .then()
        .statusCode(401)
        .contentType(startsWith("text/plain"))
        .body(equalTo("Nobody is logged in"));

  }

}
