package io.vanillabp.cockpit.adapter.camunda8.receiver.kafka;

import io.camunda.zeebe.protocol.record.Record;
import io.vanillabp.cockpit.adapter.camunda8.usertask.Camunda8UserTaskEventHandler;
import io.vanillabp.cockpit.adapter.camunda8.workflow.Camunda8WorkflowEventHandler;
import io.zeebe.exporters.kafka.serde.RecordDeserializer;
import io.zeebe.exporters.kafka.serde.RecordId;
import io.zeebe.exporters.kafka.serde.RecordIdDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;


@AutoConfiguration
@ConditionalOnProperty(KafkaConfiguration.ZEEBE_KAFKA_EXPORTER_TOPIC_PROPERTY)
@ConditionalOnClass(KafkaTemplate.class)
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaConfiguration {

    public static final String ZEEBE_KAFKA_EXPORTER_TOPIC_PROPERTY = "zeebe.kafka-exporter.topic-name";

    public static final String KAFKA_CONSUMER_PREFIX = "business-cockpit-adapter";

    @Bean
    public DefaultKafkaConsumerFactory<RecordId, Record<?>> zeebeKafkaConsumerFactory(KafkaProperties properties) {
        Map<String, Object> configs = properties.buildConsumerProperties();
        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, RecordIdDeserializer.class);
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, RecordDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(configs);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<RecordId, Record<?>> zeebeKafkaListenerContainerFactory(
            @Qualifier("zeebeKafkaConsumerFactory")
            DefaultKafkaConsumerFactory<RecordId, Record<?>> defaultKafkaConsumerFactory) {

        ConcurrentKafkaListenerContainerFactory<RecordId, Record<?>> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(defaultKafkaConsumerFactory);
        return factory;
    }

    @Bean
    public KafkaController kafkaController(
            Camunda8UserTaskEventHandler camunda8UserTaskEventHandler,
            Camunda8WorkflowEventHandler camunda8WorkflowEventHandler) {
        return new KafkaController(camunda8UserTaskEventHandler, camunda8WorkflowEventHandler);
    }
}
