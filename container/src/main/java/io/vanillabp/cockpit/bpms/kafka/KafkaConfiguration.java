package io.vanillabp.cockpit.bpms.kafka;

import io.vanillabp.cockpit.bpms.BpmsApiProperties;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import io.vanillabp.cockpit.workflowmodules.WorkflowModuleService;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.mapstruct.factory.Mappers;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.util.StringUtils;

@AutoConfiguration
@ConditionalOnProperty(
        prefix = BpmsApiProperties.PREFIX + ".kafka.topics",
        name = {"workflow", "user-task", "workflow-module"})
public class KafkaConfiguration {

    public static final String KAFKA_CONSUMER_PREFIX = "business-cockpit";

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
