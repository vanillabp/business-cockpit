package io.vanillabp.cockpit.adapter.common;


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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.converter.RecordMessageConverter;

import java.util.Map;

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
            @Qualifier("businessCockpitKafkaTemplate") KafkaTemplate<String, byte[]> kafkaTemplate) {

        return new UserTaskKafkaPublishing(
                workerId,
                properties,
                workflowsCockpitProperties,
                new UserTaskProtobufMapper(),
                kafkaTemplate
        );
    }

    @Bean
    public WorkflowPublishing workflowKafkaPublishing(
            @Qualifier("businessCockpitKafkaTemplate") KafkaTemplate<String, byte[]> kafkaTemplate) {

        return new WorkflowKafkaPublishing(
                workerId,
                properties,
                workflowsCockpitProperties,
                new WorkflowProtobufMapper(),
                kafkaTemplate
        );
    }

    @Bean
    public KafkaTemplate<String, byte[]> businessCockpitKafkaTemplate(
            @Qualifier("businessCockpitKafkaProducerFactory") ProducerFactory<String, byte[]> kafkaProducerFactory,
            ObjectProvider<RecordMessageConverter> messageConverter) {

        PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
        KafkaTemplate<String, byte[]> kafkaTemplate = new KafkaTemplate<>(kafkaProducerFactory);
        messageConverter.ifUnique(kafkaTemplate::setMessageConverter);

        map.from(this.kafkaProperties.getTemplate().getDefaultTopic()).to(kafkaTemplate::setDefaultTopic);
        map.from(this.kafkaProperties.getTemplate().getTransactionIdPrefix()).to(kafkaTemplate::setTransactionIdPrefix);
        return kafkaTemplate;
    }

    @Bean
    public DefaultKafkaProducerFactory<String, byte[]> businessCockpitKafkaProducerFactory(
            ObjectProvider<DefaultKafkaProducerFactoryCustomizer> customizers) {
        Map<String, Object> configs = this.kafkaProperties.buildProducerProperties();

        configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);

        DefaultKafkaProducerFactory<String, byte[]> factory = new DefaultKafkaProducerFactory<>(
                configs);
        String transactionIdPrefix = this.kafkaProperties.getProducer().getTransactionIdPrefix();
        if (transactionIdPrefix != null) {
            factory.setTransactionIdPrefix(transactionIdPrefix);
        }
        customizers.orderedStream().forEach((customizer) -> customizer.customize(factory));
        return factory;
    }

}
