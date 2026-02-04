package io.vanillabp.cockpit.commons.kafka;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaPropertiesTest {

    @Test
    void getGroupIdSuffix_returnsSetValue() {
        KafkaProperties properties = new KafkaProperties();
        properties.setGroupIdSuffix("my-suffix");

        assertThat(properties.getGroupIdSuffix()).isEqualTo("my-suffix");
    }

    @Test
    void getTopics_returnsSetValue() {
        KafkaProperties properties = new KafkaProperties();
        KafkaProperties.Topics topics = new KafkaProperties.Topics();
        properties.setTopics(topics);

        assertThat(properties.getTopics()).isSameAs(topics);
    }

    @Test
    void topics_getUserTask_returnsSetValue() {
        KafkaProperties.Topics topics = new KafkaProperties.Topics();
        topics.setUserTask("user-task-topic");

        assertThat(topics.getUserTask()).isEqualTo("user-task-topic");
    }

    @Test
    void topics_getWorkflow_returnsSetValue() {
        KafkaProperties.Topics topics = new KafkaProperties.Topics();
        topics.setWorkflow("workflow-topic");

        assertThat(topics.getWorkflow()).isEqualTo("workflow-topic");
    }

    @Test
    void topics_getWorkflowModule_returnsSetValue() {
        KafkaProperties.Topics topics = new KafkaProperties.Topics();
        topics.setWorkflowModule("workflow-module-topic");

        assertThat(topics.getWorkflowModule()).isEqualTo("workflow-module-topic");
    }

    @Test
    void fullConfiguration_worksCorrectly() {
        KafkaProperties properties = new KafkaProperties();
        properties.setGroupIdSuffix("test-group");

        KafkaProperties.Topics topics = new KafkaProperties.Topics();
        topics.setUserTask("ut-topic");
        topics.setWorkflow("wf-topic");
        topics.setWorkflowModule("wfm-topic");
        properties.setTopics(topics);

        assertThat(properties.getGroupIdSuffix()).isEqualTo("test-group");
        assertThat(properties.getTopics().getUserTask()).isEqualTo("ut-topic");
        assertThat(properties.getTopics().getWorkflow()).isEqualTo("wf-topic");
        assertThat(properties.getTopics().getWorkflowModule()).isEqualTo("wfm-topic");
    }
}
