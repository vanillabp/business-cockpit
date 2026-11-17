package io.vanillabp.cockpit.support.quarkus.it;

import java.util.Map;

import io.vanillabp.cockpit.support.security.usercontext.UserContext;
import io.vanillabp.cockpit.support.usertask.BcTaskClaimedByAnotherUserException;
import io.vanillabp.cockpit.support.usertask.BcTaskNotOpenException;
import io.vanillabp.cockpit.support.usertask.BcUnknownTaskException;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

/**
 * The form of a user task the way a workflow module writes it: only a clerk may open it, the user
 * comes from the user context, and the exceptions of a user task say that a task is unknown, not
 * open any more, or claimed by another user. Nothing here deals with the cookie or with status
 * codes.
 */
@Path("/loan-approval")
public class AssessRiskForm {

  private static final String WORKFLOW = "loan-approval";

  /**
   * The tasks of the workflow: two open, one done.
   */
  private static final Map<String, Boolean> OPEN_TASKS = Map.of("task-1", true, "task-2", false, "task-3", true);

  /**
   * The open tasks which a user claimed, and who claimed them.
   */
  private static final Map<String, String> CLAIMED_BY = Map.of("task-3", "mary");

  @Inject
  UserContext userContext;

  @GET
  @Path("/assess-risk/{taskId}")
  @RolesAllowed("CLERK")
  public String load(
      @PathParam("taskId") final String taskId) {

    openTask(taskId);
    return "Assess the risk, "
        + userContext.getUserLoggedInDetails().getDisplay();

  }

  @POST
  @Path("/assess-risk/{taskId}")
  @RolesAllowed("CLERK")
  public String complete(
      @PathParam("taskId") final String taskId) {

    openTask(taskId);
    final var claimedBy = CLAIMED_BY.get(taskId);
    if ((claimedBy != null) && !claimedBy.equals(userContext.getUserLoggedIn())) {
      throw new BcTaskClaimedByAnotherUserException(WORKFLOW, taskId, claimedBy);
    }
    return "completed by "
        + userContext.getUserLoggedIn();

  }

  /**
   * Open to everybody, so the user context itself has to say that nobody is logged in.
   *
   * @return The user id
   */
  @GET
  @Path("/who-am-i")
  public String whoAmI() {

    return userContext.getUserLoggedIn();

  }

  private static void openTask(
      final String taskId) {

    final var open = OPEN_TASKS.get(taskId);
    if (open == null) {
      throw new BcUnknownTaskException(WORKFLOW, taskId);
    }
    if (!open) {
      throw new BcTaskNotOpenException(WORKFLOW, taskId);
    }

  }

}
