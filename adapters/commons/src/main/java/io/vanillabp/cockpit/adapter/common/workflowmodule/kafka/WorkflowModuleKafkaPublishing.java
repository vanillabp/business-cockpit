package io.vanillabp.cockpit.adapter.common.workflowmodule.kafka;

import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.cockpit.adapter.common.workflowmodule.WorkflowModulePublishing;
import io.vanillabp.cockpit.adapter.common.workflowmodule.WorkflowModulePublishingBase;
import io.vanillabp.cockpit.adapter.common.workflowmodule.events.RegisterWorkflowModuleEvent;
import io.vanillabp.cockpit.adapter.common.workflowmodule.events.WorkflowModuleEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.BcEvent;
import io.vanillabp.spi.cockpit.workflowmodules.WorkflowModuleDetailsProvider;

import java.util.List;
import java.util.function.Consumer;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;

public class WorkflowModuleKafkaPublishing extends WorkflowModulePublishingBase implements WorkflowModulePublishing {

    private final WorkflowModuleProtobufMapper mapper;

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public WorkflowModuleKafkaPublishing(
            String workerId,
            VanillaBpCockpitProperties properties,
            ObjectProvider<List<WorkflowModuleDetailsProvider>> workflowModuleDetailsProviders,
            WorkflowModuleProtobufMapper mapper,
            KafkaTemplate<String, byte[]> kafkaTemplate) {

        super(workerId, properties, workflowModuleDetailsProviders);
        this.mapper = mapper;
        this.kafkaTemplate = kafkaTemplate;

    }

    @Override
    public void publish(
            final WorkflowModuleEvent eventObject) {

        if (eventObject instanceof RegisterWorkflowModuleEvent registerWorkflowModuleEvent){

            enrichRegisterWorkflowModuleEvent(registerWorkflowModuleEvent);
            final var event = mapper.map(registerWorkflowModuleEvent);
            this.sendWorkflowModuleEvent(
                    event.getWorkflowModuleId(),
                    builder -> builder.setRegisterWorkflowModule(event));

        }  else {

            throw new RuntimeException(
                    "Unsupported event type '"
                            + eventObject.getClass().getName()
                            + "'!");

        }

    }

    private void sendWorkflowModuleEvent(
            final String workflowModuleId,
            final Consumer<BcEvent.Builder> eventSupplier) {

        final var event = BcEvent.newBuilder();
        eventSupplier.accept(event);

        kafkaTemplate.send(
                properties.getCockpit().getKafka().getTopics().getWorkflowModule(),
                workflowModuleId,
                event.build().toByteArray());

    }

}
