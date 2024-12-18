package io.vanillabp.cockpit.bpms.kafka;

import io.vanillabp.cockpit.bpms.BpmsApiProperties;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import io.vanillabp.cockpit.workflowmodules.WorkflowModuleService;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.mapstruct.factory.Mappers;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.util.StringUtils;

@AutoConfiguration
@ConditionalOnProperty(
        prefix = BpmsApiProperties.PREFIX + ".kafka.topics",
        name = {"workflow", "user-task", "workflow-module"})
public class KafkaConfiguration {


    public static final String WORKER_ID_ENV_NAME = "WORKER_ID";

    public static final String WORKER_ID_PROPERTY_NAME = "workerId";

    public static final String KAFKA_CONSUMER_PREFIX = "business-cockpit";

    /**
     * Build property source for injection of the worker-id value in Spring beans.
     */
    @ConditionalOnMissingBean(name = "kafkaWorkerIdProperties")
    private static PropertiesPropertySource workerIdProperties(
            final Collection<String> activeProfiles) {

        final var workerId = getWorkerId(activeProfiles);
        final var props = new Properties();
        props.setProperty(WORKER_ID_PROPERTY_NAME, workerId);
        return new PropertiesPropertySource("workerIdProps", props);

    }

    /**
     * Fetch the worker-id from the environment. In Kubernetes environments this is
     * typically the pod's name. In case of local development environment and
     * missing environment variable the id 'local' is used.
     */
    private static String getWorkerId(
            final Collection<String> activeProfiles) {

        var workerId = System.getenv(WORKER_ID_ENV_NAME);
        if (workerId == null) {
            workerId = System.getProperty(WORKER_ID_ENV_NAME);
        }
        if (workerId == null) {
            workerId = System.getProperty(WORKER_ID_PROPERTY_NAME);
        }

        if (workerId == null) {
            var isDevelopment = activeProfiles.stream().anyMatch(profile -> profile.matches("local"));
            if (isDevelopment) {
                workerId = "local";
            }
        }

        if (workerId == null) {
            throw new RuntimeException("No environment variable '"
                    + WORKER_ID_ENV_NAME
                    + "' or system property '"
                    + WORKER_ID_PROPERTY_NAME
                    + "' given (see https://github.com/vanillabp/spring-boot-support#worker-id)! "
                    + "It is required for proper consuming of Kafka events.");
        }

        return workerId;

    }

    @Bean
    public DefaultKafkaConsumerFactory<?, ?> kafkaConsumerFactory(
            KafkaProperties kafkaProperties,
            BpmsApiProperties bpmsApiProperties) {

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
    public KafkaWorkflowModuleController kafkaWorkflowModuleController(
            WorkflowModuleService workflowModuleService) {

        return new KafkaWorkflowModuleController(workflowModuleService);
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
