package io.vanillabp.cockpit.bpms.kafka;


import com.google.protobuf.InvalidProtocolBufferException;
import io.vanillabp.cockpit.bpms.BpmsApiProperties;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.BcEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.GroupHierarchy;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.RegisterWorkflowModuleEvent;
import io.vanillabp.cockpit.workflowmodules.WorkflowModuleService;
import java.util.Collection;
import java.util.stream.Collectors;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;

import static io.vanillabp.cockpit.bpms.kafka.KafkaConfiguration.KAFKA_CONSUMER_PREFIX;

/**
 * Takes the registrations of workflow modules from the Kafka topic {@code workflow-module} which
 * the properties name.
 */
public class KafkaWorkflowModuleController {


    private final WorkflowModuleService workflowModuleService;

    private static final String CLIENT_ID = "wm-client";

    /**
     * Creates the listener.
     *
     * @param workflowModuleService Stores the workflow modules
     */
    public KafkaWorkflowModuleController(
            final WorkflowModuleService workflowModuleService) {
        this.workflowModuleService = workflowModuleService;
    }

    /**
     * Parses one record as a protobuf event and stores the workflow module it registers.
     *
     * @param record The record, with the registration event as value
     */
    @KafkaListener(containerFactory = KafkaConfiguration.LISTENER_CONTAINER_FACTORY,
            topics = "${" + BpmsApiProperties.PREFIX + ".kafka.topics.workflow-module}",
            clientIdPrefix = KAFKA_CONSUMER_PREFIX + "-" + CLIENT_ID + "-${workerId:local}",
            groupId = KAFKA_CONSUMER_PREFIX + "-${" + BpmsApiProperties.PREFIX + ".kafka.group-id-suffix}")
    public void consumeWorkflowModuleEvent(ConsumerRecord<String, byte[]> record) {
        try {
            final var event = BcEvent.parseFrom(record.value());

            if (event.hasRegisterWorkflowModule()) {

                handleRegisterWorkflowModuleEvent(event.getRegisterWorkflowModule());

            } else {
                throw new RuntimeException(
                        "Unsupported event type '"
                                + record.key()
                                + "'!");
            }

        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleRegisterWorkflowModuleEvent(
            final RegisterWorkflowModuleEvent event) {

        final var groupHierarchy = event.getGroupHierarchyCount() > 0
                ? event
                        .getGroupHierarchyList()
                        .stream()
                        .collect(Collectors.toMap(GroupHierarchy::getGroup, hierarchy -> (Collection<String>) hierarchy
                                .getTargetList()
                                .stream()
                                .toList()))
                : null;

        workflowModuleService
                .registerOrUpdateWorkflowModule(
                        event.getWorkflowModuleId(),
                        event.getUri(),
                        event.getTaskProviderApiUriPath(),
                        event.getWorkflowProviderApiUriPath(),
                        event.getAccessibleToGroupsList(),
                        groupHierarchy);

    }

}