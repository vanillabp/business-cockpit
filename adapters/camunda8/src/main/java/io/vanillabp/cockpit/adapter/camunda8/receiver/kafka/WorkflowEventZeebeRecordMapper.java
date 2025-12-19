package io.vanillabp.cockpit.adapter.camunda8.receiver.kafka;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RecordValueWithVariables;
import io.camunda.zeebe.protocol.record.value.ProcessEventRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceCreationRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceRecordValue;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowCreatedEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowLifeCycleEvent;
import java.util.Set;
import java.util.function.Supplier;

public class WorkflowEventZeebeRecordMapper {

    public static Camunda8WorkflowCreatedEvent map(
            ProcessInstanceCreationRecordValue record,
            KafkaController.ProcessInformation processInformation) {
        final var workflowCreatedEvent = new Camunda8WorkflowCreatedEvent();

        workflowCreatedEvent.setTenantId(record.getTenantId());
        final var version = processInformation.getVersionTag() != null
                ? "%s:%d".formatted(processInformation.getVersionTag(), processInformation.getVersion())
                : "%d".formatted(processInformation.getVersion());
        workflowCreatedEvent.setVersion(version);
        workflowCreatedEvent.setProcessDefinitionKey(record.getProcessDefinitionKey());
        workflowCreatedEvent.setProcessInstanceKey(record.getProcessInstanceKey());
        workflowCreatedEvent.setBpmnProcessId(record.getBpmnProcessId());
        workflowCreatedEvent.setProcessDefinitionKey(
                record.getProcessDefinitionKey());

        return workflowCreatedEvent;
    }

    public static Camunda8WorkflowCreatedEvent map(
            ProcessEventRecordValue record,
            KafkaController.ProcessInformation processInformation) {
        final var workflowCreatedEvent = new Camunda8WorkflowCreatedEvent();

        workflowCreatedEvent.setTenantId(record.getTenantId());
        final var version = processInformation.getVersionTag() != null
                ? "%s:%d".formatted(processInformation.getVersionTag(), processInformation.getVersion())
                : "%d".formatted(processInformation.getVersion());
        workflowCreatedEvent.setVersion(version);
        workflowCreatedEvent.setProcessDefinitionKey(record.getProcessDefinitionKey());
        workflowCreatedEvent.setProcessInstanceKey(record.getProcessInstanceKey());
        workflowCreatedEvent.setBpmnProcessId(processInformation.getBpmnProcessId());
        workflowCreatedEvent.setProcessDefinitionKey(
                record.getProcessDefinitionKey());

        return workflowCreatedEvent;
    }

    public static void addMetaData(
            final Camunda8WorkflowCreatedEvent workflowCreatedEvent,
            final Record<?> value,
            final Supplier<Set<String>> idNames){
        
        workflowCreatedEvent.setKey(value.getKey());
        workflowCreatedEvent.setTimestamp(value.getTimestamp());
        setBusinessKey(value, workflowCreatedEvent, idNames);

    }

    private static void setBusinessKey(Record<?> value,
                                       Camunda8WorkflowCreatedEvent workflowCreatedEvent,
                                       Supplier<Set<String>> idNames) {

        if (!(value.getValue() instanceof RecordValueWithVariables variableValue)) {
            return;
        }
        final var variables = variableValue.getVariables();
        if ((variables == null) || variables.isEmpty()) {
            return;
        }

        idNames
                .get()
                .stream()
                .filter(variables::containsKey)
                .findFirst()
                .ifPresent(idName -> workflowCreatedEvent.setBusinessKey((String) variables.get(idName)));

    }

    public static Camunda8WorkflowLifeCycleEvent map(ProcessInstanceRecordValue processInstanceRecord){
        Camunda8WorkflowLifeCycleEvent workflowLifeCycleEvent = new Camunda8WorkflowLifeCycleEvent();
        workflowLifeCycleEvent.setProcessInstanceKey(
                processInstanceRecord.getProcessInstanceKey());
        workflowLifeCycleEvent.setProcessDefinitionKey(
                processInstanceRecord.getProcessDefinitionKey());
        workflowLifeCycleEvent.setBpmnProcessId(
                processInstanceRecord.getBpmnProcessId());
        workflowLifeCycleEvent.setWorkflowDefinitionVersion(
                processInstanceRecord.getVersion());
        workflowLifeCycleEvent.setTenantId(
                processInstanceRecord.getTenantId());
        return workflowLifeCycleEvent;
    }


    public static void addMetaData(Camunda8WorkflowLifeCycleEvent userTaskCreatedEvent, Record<?> task){
        userTaskCreatedEvent.setKey(task.getKey());
        userTaskCreatedEvent.setTimestamp(task.getTimestamp());
        userTaskCreatedEvent.setIntent(getIntent(task));
    }

    private static Camunda8WorkflowLifeCycleEvent.Intent getIntent(Record<?> processInstanceRecord) {
        String intent = processInstanceRecord.getIntent().name();
        if(intent.equals("ELEMENT_COMPLETED")){
            return Camunda8WorkflowLifeCycleEvent.Intent.COMPLETED;
        }
        if(intent.equals("ELEMENT_TERMINATED")){
            return Camunda8WorkflowLifeCycleEvent.Intent.CANCELLED;
        }
        return null;
    }
}
