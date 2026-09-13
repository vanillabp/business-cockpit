package io.vanillabp.cockpit.bpms.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import io.vanillabp.cockpit.bpms.BpmsApiProperties;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.BcEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCancelledEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCompletedEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.util.protobuf.ProtobufHelper;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;

import static io.vanillabp.cockpit.bpms.kafka.KafkaConfiguration.KAFKA_CONSUMER_PREFIX;

public class KafkaWorkflowController {

    private final ProtobufWorkflowMapper workflowMapper;

    private final WorkflowlistService workflowlistService;

    private static final String CLIENT_ID = "workflow-client";

    public KafkaWorkflowController(WorkflowlistService workflowlistService,
                                   ProtobufWorkflowMapper workflowMapper) {
        this.workflowlistService = workflowlistService;
        this.workflowMapper = workflowMapper;
    }

    @KafkaListener(topics = "${" + BpmsApiProperties.PREFIX + ".kafka.topics.workflow}",
            clientIdPrefix = KAFKA_CONSUMER_PREFIX + "-" + CLIENT_ID + "-${workerId:local}",
            groupId = KAFKA_CONSUMER_PREFIX + "-${" + BpmsApiProperties.PREFIX + ".kafka.group-id-suffix}")
    public void consumeWorkflowEvent(ConsumerRecord<String, byte[]> record) {
        try {
            final var event = BcEvent.parseFrom(record.value());

            if (event.hasWorkflowCreatedOrUpdated()) {

                WorkflowCreatedOrUpdatedEvent workflowCreatedOrUpdatedEvent =
                        event.getWorkflowCreatedOrUpdated();

                if (workflowCreatedOrUpdatedEvent.getUpdated()) {
                    this.handleWorkflowUpdatedEventV1(workflowCreatedOrUpdatedEvent);
                } else {
                    this.handleWorkflowCreatedEventV1(workflowCreatedOrUpdatedEvent);
                }

            } else if (event.hasWorkflowCompleted()) {

                this.handleWorkflowCompletedEventV1(
                        event.getWorkflowCompleted());

            } else if (event.hasWorkflowCancelled()) {

                this.handleWorkflowCancelledEventV1(
                        event.getWorkflowCancelled());

            } else if (event.hasWorkflowCreatedV11()) {

                this.handleWorkflowCreatedEventV1_1(event.getWorkflowCreatedV11());

            } else if (event.hasWorkflowUpdatedV11()) {

                this.handleWorkflowUpdatedEventV1_1(event.getWorkflowUpdatedV11());

            } else if (event.hasWorkflowCompletedV11()) {

                this.handleWorkflowCompletedEventV1_1(event.getWorkflowCompletedV11());

            } else if (event.hasWorkflowCancelledV11()) {

                this.handleWorkflowCancelledEventV1_1(event.getWorkflowCancelledV11());

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

    private void handleWorkflowCreatedEventV1(WorkflowCreatedOrUpdatedEvent workflowCreatedOrUpdatedEvent) {
        workflowlistService.reportCreatedWorkflow(
                workflowCreatedOrUpdatedEvent.getWorkflowId(),
                ProtobufHelper.map(workflowCreatedOrUpdatedEvent.getTimestamp()),
                () -> workflowMapper.toNewWorkflow(workflowCreatedOrUpdatedEvent));
    }

    private void handleWorkflowCreatedEventV1_1(WorkflowCreatedOrUpdatedEvent workflowCreatedOrUpdatedEvent) {
        handleWorkflowCreatedEventV1(workflowCreatedOrUpdatedEvent);
    }

    private void handleWorkflowUpdatedEventV1(WorkflowCreatedOrUpdatedEvent workflowCreatedOrUpdatedEvent) {
        workflowlistService.reportChangedWorkflow(
                workflowCreatedOrUpdatedEvent.getWorkflowId(),
                ProtobufHelper.map(workflowCreatedOrUpdatedEvent.getTimestamp()),
                // an update for a workflow the cockpit never saw creates it, mirroring the REST API
                () -> workflowMapper.toNewWorkflow(workflowCreatedOrUpdatedEvent),
                workflow -> workflowMapper.toUpdatedWorkflow(workflowCreatedOrUpdatedEvent, workflow));
    }

    private void handleWorkflowUpdatedEventV1_1(WorkflowCreatedOrUpdatedEvent workflowCreatedOrUpdatedEvent) {
        handleWorkflowUpdatedEventV1(workflowCreatedOrUpdatedEvent);
    }

    /**
     * Version 1 of the API reports an end without the fields a change carries, so a case the cockpit
     * hears of by its end alone is stored with the end and nothing else until the creation, which is
     * still on its way, fills the rest in.
     */
    private void handleWorkflowCompletedEventV1(WorkflowCompletedEvent workflowCompletedEvent) {
        workflowlistService.reportEndedWorkflow(
                workflowCompletedEvent.getWorkflowId(),
                ProtobufHelper.map(workflowCompletedEvent.getTimestamp()),
                // version 1 reports nothing about a completed case but that it completed
                workflow -> { });
    }

    private void handleWorkflowCompletedEventV1_1(WorkflowCreatedOrUpdatedEvent workflowCompletedEvent) {

        workflowlistService.reportEndedWorkflow(
                workflowCompletedEvent.getWorkflowId(),
                ProtobufHelper.map(workflowCompletedEvent.getTimestamp()),
                workflow -> workflowMapper.toEndedWorkflow(workflowCompletedEvent, workflow));

    }

    /** @see #handleWorkflowCompletedEventV1(WorkflowCompletedEvent) */
    private void handleWorkflowCancelledEventV1(WorkflowCancelledEvent workflowCancelledEvent) {
        workflowlistService.reportEndedWorkflow(
                workflowCancelledEvent.getWorkflowId(),
                ProtobufHelper.map(workflowCancelledEvent.getTimestamp()),
                workflow -> workflow.setComment(workflowCancelledEvent.getComment()));
    }

    private void handleWorkflowCancelledEventV1_1(WorkflowCreatedOrUpdatedEvent workflowCancelledEvent) {

        workflowlistService.reportEndedWorkflow(
                workflowCancelledEvent.getWorkflowId(),
                ProtobufHelper.map(workflowCancelledEvent.getTimestamp()),
                workflow -> workflowMapper.toEndedWorkflow(workflowCancelledEvent, workflow));

    }

}