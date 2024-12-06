package io.vanillabp.cockpit.adapter.common.workflow.kafka;

import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.cockpit.adapter.common.workflow.WorkflowPublishingBase;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCancelledEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCompletedEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCreatedEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.BcEvent;
import java.util.function.Consumer;
import org.springframework.kafka.core.KafkaTemplate;

public class WorkflowKafkaPublishing extends WorkflowPublishingBase implements io.vanillabp.cockpit.adapter.common.workflow.WorkflowPublishing {

    private final WorkflowProtobufMapper workflowMapper;

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public WorkflowKafkaPublishing(
            String workerId,
            VanillaBpCockpitProperties properties,
            WorkflowProtobufMapper workflowMapper,
            KafkaTemplate<String, byte[]> kafkaTemplate
    ) {
        super(workerId, properties);
        this.workflowMapper = workflowMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(WorkflowEvent eventObject) {
        if(eventObject instanceof WorkflowUpdatedEvent workflowUpdatedEvent){

            editWorkflowCreatedOrUpdatedEvent(workflowUpdatedEvent);
            var event = workflowMapper.map(workflowUpdatedEvent);
            this.sendWorkflowEvent(
                    event.getWorkflowId(),
                    builder -> builder.setWorkflowCreatedOrUpdated(event));

        } else if(eventObject instanceof WorkflowCreatedEvent workflowCreatedEvent){

            editWorkflowCreatedOrUpdatedEvent(workflowCreatedEvent);
            var event = workflowMapper.map(workflowCreatedEvent);
            this.sendWorkflowEvent(
                    event.getWorkflowId(),
                    builder -> builder.setWorkflowCreatedOrUpdated(event));

        } else if(eventObject instanceof WorkflowCancelledEvent workflowCancelledEvent){

            var event = workflowMapper.map(workflowCancelledEvent);
            this.sendWorkflowEvent(
                    event.getWorkflowId(),
                    builder -> builder.setWorkflowCancelled(event));

        } else if(eventObject instanceof WorkflowCompletedEvent workflowCompletedEvent) {

            var event = workflowMapper.map(workflowCompletedEvent);
            this.sendWorkflowEvent(
                    event.getWorkflowId(),
                    builder -> builder.setWorkflowCompleted(event));

        }
        // else if suspended
        // else if activated
        else {
            throw new RuntimeException(
                    "Unsupported event type '"
                            + eventObject.getClass().getName()
                            + "'!");
        }
    }

    private void sendWorkflowEvent(final String workflowId,
                                   final Consumer<BcEvent.Builder> eventSupplier) {

        final var event = BcEvent.newBuilder();
        eventSupplier.accept(event);

        kafkaTemplate.send(
                properties.getCockpit().getKafka().getTopics().getWorkflow(),
                workflowId,
                event.build().toByteArray());

    }

}
