package io.vanillabp.cockpit.adapter.camunda8.receiver.kafka;


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
import at.phactum.zeebe.exporters.kafka.serde.RecordId;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;

import static io.vanillabp.cockpit.adapter.camunda8.receiver.kafka.KafkaConfiguration.KAFKA_CONSUMER_PREFIX;

public class KafkaController {
    private static final String CLIENT_ID = "zeebe-client";

    private final Camunda8UserTaskEventHandler camunda8UserTaskEventHandler;
    private final Camunda8WorkflowEventHandler camunda8WorkflowEventHandler;

    public KafkaController(
            Camunda8UserTaskEventHandler camunda8UserTaskEventHandler,
            Camunda8WorkflowEventHandler camunda8WorkflowEventHandler) {

        this.camunda8UserTaskEventHandler = camunda8UserTaskEventHandler;
        this.camunda8WorkflowEventHandler = camunda8WorkflowEventHandler;
    }

    @KafkaListener(topics = "${" + KafkaConfiguration.ZEEBE_KAFKA_EXPORTER_TOPIC_PROPERTY + "}",
            clientIdPrefix = KAFKA_CONSUMER_PREFIX + "-" + CLIENT_ID + "-${workerId:local}",
            groupId = KAFKA_CONSUMER_PREFIX,
            containerFactory = "zeebeKafkaListenerContainerFactory")
    public void consumeUserTaskEvent(ConsumerRecord<RecordId, Record<?>> record) {
        Record<?> value = record.value();
        ValueType valueType = value.getValueType();

        if(valueType.equals(ValueType.PROCESS_INSTANCE_CREATION)) {
            handleProcessInstanceCreationRecord(value);
        }

        if(valueType.equals(ValueType.PROCESS_INSTANCE)) {
            handleProcessInstanceRecord(value);
        }

        if(valueType.equals(ValueType.JOB)) {
            handleJobRecord(value);
        }
    }


    private void handleProcessInstanceCreationRecord(Record<?> value) {
        if(value.getKey() == -1){
            return;
        }

        ProcessInstanceCreationRecordValue processInstanceCreationRecordValue =
                (ProcessInstanceCreationRecordValue) value.getValue();
        Camunda8WorkflowCreatedEvent workflowCreatedEvent = WorkflowEventZeebeRecordMapper.map(processInstanceCreationRecordValue);
        WorkflowEventZeebeRecordMapper.addMetaData(workflowCreatedEvent, value);
        camunda8WorkflowEventHandler.notify(workflowCreatedEvent);
    }

    private void handleProcessInstanceRecord(Record<?> value) {
        ProcessInstanceRecordValue processInstanceRecordValue =
                (ProcessInstanceRecordValue) value.getValue();
        String intent = value.getIntent().name();
        if(processInstanceRecordValue.getBpmnElementType().equals(BpmnElementType.PROCESS) &&
                        (intent.equals("ELEMENT_TERMINATED") || intent.equals("ELEMENT_COMPLETED"))){
            Camunda8WorkflowLifeCycleEvent workflowLifeCycleEvent = WorkflowEventZeebeRecordMapper.map(processInstanceRecordValue);
            WorkflowEventZeebeRecordMapper.addMetaData(workflowLifeCycleEvent, value);
            camunda8WorkflowEventHandler.notify(workflowLifeCycleEvent);
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

        if(value.getIntent().name().equals("CREATED")){
            Camunda8UserTaskCreatedEvent camunda8UserTaskCreatedEvent =
                    UserTaskEventZeebeRecordMapper.mapToUserTaskCreatedInformation(jobRecordValue);
            UserTaskEventZeebeRecordMapper.addMetaData(camunda8UserTaskCreatedEvent, value);
            camunda8UserTaskEventHandler.notify(camunda8UserTaskCreatedEvent);
        } else {
            Camunda8UserTaskLifecycleEvent camunda8UserTaskLifecycleEvent =
                    UserTaskEventZeebeRecordMapper.mapToUserTaskLifecycleInformation(jobRecordValue);
            UserTaskEventZeebeRecordMapper.addMetaData(camunda8UserTaskLifecycleEvent, value);
            camunda8UserTaskEventHandler.notify(camunda8UserTaskLifecycleEvent);
        }
    }
}