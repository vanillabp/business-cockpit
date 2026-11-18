package io.vanillabp.cockpit.gui.api.v1.client.quarkus;

import io.vanillabp.cockpit.gui.api.v1.client.OfficialCurrentUserApi;
import io.vanillabp.cockpit.gui.api.v1.client.OfficialTasklistApi;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * The form endpoint of a workflow module. While it answers the user's request it asks the cockpit
 * whom the task is assigned to and who the user is, the way a form decides between its answer and
 * 409 for a task another user claimed.
 */
@Path("/loan-approval/assess-risk/{taskId}")
public class AssessRiskForm {

  @Inject
  @LoanApprovalCockpit.LoanApproval
  OfficialTasklistApi tasklist;

  @Inject
  @LoanApprovalCockpit.LoanApproval
  OfficialCurrentUserApi currentUser;

  /**
   * Says whether the user holds the task.
   *
   * @param taskId the id of the task
   * @return what the form would do
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String open(
      @PathParam("taskId") final String taskId) {

    final var assignee = tasklist.getUserTask(taskId, null).getAssignee().getId();
    final var user = currentUser.currentUser().getId();
    return assignee.equals(user)
        ? "the form for "
            + user
        : "claimed by "
            + assignee;

  }

}
