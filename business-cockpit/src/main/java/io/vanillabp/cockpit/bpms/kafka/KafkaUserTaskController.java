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
                    handleUserTaskUpdateEventV1(userTaskCreatedOrUpdated);
                } else {
                    handleUserTaskCreatedV1(userTaskCreatedOrUpdated);
                }

            } else if (event.hasUserTaskCompleted()) {

                handleUserTaskCompletedEventV1(
                        event.getUserTaskCompleted());

            } else if (event.hasUserTaskCancelled()) {

                handleUserTaskCancelledEventV1(
                        event.getUserTaskCancelled());

            } else if (event.hasUserTaskCreatedV11()) {

                handleUserTaskCreatedV1_1(event.getUserTaskCreatedV11());

            } else if (event.hasUserTaskUpdatedV11()) {

                handleUserTaskUpdateEventV1_1(event.getUserTaskUpdatedV11());

            } else if (event.hasUserTaskCompletedV11()) {

                handleUserTaskCompletedEventV1_1(event.getUserTaskCompletedV11());

            } else if (event.hasUserTaskCancelledV11()) {

                handleUserTaskCancelledEventV1_1(event.getUserTaskCancelledV11());

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

    private void handleUserTaskCreatedV1(UserTaskCreatedOrUpdatedEvent userTaskCreatedOrUpdated) {
        userTaskService.createUserTask(
                protobufUserTaskMapper.toNewTask(userTaskCreatedOrUpdated));
    }

    private void handleUserTaskCreatedV1_1(UserTaskCreatedOrUpdatedEvent userTaskCreatedOrUpdated) {
        handleUserTaskCreatedV1(userTaskCreatedOrUpdated);
    }

    private void handleUserTaskUpdateEventV1(UserTaskCreatedOrUpdatedEvent userTaskCreatedOrUpdated) {
        final var knownTask = userTaskService.getUserTask(userTaskCreatedOrUpdated.getUserTaskId());
        // an update for a task the cockpit never saw creates it, mirroring the REST API
        if (knownTask == null) {
            handleUserTaskCreatedV1(userTaskCreatedOrUpdated);
            return;
        }
        userTaskService.updateUserTask(
                protobufUserTaskMapper.toUpdatedTask(userTaskCreatedOrUpdated, knownTask));
    }

    private void handleUserTaskUpdateEventV1_1(UserTaskCreatedOrUpdatedEvent userTaskCreatedOrUpdated) {
        handleUserTaskUpdateEventV1(userTaskCreatedOrUpdated);
    }

    private void handleUserTaskCompletedEventV1(UserTaskCompletedEvent userTaskCompleted) {
        final var task = userTaskService.getUserTask(userTaskCompleted.getUserTaskId());
        if (task == null) {
            return;
        }

        final OffsetDateTime timestamp = ProtobufHelper.map(userTaskCompleted.getTimestamp());
        task.setEndedAt(timestamp);
        // who completed the task, as reported by the application (may be null =
        // completed by the process); read by the notification poller
        task.setInitiator(
                userTaskCompleted.hasInitiator() ? userTaskCompleted.getInitiator() : null);

        userTaskService.completeUserTask(task, timestamp);
    }

    private void handleUserTaskCompletedEventV1_1(UserTaskCreatedOrUpdatedEvent userTaskCompleted) {

        final var knownTask = userTaskService.getUserTask(userTaskCompleted.getUserTaskId());
        if (knownTask == null) {
            return;
        }
        final var task = protobufUserTaskMapper.toEndedTask(userTaskCompleted, knownTask);
        userTaskService.completeUserTask(task, task.getUpdatedAt());

    }

    private void handleUserTaskCancelledEventV1(UserTaskCancelledEvent userTaskCancelledEvent) {
        final var task = userTaskService.getUserTask(userTaskCancelledEvent.getUserTaskId());
        if (task == null) {
            return;
        }

        final OffsetDateTime timestamp = ProtobufHelper.map(userTaskCancelledEvent.getTimestamp());
        task.setEndedAt(timestamp);
        task.setInitiator(
                userTaskCancelledEvent.hasInitiator() ? userTaskCancelledEvent.getInitiator() : null);

        userTaskService.cancelUserTask(task, timestamp, userTaskCancelledEvent.getComment());
    }

    private void handleUserTaskCancelledEventV1_1(UserTaskCreatedOrUpdatedEvent userTaskCancelled) {

        final var knownTask = userTaskService.getUserTask(userTaskCancelled.getUserTaskId());
        if (knownTask == null) {
            return;
        }
        final var task = protobufUserTaskMapper.toEndedTask(userTaskCancelled, knownTask);
        userTaskService.cancelUserTask(task, task.getUpdatedAt(), task.getComment());

    }

}