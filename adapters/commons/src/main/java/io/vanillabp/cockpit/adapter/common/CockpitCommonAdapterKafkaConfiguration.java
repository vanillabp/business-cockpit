package io.vanillabp.cockpit.adapter.common;


import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.cockpit.adapter.common.usertask.UserTaskPublishing;
import io.vanillabp.cockpit.adapter.common.usertask.kafka.UserTaskKafkaPublishing;
import io.vanillabp.cockpit.adapter.common.usertask.kafka.UserTaskProtobufMapper;
import io.vanillabp.cockpit.adapter.common.workflow.WorkflowPublishing;
import io.vanillabp.cockpit.adapter.common.workflow.kafka.WorkflowKafkaPublishing;
import io.vanillabp.cockpit.adapter.common.workflow.kafka.WorkflowProtobufMapper;
import io.vanillabp.cockpit.adapter.common.workflowmodule.WorkflowModulePublishing;
import io.vanillabp.cockpit.adapter.common.workflowmodule.kafka.WorkflowModuleKafkaPublishing;
import io.vanillabp.cockpit.adapter.common.workflowmodule.kafka.WorkflowModuleProtobufMapper;
import io.vanillabp.spi.cockpit.workflowmodules.WorkflowModuleDetailsProvider;
import io.vanillabp.springboot.adapter.VanillaBpProperties;

import java.util.List;
import java.util.Map;
import java.util.TimeZone;
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
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@AutoConfiguration
@AutoConfigureBefore(KafkaAutoConfiguration.class)
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnProperty(
        prefix = VanillaBpProperties.PREFIX + ".cockpit.kafka.topics",
        name = {"user-task", "workflow", "workflow-module"})
public class CockpitCommonAdapterKafkaConfiguration {

    @Value("${workerId}")
    private String workerId;

    @Autowired
    private KafkaProperties kafkaProperties;

    @Autowired
    private VanillaBpCockpitProperties properties;

    @Bean
    public UserTaskPublishing userTaskKafkaPublishing(
            @Qualifier("businessCockpitKafkaTemplate") KafkaTemplate<String, byte[]> kafkaTemplate) {

        return new UserTaskKafkaPublishing(
                workerId,
                properties,
                new UserTaskProtobufMapper(businessCockpitProtobufObjectMapper()),
                kafkaTemplate
        );
    }

    @Bean
    public WorkflowPublishing workflowKafkaPublishing(
            @Qualifier("businessCockpitKafkaTemplate") KafkaTemplate<String, byte[]> kafkaTemplate) {

        return new WorkflowKafkaPublishing(
                workerId,
                properties,
                new WorkflowProtobufMapper(businessCockpitProtobufObjectMapper()),
                kafkaTemplate
        );
    }

    @Bean
    public WorkflowModulePublishing workflowModuleKafkaPublishing(
            @Qualifier("businessCockpitKafkaTemplate") KafkaTemplate<String, byte[]> kafkaTemplate,
            ObjectProvider<List<WorkflowModuleDetailsProvider>> workflowModuleDetailsProviders) {

        return new WorkflowModuleKafkaPublishing(
                workerId,
                properties,
                workflowModuleDetailsProviders,
                new WorkflowModuleProtobufMapper(),
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

    /**
     * Mapper for the Kafka messages exchanged between workflow modules and the cockpit.
     * <p>
     * Jackson 3: the ObjectMapper is immutable, so it is assembled through JsonMapper.builder() instead of
     * being mutated after construction. The date related flags moved from SerializationFeature to
     * DateTimeFeature, setSerializationInclusion became changeDefaultPropertyInclusion, and the
     * JavaTimeModule registration is gone because Jackson 3 has those types built in.
     * <p>
     * The produced format must stay byte-identical - these messages are read by cockpits and workflow
     * modules of other versions. CockpitCommonAdapterKafkaObjectMapperTest pins it down.
     */
    public ObjectMapper businessCockpitProtobufObjectMapper() {

        return JsonMapper.builder()
                // Jackson 3 already defaults WRITE_DATES_AS_TIMESTAMPS to off, Jackson 2 defaulted it to on;
                // the explicit disable below is redundant today and kept only against a future default flip.
                // Jackson 3 sorts properties alphabetically by default, Jackson 2 used declaration order.
                // Key order is semantically irrelevant in JSON, but these are messages that other cockpit
                // and workflow-module versions read, so the bytes are kept as they were.
                .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .enable(DateTimeFeature.WRITE_DATES_WITH_CONTEXT_TIME_ZONE)
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .defaultTimeZone(TimeZone.getTimeZone("UTC"))
                .changeDefaultPropertyInclusion(
                        inclusion -> inclusion.withValueInclusion(JsonInclude.Include.NON_NULL))
                .build();

    }

}
