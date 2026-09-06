package io.vanillabp.cockpit.bpms.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import io.vanillabp.cockpit.bpms.BpmsApiProperties;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.BcEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCancelledEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCompletedEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.util.protobuf.ProtobufHelper;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import java.time.OffsetDateTime;
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
        workflowlistService.createWorkflow(
                workflowMapper.toNewWorkflow(workflowCreatedOrUpdatedEvent));
    }

    private void handleWorkflowCreatedEventV1_1(WorkflowCreatedOrUpdatedEvent workflowCreatedOrUpdatedEvent) {
        handleWorkflowCreatedEventV1(workflowCreatedOrUpdatedEvent);
    }

    private void handleWorkflowUpdatedEventV1(WorkflowCreatedOrUpdatedEvent workflowCreatedOrUpdatedEvent) {
        final var knownWorkflow = workflowlistService.getWorkflow(
                workflowCreatedOrUpdatedEvent.getWorkflowId());
        // an update for a workflow the cockpit never saw creates it, mirroring the REST API
        if (knownWorkflow == null) {
            handleWorkflowCreatedEventV1(workflowCreatedOrUpdatedEvent);
            return;
        }
        workflowlistService.updateWorkflow(
                workflowMapper.toUpdatedWorkflow(workflowCreatedOrUpdatedEvent, knownWorkflow));
    }

    private void handleWorkflowUpdatedEventV1_1(WorkflowCreatedOrUpdatedEvent workflowCreatedOrUpdatedEvent) {
        handleWorkflowUpdatedEventV1(workflowCreatedOrUpdatedEvent);
    }

    private void handleWorkflowCompletedEventV1(WorkflowCompletedEvent workflowCompletedEvent) {
        final var workflow = workflowlistService.getWorkflow(workflowCompletedEvent.getWorkflowId());
        if (workflow == null) {
            return;
        }

        final OffsetDateTime timestamp = ProtobufHelper.map(workflowCompletedEvent.getTimestamp());
        workflow.setEndedAt(timestamp);

        workflowlistService.completeWorkflow(workflow, timestamp);
    }

    private void handleWorkflowCompletedEventV1_1(WorkflowCreatedOrUpdatedEvent workflowCompletedEvent) {

        final var knownWorkflow = workflowlistService.getWorkflow(workflowCompletedEvent.getWorkflowId());
        if (knownWorkflow == null) {
            return;
        }
        final var workflow = workflowMapper.toUpdatedWorkflow(workflowCompletedEvent, knownWorkflow);
        workflowlistService.completeWorkflow(workflow, workflow.getUpdatedAt());

    }

    private void handleWorkflowCancelledEventV1(WorkflowCancelledEvent workflowCancelledEvent) {
        final var workflow = workflowlistService.getWorkflow(workflowCancelledEvent.getWorkflowId());
        if (workflow == null) {
            return;
        }

        final OffsetDateTime timestamp = ProtobufHelper.map(workflowCancelledEvent.getTimestamp());
        workflow.setEndedAt(timestamp);

        workflowlistService.cancelWorkflow(workflow, timestamp, workflowCancelledEvent.getComment());
    }

    private void handleWorkflowCancelledEventV1_1(WorkflowCreatedOrUpdatedEvent workflowCancelledEvent) {

        final var knownWorkflow = workflowlistService.getWorkflow(workflowCancelledEvent.getWorkflowId());
        if (knownWorkflow == null) {
            return;
        }
        final var workflow = workflowMapper.toUpdatedWorkflow(workflowCancelledEvent, knownWorkflow);
        workflowlistService.cancelWorkflow(workflow, workflow.getUpdatedAt(), workflow.getComment());

    }

}