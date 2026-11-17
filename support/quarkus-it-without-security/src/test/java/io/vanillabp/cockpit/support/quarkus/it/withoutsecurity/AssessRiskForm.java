package io.vanillabp.cockpit.support.quarkus.it.withoutsecurity;

import io.vanillabp.cockpit.support.security.BcUnauthorizedException;
import io.vanillabp.cockpit.support.security.usercontext.UserContext;
import io.vanillabp.cockpit.support.usertask.BcTaskClaimedByAnotherUserException;
import io.vanillabp.cockpit.support.usertask.BcTaskNotOpenException;
import io.vanillabp.cockpit.support.usertask.BcUnknownTaskException;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

/**
 * The form of a user task in a workflow module which has its own way to tell the user, here a
 * header, and only throws the exceptions of a user task. It knows nothing about status codes.
 */
@Path("/loan-approval")
public class AssessRiskForm {

  private static final String WORKFLOW = "loan-approval";

  @Inject
  UserContext userContext;

  @POST
  @Path("/assess-risk/{taskId}")
  public String complete(
      @PathParam("taskId") final String taskId,
      @HeaderParam("X-User") final String user) {

    if (user == null) {
      throw new BcUnauthorizedException("No user");
    }
    switch (taskId) {
      case "task-1":
        return "completed by "
            + user;
      case "task-2":
        throw new BcTaskNotOpenException(WORKFLOW, taskId);
      case "task-3":
        throw new BcTaskClaimedByAnotherUserException(WORKFLOW, taskId, "mary");
      default:
        throw new BcUnknownTaskException(WORKFLOW, taskId);
    }

  }

  /**
   * Asks the user context, which has no login to read in this application.
   *
   * @return The user id, never
   */
  @GET
  @Path("/who-am-i")
  public String whoAmI() {

    return userContext.getUserLoggedIn();

  }

}
