package io.vanillabp.cockpit.workflowmodule.onsupportonly;

import java.util.List;
import java.util.Map;

import io.vanillabp.cockpit.support.security.usercontext.UserDetails;
import io.vanillabp.cockpit.support.security.usercontext.UserDetailsImpl;
import io.vanillabp.spi.cockpit.BusinessCockpitService;
import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetailsProvider;
import io.vanillabp.spi.cockpit.workflow.PrefilledWorkflowDetails;
import io.vanillabp.spi.cockpit.workflow.WorkflowDetails;
import io.vanillabp.spi.cockpit.workflow.WorkflowDetailsProvider;
import io.vanillabp.spi.service.BpmnProcess;
import io.vanillabp.spi.service.TaskId;
import io.vanillabp.spi.service.WorkflowService;
import io.vanillabp.spi.service.WorkflowTask;

/**
 * The workflow service of the loan approval, written the way a workflow module behind the Business
 * Cockpit writes it. Every annotation and type it uses comes with the module {@code support}: the
 * SPI of VanillaBP, the SPI of the cockpit and the user. The module exists for the compiler only.
 * Nothing runs it, because running it needs a BPMS and the cockpit's adapter for it.
 */
@WorkflowService(
    workflowAggregateClass = LoanApproval.class,
    bpmnProcess = @BpmnProcess(bpmnProcessId = "LoanApproval"))
public class LoanApprovalService {

  private final BusinessCockpitService<LoanApproval> businessCockpitService;

  /**
   * Creates the service.
   *
   * @param businessCockpitService Tells the cockpit about changed business data
   */
  public LoanApprovalService(
      final BusinessCockpitService<LoanApproval> businessCockpitService) {
    this.businessCockpitService = businessCockpitService;
  }

  /**
   * Waits for a clerk to decide about the loan request.
   *
   * @param loanApproval The workflow aggregate
   * @param taskId The id of the user task
   */
  @WorkflowTask
  public void assessRisk(
      final LoanApproval loanApproval,
      @TaskId final String taskId) {
    businessCockpitService.aggregateChanged(loanApproval, taskId);
  }

  /**
   * The details of the user task {@code assessRisk}, as the cockpit shows them.
   *
   * @param loanApproval The workflow aggregate
   * @param prefilled What the BPMS reported about the user task
   * @return The details
   */
  @UserTaskDetailsProvider
  public UserTaskDetails assessRisk(
      final LoanApproval loanApproval,
      final PrefilledUserTaskDetails prefilled) {
    prefilled.setTitle(Map.of("en", "Assess the risk of "
        + loanApproval.getLoanRequestId()));
    final UserDetails clerk = clerkOf(loanApproval);
    prefilled.setAssignee(clerk.getId());
    return prefilled;
  }

  /**
   * The details of the workflow, as the cockpit shows them.
   *
   * @param loanApproval The workflow aggregate
   * @param prefilled What the BPMS reported about the workflow
   * @return The details
   */
  @WorkflowDetailsProvider
  public WorkflowDetails workflowDetails(
      final LoanApproval loanApproval,
      final PrefilledWorkflowDetails prefilled) {
    return prefilled;
  }

  private UserDetails clerkOf(
      final LoanApproval loanApproval) {
    if (loanApproval.getClerk() == null) {
      return new UserDetailsImpl("nobody", null, "Nobody", "-", List.of());
    }
    return new Clerk(loanApproval.getClerk(), loanApproval.getClerk(), List.of("clerks"));
  }

}
