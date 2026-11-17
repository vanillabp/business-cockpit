package io.vanillabp.cockpit.workflowmodule.onquarkussupportonly;

import java.util.Set;

import io.vanillabp.cockpit.support.usertask.BcTaskNotOpenException;
import io.vanillabp.cockpit.support.usertask.BcUnknownTaskException;

/**
 * What the form of the user task {@code assessRisk} checks before it takes the answer of a clerk.
 * It throws two exceptions of a user task, and both come with the Quarkus module of
 * {@code support}. In a real workflow module this is a REST endpoint, and the module's mappers
 * answer both exceptions with {@code 404}. This module has no REST layer, because Quarkus
 * is the application's choice and so not part of what {@code support} brings.
 */
public class AssessRiskForm {

  private final Set<String> openTaskIds;

  private final Set<String> doneTaskIds;

  /**
   * Creates the form.
   *
   * @param openTaskIds The ids of the tasks a clerk can still answer
   * @param doneTaskIds The ids of the tasks which were completed or canceled already
   */
  public AssessRiskForm(
      final Set<String> openTaskIds,
      final Set<String> doneTaskIds) {
    this.openTaskIds = Set.copyOf(openTaskIds);
    this.doneTaskIds = Set.copyOf(doneTaskIds);
  }

  /**
   * Checks that the task named by the form can take an answer.
   *
   * @param loanApproval The workflow aggregate
   * @param taskId The id of the user task the form names
   */
  public void checkTaskIsOpen(
      final LoanApproval loanApproval,
      final String taskId) {
    if (doneTaskIds.contains(taskId)) {
      throw new BcTaskNotOpenException(loanApproval.getLoanRequestId(), taskId);
    }
    if (!openTaskIds.contains(taskId)) {
      throw new BcUnknownTaskException(loanApproval.getLoanRequestId(), taskId);
    }
  }

}
