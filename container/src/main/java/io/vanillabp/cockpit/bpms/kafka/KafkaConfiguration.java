package io.vanillabp.cockpit.bpms.kafka;

import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.mapstruct.factory.Mappers;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.Map;

@AutoConfiguration
@Profile("kafka")
public class KafkaConfiguration {

    public static final String KAFKA_CONSUMER_PREFIX = "business-cockpit";

    @Bean
    public DefaultKafkaConsumerFactory<?, ?> kafkaConsumerFactory(
            KafkaProperties properties) {

        Map<String, Object> configs = properties.buildConsumerProperties();
        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(configs);
    }

    @Bean
    public KafkaUserTaskController kafkaUserTaskController(
            UserTaskService userTaskService,
            ProtobufUserTaskMapper protobufUserTaskMapper) {

        return new KafkaUserTaskController(userTaskService, protobufUserTaskMapper);
    }

    @Bean
    public KafkaWorkflowController kafkaWorkflowController(
            WorkflowlistService workflowlistService,
            ProtobufWorkflowMapper workflowMapper) {

        return new KafkaWorkflowController(workflowlistService, workflowMapper);
    }

    @Bean
    public ProtobufUserTaskMapper protobufUserTaskMapper(){
        return Mappers.getMapper(ProtobufUserTaskMapper.class);
    }

    @Bean
    public ProtobufWorkflowMapper protobufWorkflowMapper(){
        return Mappers.getMapper(ProtobufWorkflowMapper.class);
    }
}
