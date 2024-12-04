package io.vanillabp.cockpit.adapter.common.usertask.kafka;

import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.cockpit.adapter.common.usertask.UserTaskPublishing;
import io.vanillabp.cockpit.adapter.common.usertask.UserTaskPublishingBase;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskActivatedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCancelledEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCompletedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCreatedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskSuspendedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.BcEvent;
import java.util.function.Consumer;
import org.springframework.kafka.core.KafkaTemplate;

public class UserTaskKafkaPublishing extends UserTaskPublishingBase implements UserTaskPublishing {

    private final UserTaskProtobufMapper userTaskMapper;

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public UserTaskKafkaPublishing(
            String workerId,
            VanillaBpCockpitProperties properties,
            UserTaskProtobufMapper userTaskMapper,
            KafkaTemplate<String, byte[]> kafkaTemplate
    ) {
        super(workerId, properties);
        this.userTaskMapper = userTaskMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(UserTaskEvent eventObject) {

        if (eventObject instanceof UserTaskUpdatedEvent userTaskUpdatedEvent){

            editUserTaskCreatedOrUpdatedEvent(userTaskUpdatedEvent);
            var event = this.userTaskMapper.map(userTaskUpdatedEvent);
            sendUserTaskEvent(
                    event.getUserTaskId(),
                    builder -> builder.setUserTaskCreatedOrUpdated(event));

        } else if (eventObject instanceof UserTaskCreatedEvent userTaskCreatedEvent){

            editUserTaskCreatedOrUpdatedEvent(userTaskCreatedEvent);
            var event = this.userTaskMapper.map(userTaskCreatedEvent);
            sendUserTaskEvent(
                    event.getUserTaskId(),
                    builder -> builder.setUserTaskCreatedOrUpdated(event));

        } else if (eventObject instanceof UserTaskCompletedEvent userTaskCompletedEvent) {

            var event = userTaskMapper.map(userTaskCompletedEvent);
            BcEvent.newBuilder().setUserTaskCompleted(event);
            sendUserTaskEvent(
                    event.getUserTaskId(),
                    builder -> builder.setUserTaskCompleted(event));

        } else if (eventObject instanceof UserTaskCancelledEvent userTaskCancelledEvent) {

            var event = userTaskMapper.map(userTaskCancelledEvent);
            sendUserTaskEvent(
                    event.getUserTaskId(),
                    builder -> builder.setUserTaskCancelled(event));

        } else if (eventObject instanceof UserTaskActivatedEvent userTaskActivatedEvent) {

            var event = userTaskMapper.map(userTaskActivatedEvent);
            sendUserTaskEvent(
                    event.getUserTaskId(),
                    builder -> builder.setUserTaskActivated(event));

        } else if (eventObject instanceof UserTaskSuspendedEvent userTaskSuspendedEvent) {

            var event = userTaskMapper.map(userTaskSuspendedEvent);
            sendUserTaskEvent(
                    event.getUserTaskId(),
                    builder -> builder.setUserTaskSuspended(event));

        } else {

            throw new RuntimeException(
                    "Unsupported event type '"
                            + eventObject.getClass().getName()
                            + "'!");

        }
    }

    private void sendUserTaskEvent(final String userTaskId,
                                   final Consumer<BcEvent.Builder> eventSupplier) {

        final var event = BcEvent.newBuilder();
        eventSupplier.accept(event);

        kafkaTemplate.send(
                properties.getCockpit().getKafka().getTopics().getUserTask(),
                userTaskId,
                event.build().toByteArray());

    }

}