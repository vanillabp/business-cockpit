package io.vanillabp.cockpit.adapter.camunda8.receiver.kafka;


import at.phactum.zeebe.exporters.kafka.serde.RecordId;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RecordType;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.value.BpmnElementType;
import io.camunda.zeebe.protocol.record.value.JobRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceCreationRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceRecordValue;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8UserTaskCreatedEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8UserTaskLifecycleEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowCreatedEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowLifeCycleEvent;
import io.vanillabp.cockpit.adapter.camunda8.usertask.Camunda8UserTaskEventHandler;
import io.vanillabp.cockpit.adapter.camunda8.workflow.Camunda8WorkflowEventHandler;
import io.vanillabp.springboot.adapter.VanillaBpProperties;
import java.util.Set;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;

import static io.vanillabp.cockpit.adapter.camunda8.receiver.kafka.KafkaConfiguration.KAFKA_CONSUMER_PREFIX;

public class KafkaController {

    private static final Logger logger = LoggerFactory.getLogger(KafkaController.class);
    private static final String CLIENT_ID = "zeebe-client";

    private final Camunda8UserTaskEventHandler camunda8UserTaskEventHandler;
    private final Camunda8WorkflowEventHandler camunda8WorkflowEventHandler;
    private final Set<String> idNames;

    public KafkaController(
            Camunda8UserTaskEventHandler camunda8UserTaskEventHandler,
            Camunda8WorkflowEventHandler camunda8WorkflowEventHandler,
            Set<String> idNames) {

        this.camunda8UserTaskEventHandler = camunda8UserTaskEventHandler;
        this.camunda8WorkflowEventHandler = camunda8WorkflowEventHandler;
        this.idNames = idNames;
    }

    @KafkaListener(topics = "${" + KafkaConfiguration.ZEEBE_KAFKA_EXPORTER_TOPIC_PROPERTY + "}",
            clientIdPrefix = KAFKA_CONSUMER_PREFIX + "-" + CLIENT_ID + "-${workerId:local}",
            groupId = KAFKA_CONSUMER_PREFIX + "-${" + VanillaBpProperties.PREFIX + ".cockpit.kafka.group-id-suffix:local}",
            containerFactory = "zeebeKafkaListenerContainerFactory")
    public void consumeUserTaskEvent(ConsumerRecord<RecordId, Record<?>> record) {
        Record<?> value = record.value();
        ValueType valueType = value.getValueType();

        if(valueType.equals(ValueType.PROCESS_INSTANCE_CREATION)) {
            handleProcessInstanceCreationRecord(value);
            return;
        }

        if(valueType.equals(ValueType.PROCESS_INSTANCE)) {
            handleProcessInstanceRecord(value);
            return;
        }

        if(valueType.equals(ValueType.JOB)) {
            handleJobRecord(value);
            return;
        }

        logger.trace("Ignoring unsupported Zeebe event '{}' ('{}'): {}",
                value.getKey(),
                valueType,
                value.getValue());

    }


    private void handleProcessInstanceCreationRecord(Record<?> value) {
        if(value.getKey() == -1){
            return;
        }

        ProcessInstanceCreationRecordValue processInstanceCreationRecordValue =
                (ProcessInstanceCreationRecordValue) value.getValue();

        Camunda8WorkflowCreatedEvent workflowCreatedEvent = WorkflowEventZeebeRecordMapper.map(
                processInstanceCreationRecordValue, idNames);
        WorkflowEventZeebeRecordMapper.addMetaData(workflowCreatedEvent, value);
        camunda8WorkflowEventHandler.processWorkflowCreatedEvent(workflowCreatedEvent);
    }

    private void handleProcessInstanceRecord(Record<?> value) {
        ProcessInstanceRecordValue processInstanceRecordValue =
                (ProcessInstanceRecordValue) value.getValue();
        String intent = value.getIntent().name();
        if(processInstanceRecordValue.getBpmnElementType().equals(BpmnElementType.PROCESS) &&
                        (intent.equals("ELEMENT_TERMINATED") || intent.equals("ELEMENT_COMPLETED"))){
            Camunda8WorkflowLifeCycleEvent workflowLifeCycleEvent = WorkflowEventZeebeRecordMapper.map(processInstanceRecordValue);
            WorkflowEventZeebeRecordMapper.addMetaData(workflowLifeCycleEvent, value);
            camunda8WorkflowEventHandler.processWorkflowLifecycleEvent(workflowLifeCycleEvent);
        }
    }

    private void handleJobRecord(Record<?> value) {
        JobRecordValue jobRecordValue = (JobRecordValue) value.getValue();

        if(!value.getRecordType().equals(RecordType.EVENT)){
            return;
        }

        if(!jobRecordValue.getType().equals("io.camunda.zeebe:userTask")){
            return;
        }

        String intentName = value.getIntent().name();
        if(intentName.equals("CREATED")){
            Camunda8UserTaskCreatedEvent camunda8UserTaskCreatedEvent =
                    UserTaskEventZeebeRecordMapper.mapToUserTaskCreatedInformation(jobRecordValue);
            UserTaskEventZeebeRecordMapper.addMetaData(camunda8UserTaskCreatedEvent, value);
            camunda8UserTaskEventHandler.notify(camunda8UserTaskCreatedEvent);
        } else if(Camunda8UserTaskLifecycleEvent.getIntentValueNames().contains(intentName)){
            Camunda8UserTaskLifecycleEvent camunda8UserTaskLifecycleEvent =
                    UserTaskEventZeebeRecordMapper.mapToUserTaskLifecycleInformation(jobRecordValue);
            UserTaskEventZeebeRecordMapper.addMetaData(camunda8UserTaskLifecycleEvent, value);
            camunda8UserTaskEventHandler.notify(camunda8UserTaskLifecycleEvent);
        } else {
            logger.info("Ignored a zeebe job event for a user task with intent={} ", intentName);
        }
    }

}