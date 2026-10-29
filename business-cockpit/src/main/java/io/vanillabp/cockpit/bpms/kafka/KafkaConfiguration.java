package io.vanillabp.cockpit.bpms.kafka;

import io.vanillabp.cockpit.bpms.BpmsApiProperties;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.BcEvent;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import io.vanillabp.cockpit.workflowmodules.WorkflowModuleService;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.kafka.autoconfigure.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.util.StringUtils;

/**
 * Sets up the cockpit to take the reports of the BPMS adapters from Kafka, as another way beside
 * REST. It is only there if Kafka and the protobuf classes are on the class path and all three
 * topics are configured.
 */
// registered through the .imports file of this module, which is what makes the conditions below the
// only thing deciding whether the ingestion is there
@AutoConfiguration
@ConditionalOnClass({ DefaultKafkaConsumerFactory.class, BcEvent.class })
@ConditionalOnProperty(
        prefix = BpmsApiProperties.PREFIX + ".kafka.topics",
        name = {"workflow", "user-task", "workflow-module"})
public class KafkaConfiguration {

    private static final String WORKER_ID_NOT_SET = "not-set";
    /**
     * The start of the client id and of the consumer group id of every listener of the cockpit.
     */
    public static final String KAFKA_CONSUMER_PREFIX = "business-cockpit";

    /**
     * The listener container factory the three listeners of the cockpit run in. It is one of its
     * own, so that the error handler of {@link RepeatUntilStored} applies to them and not to any
     * other listener of an application derived from the cockpit.
     */
    public static final String LISTENER_CONTAINER_FACTORY = "businessCockpitKafkaListenerContainerFactory";

    @Value("${workerId:" + WORKER_ID_NOT_SET + "}")
    private String workerId;

    /**
     * Creates the configuration. Spring Boot creates it as an auto-configuration.
     */
    public KafkaConfiguration() {
    }

    /**
     * The consumer factory of the listeners, built from the Kafka settings of Spring Boot. It reads
     * the key as a string and the value as bytes, which the listeners parse as protobuf. The start
     * fails if the worker id or the suffix of the consumer group id is missing.
     *
     * @param kafkaProperties The Kafka settings of Spring Boot
     * @param bpmsApiProperties The settings of the BPMS API, for the suffix of the consumer group id
     * @return The consumer factory
     */
    @Bean
    public DefaultKafkaConsumerFactory<?, ?> kafkaConsumerFactory(
            KafkaProperties kafkaProperties,
            BpmsApiProperties bpmsApiProperties) {

        if (workerId.equals(WORKER_ID_NOT_SET)) {
            throw new RuntimeException(
                    "Property 'workerId' not set (see https://github.com/vanillabp/spring-boot-support#worker-id)! "
                    + "It is required for proper consuming of Kafka events. "
                    + "For local development use start parameter '-DworkerId=local'.");
        }

        if ((bpmsApiProperties.getKafka() == null)
                || !StringUtils.hasText(bpmsApiProperties.getKafka().getGroupIdSuffix())) {
            throw new RuntimeException(
                    "The property '"
                            + BpmsApiProperties.PREFIX
                            + ".kafka.group-id-suffix' is mandatory and has to identity "
                            + "the application. It is recommended to use '${spring.application.name}'! \n"
                            + "Hint: Do not mixup with workerId (see https://github.com/vanillabp/spring-boot-support#worker-id) "
                            + "which needs to be set, too." );
        }

        Map<String, Object> configs = kafkaProperties.buildConsumerProperties();
        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(configs);
    }

    /**
     * Built with the Kafka settings of Spring Boot, like the factory Spring Boot builds itself, and
     * with the error handler of {@link RepeatUntilStored} in place of the one Spring Boot would set.
     *
     * @param configurer Applies the Kafka settings of Spring Boot to the factory
     * @param kafkaConsumerFactory The consumer factory the containers use
     * @return The listener container factory of the cockpit
     */
    @Bean(LISTENER_CONTAINER_FACTORY)
    public ConcurrentKafkaListenerContainerFactory<Object, Object> businessCockpitKafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> kafkaConsumerFactory) {

        final var factory = new ConcurrentKafkaListenerContainerFactory<Object, Object>();
        configurer.configure(factory, kafkaConsumerFactory);
        factory.setCommonErrorHandler(RepeatUntilStored.errorHandler());
        return factory;

    }

    /**
     * The listener for reports about user tasks.
     *
     * @param userTaskService Stores the reports
     * @param protobufUserTaskMapper Maps the reports onto user tasks
     * @return The listener
     */
    @Bean
    public KafkaUserTaskController kafkaUserTaskController(
            UserTaskService userTaskService,
            ProtobufUserTaskMapper protobufUserTaskMapper) {

        return new KafkaUserTaskController(userTaskService, protobufUserTaskMapper);
    }

    /**
     * The listener for reports about workflows.
     *
     * @param workflowlistService Stores the reports
     * @param workflowMapper Maps the reports onto workflows
     * @return The listener
     */
    @Bean
    public KafkaWorkflowController kafkaWorkflowController(
            WorkflowlistService workflowlistService,
            ProtobufWorkflowMapper workflowMapper) {

        return new KafkaWorkflowController(workflowlistService, workflowMapper);
    }

    /**
     * The listener for workflow modules which register themselves.
     *
     * @param workflowModuleService Stores the workflow modules
     * @return The listener
     */
    @Bean
    public KafkaWorkflowModuleController kafkaWorkflowModuleController(
            WorkflowModuleService workflowModuleService) {

        return new KafkaWorkflowModuleController(workflowModuleService);
    }

    /**
     * The mapper from protobuf user task reports onto user tasks.
     *
     * @return The mapper
     */
    @Bean
    public ProtobufUserTaskMapper protobufUserTaskMapper(){
        return Mappers.getMapper(ProtobufUserTaskMapper.class);
    }

    /**
     * The mapper from protobuf workflow reports onto workflows.
     *
     * @return The mapper
     */
    @Bean
    public ProtobufWorkflowMapper protobufWorkflowMapper(){
        return Mappers.getMapper(ProtobufWorkflowMapper.class);
    }
}
