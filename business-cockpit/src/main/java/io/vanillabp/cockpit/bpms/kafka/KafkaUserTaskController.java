package io.vanillabp.cockpit.bpms.kafka;


import com.google.protobuf.InvalidProtocolBufferException;
import io.vanillabp.cockpit.bpms.BpmsApiProperties;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.BcEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskCancelledEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskCompletedEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.model.UserTaskEndReason;
import io.vanillabp.cockpit.util.protobuf.ProtobufHelper;
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
        userTaskService.reportCreatedUserTask(
                userTaskCreatedOrUpdated.getUserTaskId(),
                ProtobufHelper.map(userTaskCreatedOrUpdated.getTimestamp()),
                () -> protobufUserTaskMapper.toNewTask(userTaskCreatedOrUpdated));
    }

    private void handleUserTaskCreatedV1_1(UserTaskCreatedOrUpdatedEvent userTaskCreatedOrUpdated) {
        handleUserTaskCreatedV1(userTaskCreatedOrUpdated);
    }

    private void handleUserTaskUpdateEventV1(UserTaskCreatedOrUpdatedEvent userTaskCreatedOrUpdated) {
        userTaskService.reportChangedUserTask(
                userTaskCreatedOrUpdated.getUserTaskId(),
                ProtobufHelper.map(userTaskCreatedOrUpdated.getTimestamp()),
                // an update for a task the cockpit never saw creates it, mirroring the REST API
                () -> protobufUserTaskMapper.toNewTask(userTaskCreatedOrUpdated),
                task -> protobufUserTaskMapper.toUpdatedTask(userTaskCreatedOrUpdated, task));
    }

    private void handleUserTaskUpdateEventV1_1(UserTaskCreatedOrUpdatedEvent userTaskCreatedOrUpdated) {
        handleUserTaskUpdateEventV1(userTaskCreatedOrUpdated);
    }

    /**
     * Version 1 of the API reports an end without the fields a change carries, so a task the cockpit
     * hears of by its end alone is stored with the end and nothing else until the creation, which is
     * still on its way, fills the rest in.
     * <p>
     * There is no mapping of an end here for the same reason. An end of this version says who ended
     * the task and, where it was cancelled, why; both are written by hand below. So an end of this
     * version cannot take the due date, the title or the business data of a stored task away,
     * whatever the reporting side leaves out.
     */
    private void handleUserTaskCompletedEventV1(UserTaskCompletedEvent userTaskCompleted) {
        userTaskService.reportEndedUserTask(
                userTaskCompleted.getUserTaskId(),
                ProtobufHelper.map(userTaskCompleted.getTimestamp()),
                UserTaskEndReason.COMPLETED,
                // who completed the task, as reported by the application (may be null =
                // completed by the process); read by the notification poller
                task -> task.setInitiator(
                        userTaskCompleted.hasInitiator() ? userTaskCompleted.getInitiator() : null));
    }

    private void handleUserTaskCompletedEventV1_1(UserTaskCreatedOrUpdatedEvent userTaskCompleted) {

        userTaskService.reportEndedUserTask(
                userTaskCompleted.getUserTaskId(),
                ProtobufHelper.map(userTaskCompleted.getTimestamp()),
                UserTaskEndReason.COMPLETED,
                task -> protobufUserTaskMapper.toEndedTask(userTaskCompleted, task));

    }

    /** @see #handleUserTaskCompletedEventV1(UserTaskCompletedEvent) */
    private void handleUserTaskCancelledEventV1(UserTaskCancelledEvent userTaskCancelledEvent) {
        userTaskService.reportEndedUserTask(
                userTaskCancelledEvent.getUserTaskId(),
                ProtobufHelper.map(userTaskCancelledEvent.getTimestamp()),
                UserTaskEndReason.CANCELLED,
                task -> {
                    task.setInitiator(
                            userTaskCancelledEvent.hasInitiator() ? userTaskCancelledEvent.getInitiator() : null);
                    task.setComment(userTaskCancelledEvent.getComment());
                });
    }

    private void handleUserTaskCancelledEventV1_1(UserTaskCreatedOrUpdatedEvent userTaskCancelled) {

        userTaskService.reportEndedUserTask(
                userTaskCancelled.getUserTaskId(),
                ProtobufHelper.map(userTaskCancelled.getTimestamp()),
                UserTaskEndReason.CANCELLED,
                task -> protobufUserTaskMapper.toEndedTask(userTaskCancelled, task));

    }

}