package io.vanillabp.cockpit.adapter.common;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vanillabp.cockpit.adapter.common.usertask.UserTaskPublishing;
import io.vanillabp.cockpit.adapter.common.usertask.UserTasksWorkflowProperties;
import io.vanillabp.cockpit.adapter.common.usertask.kafka.UserTaskKafkaPublishing;
import io.vanillabp.cockpit.adapter.common.usertask.kafka.UserTaskProtobufMapper;
import io.vanillabp.cockpit.adapter.common.workflow.WorkflowPublishing;
import io.vanillabp.cockpit.adapter.common.workflow.kafka.WorkflowKafkaPublishing;
import io.vanillabp.cockpit.adapter.common.workflow.kafka.WorkflowProtobufMapper;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.Map;
import java.util.TimeZone;

@AutoConfiguration
@AutoConfigureBefore(KafkaAutoConfiguration.class)
@EnableConfigurationProperties({
        CockpitProperties.class,
        UserTasksWorkflowProperties.class,
        KafkaProperties.class
})
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnProperty(
        prefix = CockpitProperties.PREFIX + ".kafka-topics",
        name = {"workflow", "user-task"})
public class CockpitCommonAdapterKafkaConfiguration {

    @Value("${workerId}")
    private String workerId;

    @Autowired
    private KafkaProperties kafkaProperties;

    @Autowired
    private CockpitProperties properties;

    @Autowired
    private UserTasksWorkflowProperties workflowsCockpitProperties;


    @Bean
    public UserTaskPublishing userTaskKafkaPublishing(
            @Qualifier("businessCockpitKafkaTemplate") KafkaTemplate<String, byte[]> kafkaTemplate,
            @Qualifier("businessCockpitProtobufObjectMapper") ObjectMapper objectMapper) {

        return new UserTaskKafkaPublishing(
                workerId,
                properties,
                workflowsCockpitProperties,
                new UserTaskProtobufMapper(objectMapper),
                kafkaTemplate
        );
    }

    @Bean
    public WorkflowPublishing workflowKafkaPublishing(
            @Qualifier("businessCockpitKafkaTemplate") KafkaTemplate<String, byte[]> kafkaTemplate,
            @Qualifier("businessCockpitProtobufObjectMapper") ObjectMapper objectMapper) {

        return new WorkflowKafkaPublishing(
                workerId,
                properties,
                workflowsCockpitProperties,
                new WorkflowProtobufMapper(objectMapper),
                kafkaTemplate
        );
    }

    @Bean
    public KafkaTemplate<String, byte[]> businessCockpitKafkaTemplate(
            @Qualifier("businessCockpitKafkaProducerFactory") ProducerFactory<String, byte[]> kafkaProducerFactory) {
        return new KafkaTemplate<>(kafkaProducerFactory);
    }

    @Bean
    public DefaultKafkaProducerFactory<String, byte[]> businessCockpitKafkaProducerFactory() {
        Map<String, Object> configs = this.kafkaProperties.buildProducerProperties();

        configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);

        return new DefaultKafkaProducerFactory<>(configs);
    }

    @Bean
    @Qualifier("businessCockpitProtobufObjectMapper")
    public ObjectMapper businessCockpitProtobufObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.enable(SerializationFeature.WRITE_DATES_WITH_CONTEXT_TIME_ZONE);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);

        objectMapper.setTimeZone(TimeZone.getTimeZone("UTC"));
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

}
