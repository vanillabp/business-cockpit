package io.vanillabp.cockpit.bpms.kafka;


import static io.vanillabp.cockpit.bpms.kafka.KafkaConfiguration.KAFKA_CONSUMER_PREFIX;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;

import com.google.protobuf.InvalidProtocolBufferException;

import io.vanillabp.cockpit.bpms.BpmsApiProperties;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.BcEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.GroupHierarchy;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.RegisterWorkflowModuleEvent;
import io.vanillabp.cockpit.exceptions.BcInvalidRequestException;
import io.vanillabp.cockpit.workflowmodules.WhatARegistrationMustSay;
import io.vanillabp.cockpit.workflowmodules.WorkflowModuleService;

/**
 * Takes the registrations of workflow modules from the Kafka topic {@code workflow-module} which
 * the properties name.
 */
public class KafkaWorkflowModuleController {

  private static final Logger logger = LoggerFactory.getLogger(KafkaWorkflowModuleController.class);

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
      topics = "${"
          + BpmsApiProperties.PREFIX
          + ".kafka.topics.workflow-module}",
      clientIdPrefix = KAFKA_CONSUMER_PREFIX
          + "-"
          + CLIENT_ID
          + "-${workerId:local}",
      groupId = KAFKA_CONSUMER_PREFIX
          + "-${"
          + BpmsApiProperties.PREFIX
          + ".kafka.group-id-suffix}")
  public void consumeWorkflowModuleEvent(
      ConsumerRecord<String, byte[]> record) {
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

  /**
   * A registration which breaks the rules of {@link WhatARegistrationMustSay} is dropped and
   * logged. Kafka has no answer to tell the sender, and the same record would be refused again.
   */
  private void handleRegisterWorkflowModuleEvent(
      final RegisterWorkflowModuleEvent event) {

    try {
      final var groupHierarchy = event.getGroupHierarchyCount() > 0
          ? WhatARegistrationMustSay.groupHierarchyOf(
              event.getGroupHierarchyList(),
              GroupHierarchy::getGroup,
              GroupHierarchy::getTargetList)
          : null;

      workflowModuleService
          .registerOrUpdateWorkflowModule(
              event.getWorkflowModuleId(),
              event.getUri(),
              event.getTaskProviderApiUriPath(),
              event.getWorkflowProviderApiUriPath(),
              event.getAccessibleToGroupsList(),
              groupHierarchy);
    } catch (BcInvalidRequestException e) {
      logger.error(
          "Dropping the registration of the workflow module '{}' received by Kafka: {}. "
              + "Correct the configuration of the module and start it again.",
          event.getWorkflowModuleId(),
          e.getMessage());
    }

  }

}