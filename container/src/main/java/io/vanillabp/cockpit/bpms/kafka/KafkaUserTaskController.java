package io.vanillabp.cockpit.bpms.kafka;


import com.google.protobuf.InvalidProtocolBufferException;
import io.vanillabp.cockpit.bpms.BpmsApiProperties;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.BcEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskCancelledEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskCompletedEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.util.protobuf.ProtobufHelper;
import java.time.OffsetDateTime;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import reactor.core.publisher.Mono;

import static io.vanillabp.cockpit.bpms.kafka.KafkaConfiguration.KAFKA_CONSUMER_PREFIX;

public class KafkaUserTaskController {

    private final ProtobufUserTaskMapper protobufUserTaskMapper;

    private final UserTaskService userTaskService;

    private static final String CLIENT_ID = "user-task-client";

    public KafkaUserTaskController(UserTaskService userTaskService,
                                   ProtobufUserTaskMapper protobufUserTaskMapper) {
        this.protobufUserTaskMapper = protobufUserTaskMapper;
        this.userTaskService = userTaskService;
    }

    @KafkaListener(topics = "${" + BpmsApiProperties.PREFIX + ".kafka.topics.user-task}",
            clientIdPrefix = KAFKA_CONSUMER_PREFIX + "-" + CLIENT_ID + "-${workerId:local}",
            groupId = KAFKA_CONSUMER_PREFIX + "-${" + BpmsApiProperties.PREFIX + ".kafka.group-id-suffix}")
    public void consumeUserTaskEvent(ConsumerRecord<String, byte[]> record) {
        try {
            final var event = BcEvent.parseFrom(record.value());

            if (event.hasUserTaskCreatedOrUpdated()) {

                UserTaskCreatedOrUpdatedEvent userTaskCreatedOrUpdated =
                        event.getUserTaskCreatedOrUpdated();

                if(userTaskCreatedOrUpdated.getUpdated()) {
                    handleUserTaskUpdateEvent(userTaskCreatedOrUpdated);
                } else {
                    handleUserTaskCreated(userTaskCreatedOrUpdated);
                }

            } else if (event.hasUserTaskCompleted()) {

                handleUserTaskCompletedEvent(
                        event.getUserTaskCompleted());

            } else if (event.hasUserTaskCancelled()) {

                handleUserTaskCancelledEvent(
                        event.getUserTaskCancelled());

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

    private Mono<Boolean> userTaskCreatedMono(UserTaskCreatedOrUpdatedEvent userTaskCreatedOrUpdated) {
        return Mono.just(userTaskCreatedOrUpdated)
                .map(protobufUserTaskMapper::toNewTask)
                .flatMap(userTaskService::createUserTask);
    }
    private void handleUserTaskCreated(UserTaskCreatedOrUpdatedEvent userTaskCreatedOrUpdated) {
        userTaskCreatedMono(userTaskCreatedOrUpdated)
                .subscribe();
    }

    private void handleUserTaskUpdateEvent(UserTaskCreatedOrUpdatedEvent userTaskCreatedOrUpdated) {
        userTaskService
                .getUserTask(userTaskCreatedOrUpdated.getUserTaskId())
                .zipWith(Mono.just(userTaskCreatedOrUpdated))
                .map(t -> protobufUserTaskMapper.toUpdatedTask(t.getT2(), t.getT1()))
                .flatMap(userTaskService::updateUserTask)
                .switchIfEmpty(userTaskCreatedMono(userTaskCreatedOrUpdated))
                .subscribe();
    }

    private void handleUserTaskCompletedEvent(UserTaskCompletedEvent userTaskCompleted) {
        userTaskService
                .getUserTask(userTaskCompleted.getUserTaskId())
                .zipWith(Mono.just(userTaskCompleted))
                .flatMap(t -> {
                    final var task = t.getT1();
                    final var completedEvent = t.getT2();

                    OffsetDateTime timestamp = ProtobufHelper.map(completedEvent.getTimestamp());
                    task.setEndedAt(timestamp);

                    return userTaskService.completeUserTask(
                            task,
                            timestamp);
                })
                .subscribe();
    }

    private void handleUserTaskCancelledEvent(UserTaskCancelledEvent userTaskCancelledEvent) {
        userTaskService
                .getUserTask(userTaskCancelledEvent.getUserTaskId())
                .zipWith(Mono.just(userTaskCancelledEvent))
                .flatMap(t -> {
                    final var task = t.getT1();
                    final var completedEvent = t.getT2();

                    OffsetDateTime timestamp = ProtobufHelper.map(completedEvent.getTimestamp());
                    task.setEndedAt(timestamp);

                    return userTaskService.cancelUserTask(
                            task,
                            timestamp,
                            completedEvent.getComment());
                })
                .subscribe();
    }
}