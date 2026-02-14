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
import reactor.core.publisher.Mono;

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

    private Mono<Boolean> workflowCreateMono(
            final WorkflowCreatedOrUpdatedEvent workflowCreatedOrUpdatedEvent) {

        return Mono.just(workflowCreatedOrUpdatedEvent)
                .map(workflowMapper::toNewWorkflow)
                .flatMap(workflowlistService::createWorkflow);

    }

    private void handleWorkflowCreatedEventV1(WorkflowCreatedOrUpdatedEvent workflowCreatedOrUpdatedEvent) {
        workflowCreateMono(workflowCreatedOrUpdatedEvent)
                .subscribe();
    }

    private void handleWorkflowCreatedEventV1_1(WorkflowCreatedOrUpdatedEvent workflowCreatedOrUpdatedEvent) {
        handleWorkflowCreatedEventV1(workflowCreatedOrUpdatedEvent);
    }

    private void handleWorkflowUpdatedEventV1(WorkflowCreatedOrUpdatedEvent workflowCreatedOrUpdatedEvent) {
        workflowlistService
                .getWorkflow(workflowCreatedOrUpdatedEvent.getWorkflowId())
                .map(workflow -> workflowMapper.toUpdatedWorkflow(workflowCreatedOrUpdatedEvent, workflow))
                .flatMap(workflowlistService::updateWorkflow)
                .switchIfEmpty(workflowCreateMono(workflowCreatedOrUpdatedEvent))
                .subscribe();
    }

    private void handleWorkflowUpdatedEventV1_1(WorkflowCreatedOrUpdatedEvent workflowCreatedOrUpdatedEvent) {
        handleWorkflowUpdatedEventV1(workflowCreatedOrUpdatedEvent);
    }

    private void handleWorkflowCompletedEventV1(WorkflowCompletedEvent workflowCompletedEvent) {
        workflowlistService
                .getWorkflow(workflowCompletedEvent.getWorkflowId())
                .zipWith(Mono.just(workflowCompletedEvent))
                .flatMap(t -> {
                    final var workflow = t.getT1();
                    final var completedEvent = t.getT2();

                    OffsetDateTime timestamp = ProtobufHelper.map(completedEvent.getTimestamp());
                    workflow.setEndedAt(timestamp);

                    return workflowlistService.completeWorkflow(
                            workflow,
                            timestamp);
                })
                .subscribe();
    }

    private void handleWorkflowCompletedEventV1_1(WorkflowCreatedOrUpdatedEvent workflowCompletedEvent) {

        workflowlistService
                .getWorkflow(workflowCompletedEvent.getWorkflowId())
                .map(workflow -> workflowMapper.toUpdatedWorkflow(workflowCompletedEvent, workflow))
                .flatMap(workflow -> workflowlistService.completeWorkflow(
                        workflow,
                        workflow.getUpdatedAt()))
                .subscribe();

    }

    private void handleWorkflowCancelledEventV1(WorkflowCancelledEvent workflowCancelledEvent) {
        workflowlistService
                .getWorkflow(workflowCancelledEvent.getWorkflowId())
                .zipWith(Mono.just(workflowCancelledEvent))
                .flatMap(t -> {
                    final var workflow = t.getT1();
                    final var completedEvent = t.getT2();

                    OffsetDateTime timestamp = ProtobufHelper.map(completedEvent.getTimestamp());
                    workflow.setEndedAt(timestamp);

                    return workflowlistService.cancelWorkflow(
                            workflow,
                            timestamp,
                            completedEvent.getComment());
                })
                .subscribe();
    }

    private void handleWorkflowCancelledEventV1_1(WorkflowCreatedOrUpdatedEvent workflowCancelledEvent) {

        workflowlistService
                .getWorkflow(workflowCancelledEvent.getWorkflowId())
                .map(workflow -> workflowMapper.toUpdatedWorkflow(workflowCancelledEvent, workflow))
                .flatMap(workflow -> workflowlistService.cancelWorkflow(
                        workflow,
                        workflow.getUpdatedAt(),
                        workflow.getComment()))
                .subscribe();

    }

}