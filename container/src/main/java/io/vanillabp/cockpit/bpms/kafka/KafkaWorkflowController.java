package io.vanillabp.cockpit.bpms.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.BcEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCancelledEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCompletedEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.util.protobuf.ProtobufHelper;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

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

    @KafkaListener(topics = "${bpms-api.kafka-topics.workflow}",
            clientIdPrefix = KAFKA_CONSUMER_PREFIX + "-" + CLIENT_ID + "-${workerId:local}",
            groupId = KAFKA_CONSUMER_PREFIX)
    public void consumeWorkflowEvent(ConsumerRecord<String, byte[]> record) {
        try {
            final var event = BcEvent.parseFrom(record.value());

            if (event.hasWorkflowCreatedOrUpdated()) {

                WorkflowCreatedOrUpdatedEvent workflowCreatedOrUpdatedEvent =
                        event.getWorkflowCreatedOrUpdated();

                if (workflowCreatedOrUpdatedEvent.getUpdated()){
                    this.handleWorkflowUpdatedEvent(workflowCreatedOrUpdatedEvent);
                }else{
                    this.handleWorkflowCreatedEvent(workflowCreatedOrUpdatedEvent);
                }

            } else if (event.hasWorkflowCompleted()) {

                this.handleWorkflowCompletedEvent(
                        event.getWorkflowCompleted());

            } else if (event.hasWorkflowCancelled()) {

                this.handleWorkflowCancelledEvent(
                        event.getWorkflowCancelled());

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

    private void handleWorkflowCreatedEvent(WorkflowCreatedOrUpdatedEvent workflowCreatedOrUpdatedEvent) {
        workflowCreateMono(workflowCreatedOrUpdatedEvent)
                .subscribe();
    }


    private void handleWorkflowUpdatedEvent(WorkflowCreatedOrUpdatedEvent workflowCreatedOrUpdatedEvent) {
        workflowlistService
                .getWorkflow(workflowCreatedOrUpdatedEvent.getWorkflowId())
                .map(workflow -> workflowMapper.toUpdatedWorkflow(workflowCreatedOrUpdatedEvent, workflow))
                .flatMap(workflowlistService::updateWorkflow)
                .switchIfEmpty(workflowCreateMono(workflowCreatedOrUpdatedEvent))
                .subscribe();
    }


    private void handleWorkflowCompletedEvent(WorkflowCompletedEvent workflowCompletedEvent){
        workflowlistService
                .getWorkflow(workflowCompletedEvent.getWorkflowId())
                .zipWith(Mono.just(workflowCompletedEvent))
                .flatMap(t -> {
                    final var task = t.getT1();
                    final var completedEvent = t.getT2();

                    OffsetDateTime timestamp = ProtobufHelper.map(completedEvent.getTimestamp());
                    task.setEndedAt(timestamp);

                    return workflowlistService.completeWorkflow(
                            task,
                            timestamp);
                })
                .subscribe();
    }


    private void handleWorkflowCancelledEvent(WorkflowCancelledEvent workflowCancelledEvent){
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
}
