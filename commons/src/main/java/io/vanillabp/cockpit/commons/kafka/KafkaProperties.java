package io.vanillabp.cockpit.commons.kafka;

/**
 * The settings for reports which reach the cockpit by Kafka: the topics it reads and the suffix
 * of its consumer group id.
 */
public class KafkaProperties {

  private String groupIdSuffix;

  private Topics topics;

  /**
   * Creates empty settings.
   */
  public KafkaProperties() {
  }

  /**
   * The text added to the consumer group id. It has to name the application, so each
   * application which reads the topics gets all records. If Kafka is used, the cockpit does not
   * start without it.
   *
   * @return The suffix
   */
  public String getGroupIdSuffix() {
    return groupIdSuffix;
  }

  /**
   * Sets the text added to the consumer group id. It has to name the application.
   *
   * @param groupIdSuffix The suffix
   */
  public void setGroupIdSuffix(
      String groupIdSuffix) {
    this.groupIdSuffix = groupIdSuffix;
  }

  /**
   * The names of the topics.
   *
   * @return The topic names
   */
  public Topics getTopics() {
    return topics;
  }

  /**
   * Sets the names of the topics.
   *
   * @param topics The topic names
   */
  public void setTopics(
      Topics topics) {
    this.topics = topics;
  }

  /**
   * The names of the Kafka topics, one for each kind of event.
   */
  public static class Topics {

    private String userTask;
    private String workflow;
    private String workflowModule;

    /**
     * Creates empty settings, without topic names.
     */
    public Topics() {
    }

    /**
     * The topic of the user task events.
     *
     * @return The topic name
     */
    public String getUserTask() {
      return userTask;
    }

    /**
     * Sets the topic of the user task events.
     *
     * @param userTask The topic name
     */
    public void setUserTask(
        String userTask) {
      this.userTask = userTask;
    }

    /**
     * The topic of the workflow events.
     *
     * @return The topic name
     */
    public String getWorkflow() {
      return workflow;
    }

    /**
     * Sets the topic of the workflow events.
     *
     * @param workflow The topic name
     */
    public void setWorkflow(
        String workflow) {
      this.workflow = workflow;
    }

    /**
     * The topic of the workflow module events.
     *
     * @return The topic name
     */
    public String getWorkflowModule() {
      return workflowModule;
    }

    /**
     * Sets the topic of the workflow module events.
     *
     * @param workflowModule The topic name
     */
    public void setWorkflowModule(
        String workflowModule) {
      this.workflowModule = workflowModule;
    }

  }

}
