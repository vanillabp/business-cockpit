package io.vanillabp.cockpit.commons.kafka;

public class KafkaProperties {

    private String groupIdSuffix;

    private Topics topics;

    public String getGroupIdSuffix() {
        return groupIdSuffix;
    }

    public void setGroupIdSuffix(String groupIdSuffix) {
        this.groupIdSuffix = groupIdSuffix;
    }

    public Topics getTopics() {
        return topics;
    }

    public void setTopics(Topics topics) {
        this.topics = topics;
    }

    public static class Topics {

        private String userTask;
        private String workflow;
        private String workflowModule;

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

        public String getWorkflowModule() {
            return workflowModule;
        }

        public void setWorkflowModule(String workflowModule) {
            this.workflowModule = workflowModule;
        }

    }

}
