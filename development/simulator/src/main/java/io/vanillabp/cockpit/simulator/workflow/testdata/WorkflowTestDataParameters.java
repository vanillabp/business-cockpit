package io.vanillabp.cockpit.simulator.workflow.testdata;

/**
 * The parameters of the form which generates test workflows.
 */
public class WorkflowTestDataParameters {

  private int noOfEvents;

  private int percentageUpdates;

  private int noOfConcurrentRequest;

  private String languages;

  private int throttling;

  /**
   * Creates empty parameters. Spring fills them from the form.
   */
  public WorkflowTestDataParameters() {
  }

  /**
   * The number of events to send, new workflows and updates together.
   *
   * @return The number
   */
  public int getNoOfEvents() {
    return noOfEvents;
  }

  /**
   * Sets the number of events to send.
   *
   * @param noOfEvents The number
   */
  public void setNoOfEvents(
      int noOfEvents) {
    this.noOfEvents = noOfEvents;
  }

  /**
   * How many of the events are updates of workflows created before, in percent.
   *
   * @return The percentage
   */
  public int getPercentageUpdates() {
    return percentageUpdates;
  }

  /**
   * Sets how many of the events are updates.
   *
   * @param percentageUpdates The percentage
   */
  public void setPercentageUpdates(
      int percentageUpdates) {
    this.percentageUpdates = percentageUpdates;
  }

  /**
   * The number of threads which send events at the same time.
   *
   * @return The number
   */
  public int getNoOfConcurrentRequest() {
    return noOfConcurrentRequest;
  }

  /**
   * Sets the number of threads which send events.
   *
   * @param noOfConcurrentRequest The number
   */
  public void setNoOfConcurrentRequest(
      int noOfConcurrentRequest) {
    this.noOfConcurrentRequest = noOfConcurrentRequest;
  }

  /**
   * The languages of the workflow titles, separated by commas, e.g. {@code en,de}.
   *
   * @return The languages
   */
  public String getLanguages() {
    return languages;
  }

  /**
   * Sets the languages of the workflow titles.
   *
   * @param languages The languages
   */
  public void setLanguages(
      String languages) {
    this.languages = languages;
  }

  /**
   * The pause after each event, in milliseconds.
   *
   * @return The pause
   */
  public int getThrottling() {
    return throttling;
  }

  /**
   * Sets the pause after each event.
   *
   * @param throttling The pause
   */
  public void setThrottling(
      int throttling) {
    this.throttling = throttling;
  }
}
