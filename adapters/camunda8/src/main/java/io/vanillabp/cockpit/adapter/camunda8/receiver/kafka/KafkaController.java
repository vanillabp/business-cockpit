package io.vanillabp.cockpit.adapter.camunda8.receiver.kafka;

import at.phactum.zeebe.exporters.kafka.serde.RecordId;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.command.ClientException;
import io.camunda.spring.client.event.CamundaClientCreatedEvent;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RecordType;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.intent.ProcessEventIntent;
import io.camunda.zeebe.protocol.record.intent.ProcessInstanceIntent;
import io.camunda.zeebe.protocol.record.value.BpmnElementType;
import io.camunda.zeebe.protocol.record.value.JobRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessEventRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceCreationRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceRecordValue;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8UserTaskCreatedEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8UserTaskLifecycleEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowLifeCycleEvent;
import io.vanillabp.cockpit.adapter.camunda8.usertask.Camunda8UserTaskEventHandler;
import io.vanillabp.cockpit.adapter.camunda8.workflow.Camunda8WorkflowEventHandler;
import io.vanillabp.springboot.adapter.VanillaBpProperties;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;

import static io.vanillabp.cockpit.adapter.camunda8.receiver.kafka.KafkaConfiguration.KAFKA_CONSUMER_PREFIX;

public class KafkaController {

    public static class ProcessInformation {
        private final String bpmnProcessId;
        private final long version;
        private final String versionTag;

        public ProcessInformation(
                final String bpmnProcessId,
                final String versionTag,
                final long version) {
            this.bpmnProcessId = bpmnProcessId;
            this.version = version;
            this.versionTag = versionTag;
        }

        public long getVersion() {
            return version;
        }

        public String getBpmnProcessId() {
            return bpmnProcessId;
        }

        public String getVersionTag() {
            return versionTag;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(KafkaController.class);
    private static final String CLIENT_ID = "zeebe-client";

    private static final Map<Long, ProcessInformation> processDefinitions = new HashMap<>();

    private final Camunda8UserTaskEventHandler camunda8UserTaskEventHandler;
    private final Camunda8WorkflowEventHandler camunda8WorkflowEventHandler;
    private final Supplier<Set<String>> idNames;

    private CamundaClient client;

    public KafkaController(
            Camunda8UserTaskEventHandler camunda8UserTaskEventHandler,
            Camunda8WorkflowEventHandler camunda8WorkflowEventHandler,
            Supplier<Set<String>> idNames) {

        this.camunda8UserTaskEventHandler = camunda8UserTaskEventHandler;
        this.camunda8WorkflowEventHandler = camunda8WorkflowEventHandler;
        this.idNames = idNames;
    }

    @EventListener
    public void camundaClientCreated(
            final CamundaClientCreatedEvent event) {

        this.client = event.getClient();

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
        if (valueType.equals(ValueType.PROCESS_EVENT)) {
            handleProcessEventRecordValue(value);
            return;
        }
        if (valueType.equals(ValueType.PROCESS_INSTANCE)) {
            handleProcessInstanceRecord(value);
            return;
        }
        if (valueType.equals(ValueType.JOB)) {
            handleJobRecord(value);
            return;
        }

    }

    private void handleProcessInstanceCreationRecord(
            final Record<?> value) {

        if(value.getKey() == -1){
            return;
        }
        // empty (none) start event
        final var processInstanceCreationRecordValue = (ProcessInstanceCreationRecordValue) value.getValue();
        final var processInformation = getProcessInformation(processInstanceCreationRecordValue.getProcessDefinitionKey());
        if (processInformation.isEmpty()) {
            if (camunda8WorkflowEventHandler.isTenantKnown(processInstanceCreationRecordValue.getTenantId())) {
                logger.warn("No process information found for process definition key '{}'!" +
                                "The workflow of tenant '{}' with id '{}' may not work properly!",
                        processInstanceCreationRecordValue.getProcessDefinitionKey(),
                        processInstanceCreationRecordValue.getTenantId(),
                        processInstanceCreationRecordValue.getBpmnProcessId());
            }
            return;
        }
        final var workflowCreatedEvent = WorkflowEventZeebeRecordMapper.map(processInstanceCreationRecordValue, processInformation.get());
        WorkflowEventZeebeRecordMapper.addMetaData(workflowCreatedEvent, value, idNames);
        camunda8WorkflowEventHandler.processCreatedEvent(workflowCreatedEvent);

    }

    private Optional<ProcessInformation> getProcessInformation(final long processDefinitionKey) {

        final var cachedResult = processDefinitions.get(processDefinitionKey);
        if (cachedResult != null) {
            return Optional.of(cachedResult);
        }

        try {
            final var processDefinition = client
                    .newProcessDefinitionGetRequest(processDefinitionKey)
                    .send()
                    .join();
            final var result = new ProcessInformation(
                    processDefinition.getProcessDefinitionId(),
                    processDefinition.getVersionTag(),
                    processDefinition.getVersion());
            processDefinitions.put(processDefinitionKey, result);
            return Optional.of(result);
        } catch (ClientException e) {
            logger.warn("Could not load process information for process definition key '{}'.", processDefinitionKey, e);
            return Optional.empty();
        }

    }

    private void handleProcessEventRecordValue(Record<?> value) {
        if (!value.getIntent().equals(ProcessEventIntent.TRIGGERING)) {
            return;
        }
        final var processEventRecordValue = (ProcessEventRecordValue) value.getValue();
        if (processEventRecordValue.getScopeKey() != processEventRecordValue.getProcessDefinitionKey()) {
            // not a process start event
            return;
        }
        // message start event
        final var processInformation = getProcessInformation(processEventRecordValue.getProcessDefinitionKey());
        if (processInformation.isEmpty()) {
            if (camunda8WorkflowEventHandler.isTenantKnown(processEventRecordValue.getTenantId())) {
                logger.warn("No process information found for process definition key '{}'!" +
                                "The workflow of tenant '{}' with target element id '{}' will not be shown in business cockpit!",
                        processEventRecordValue.getProcessDefinitionKey(),
                        processEventRecordValue.getTenantId(),
                        processEventRecordValue.getTargetElementId());
            }
            return;
        }
        final var workflowCreatedEvent = WorkflowEventZeebeRecordMapper
                .map(processEventRecordValue, processInformation.get());
        WorkflowEventZeebeRecordMapper.addMetaData(workflowCreatedEvent, value, idNames);
        camunda8WorkflowEventHandler.processCreatedEvent(workflowCreatedEvent);

    }

    private void handleProcessInstanceRecord(Record<?> value) {
        ProcessInstanceRecordValue processInstanceRecordValue =
                (ProcessInstanceRecordValue) value.getValue();
        String intent = value.getIntent().name();
        if (!processInstanceRecordValue.getBpmnElementType().equals(BpmnElementType.PROCESS)) {
            return;
        }
        // call-activity
        if ((value.getIntent().equals(ProcessInstanceIntent.ELEMENT_ACTIVATING))
                && processInstanceRecordValue.getBpmnElementType().equals(BpmnElementType.PROCESS)
                && (processInstanceRecordValue.getParentProcessInstanceKey() != -1)
                && (processInstanceRecordValue.getFlowScopeKey() == -1)) {
            final var processInformation = getProcessInformation(processInstanceRecordValue.getProcessDefinitionKey());
            if (processInformation.isEmpty()) {
                if (camunda8WorkflowEventHandler.isTenantKnown(processInstanceRecordValue.getTenantId())) {
                    logger.warn("No process information found for process definition key '{}'!" +
                                    "The workflow of tenant '{}' with id '{}' may not work properly!",
                            processInstanceRecordValue.getProcessDefinitionKey(),
                            processInstanceRecordValue.getTenantId(),
                            processInstanceRecordValue.getBpmnProcessId());
                }
                return;
            }
        }
        // process instance lifecycle
        if (intent.equals("ELEMENT_TERMINATED") || intent.equals("ELEMENT_COMPLETED")) {
            Camunda8WorkflowLifeCycleEvent workflowLifeCycleEvent = WorkflowEventZeebeRecordMapper.map(processInstanceRecordValue);
            WorkflowEventZeebeRecordMapper.addMetaData(workflowLifeCycleEvent, value);
            camunda8WorkflowEventHandler.processLifecycleEvent(workflowLifeCycleEvent);
        }
    }

    private void handleJobRecord(Record<?> value) {
        JobRecordValue jobRecordValue = (JobRecordValue) value.getValue();

        if (!value.getRecordType().equals(RecordType.EVENT)) {
            return;
        }
        if (jobRecordValue.getType().equals("io.camunda.zeebe:userTask")) {
            return;
        }
        // job lifecycle
        String intentName = value.getIntent().name();
        if (intentName.equals("CREATED")) {
            Camunda8UserTaskCreatedEvent camunda8UserTaskCreatedEvent =
                    UserTaskEventZeebeRecordMapper.mapToUserTaskCreatedInformation(jobRecordValue, idNames);
            UserTaskEventZeebeRecordMapper.addMetaData(camunda8UserTaskCreatedEvent, value);
            camunda8UserTaskEventHandler.processCreatedEvent(camunda8UserTaskCreatedEvent);
        } else if (Camunda8UserTaskLifecycleEvent.getIntentValueNames().contains(intentName)) {
            Camunda8UserTaskLifecycleEvent camunda8UserTaskLifecycleEvent =
                    UserTaskEventZeebeRecordMapper.mapToUserTaskLifecycleInformation(jobRecordValue);
            UserTaskEventZeebeRecordMapper.addMetaData(camunda8UserTaskLifecycleEvent, value);
            camunda8UserTaskEventHandler.processLifecycleEvent(camunda8UserTaskLifecycleEvent);
        }

    }

}