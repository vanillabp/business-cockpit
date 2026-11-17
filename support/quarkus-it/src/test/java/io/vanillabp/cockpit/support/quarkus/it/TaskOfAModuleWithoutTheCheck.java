package io.vanillabp.cockpit.support.quarkus.it;

import io.vanillabp.cockpit.support.usertask.BcTaskClaimedByAnotherUserException;
import io.vanillabp.cockpit.support.usertask.BcTaskNotOpenException;
import io.vanillabp.cockpit.support.usertask.BcUnknownTaskException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

/**
 * A form which needs no role, the way a workflow module writes it which does not read the cockpit's
 * login token. It only throws the exceptions of a user task.
 */
@Path("/without-the-check")
public class TaskOfAModuleWithoutTheCheck {

  private static final String WORKFLOW = "loan-approval";

  @POST
  @Path("/assess-risk/{taskId}")
  public String complete(
      @PathParam("taskId") final String taskId) {

    switch (taskId) {
      case "task-1":
        return "completed";
      case "task-2":
        throw new BcTaskNotOpenException(WORKFLOW, taskId);
      case "task-3":
        throw new BcTaskClaimedByAnotherUserException(WORKFLOW, taskId, "mary");
      default:
        throw new BcUnknownTaskException(WORKFLOW, taskId);
    }

  }

}
