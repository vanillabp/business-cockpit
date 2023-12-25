package io.vanillabp.cockpit.commons.kafka;

public class KafkaTopicProperties {
    private String userTask;
    private String workflow;

    public String getUserTask() {
        return userTask;
    }

    public void setUserTask(String userTask) {
        this.userTask = userTask;
    }

    public String getWorkflow() {
        return workflow;
    }

    public void setWorkflow(String workflow) {
        this.workflow = workflow;
    }
}
