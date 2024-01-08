package io.vanillabp.cockpit.bpms.kafka;

import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaConsumerFactoryCustomizer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

@AutoConfiguration
@Profile("kafka")
@ConditionalOnClass(KafkaTemplate.class)
public class KafkaConfiguration {

    public static final String KAFKA_CONSUMER_PREFIX = "business-cockpit";

    @Bean
    public DefaultKafkaConsumerFactory<?, ?> kafkaConsumerFactory(
            KafkaProperties properties,
            ObjectProvider<DefaultKafkaConsumerFactoryCustomizer> customizers) {

        Map<String, Object> configs = properties.buildConsumerProperties();
        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);

        DefaultKafkaConsumerFactory<Object, Object> factory = new DefaultKafkaConsumerFactory<>(configs);
        customizers.orderedStream().forEach((customizer) -> customizer.customize(factory));
        return factory;
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
