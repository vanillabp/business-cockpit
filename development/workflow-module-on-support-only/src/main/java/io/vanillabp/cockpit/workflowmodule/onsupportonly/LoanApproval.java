package io.vanillabp.cockpit.workflowmodule.onsupportonly;

/**
 * The workflow aggregate of the loan approval: the business data one run of the workflow works on.
 */
public class LoanApproval {

  private String loanRequestId;

  private String clerk;

  /**
   * The id of the request for a loan this run decides about.
   *
   * @return The id
   */
  public String getLoanRequestId() {
    return loanRequestId;
  }

  /**
   * Sets the id of the request for a loan.
   *
   * @param loanRequestId The id
   */
  public void setLoanRequestId(
      final String loanRequestId) {
    this.loanRequestId = loanRequestId;
  }

  /**
   * The id of the clerk who decides about the request.
   *
   * @return The id, or {@code null} if no clerk was chosen yet
   */
  public String getClerk() {
    return clerk;
  }

  /**
   * Sets the clerk who decides about the request.
   *
   * @param clerk The id of the clerk
   */
  public void setClerk(
      final String clerk) {
    this.clerk = clerk;
  }

}
