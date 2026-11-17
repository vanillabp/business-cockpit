package io.vanillabp.cockpit.support.quarkus.it;

import java.util.Map;

import io.vanillabp.cockpit.support.security.usercontext.UserContext;
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
 * comes from the user context, and the exceptions of a user task say that a task is unknown or not
 * open any more. Nothing here deals with the cookie or with status codes.
 */
@Path("/loan-approval")
public class AssessRiskForm {

  private static final String WORKFLOW = "loan-approval";

  /**
   * The tasks of the workflow: one open, one done.
   */
  private static final Map<String, Boolean> OPEN_TASKS = Map.of("task-1", true, "task-2", false);

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
