package io.vanillabp.cockpit.adapter.camunda8.receiver.kafka;

import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceCreationRecordValue;
import io.camunda.zeebe.protocol.record.value.ProcessInstanceRecordValue;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowCreatedEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowLifeCycleEvent;
import java.util.Map;
import java.util.Set;

public class WorkflowEventZeebeRecordMapper {

    public static Camunda8WorkflowCreatedEvent map(
            ProcessInstanceCreationRecordValue processInstanceCreationRecord,
            Set<String> idNames) {
        Camunda8WorkflowCreatedEvent workflowCreatedEvent = new Camunda8WorkflowCreatedEvent();

        workflowCreatedEvent.setTenantId(
                processInstanceCreationRecord.getTenantId());
        workflowCreatedEvent.setVersion(
                processInstanceCreationRecord.getVersion());
        workflowCreatedEvent.setProcessDefinitionKey(
                processInstanceCreationRecord.getProcessDefinitionKey());
        workflowCreatedEvent.setProcessInstanceKey(
                processInstanceCreationRecord.getProcessInstanceKey());
        workflowCreatedEvent.setBpmnProcessId(
                processInstanceCreationRecord.getBpmnProcessId());

        setBusinessKey(processInstanceCreationRecord, workflowCreatedEvent, idNames);

        return workflowCreatedEvent;
    }


    public static void addMetaData(Camunda8WorkflowCreatedEvent userTaskCreatedEvent, Record<?> task){
        userTaskCreatedEvent.setKey(task.getKey());
        userTaskCreatedEvent.setTimestamp(task.getTimestamp());
    }


    private static void setBusinessKey(ProcessInstanceCreationRecordValue processInstanceCreationRecord,
                                       Camunda8WorkflowCreatedEvent workflowCreatedEvent,
                                       Set<String> idNames) {

        Map<String, Object> variables = processInstanceCreationRecord.getVariables();
        if(variables == null) {
            return;
        }

        idNames.stream()
                .filter(variables::containsKey)
                .findFirst()
                .ifPresent(idName -> workflowCreatedEvent.setBusinessKey((String) variables.get(idName))) ;

    }

    public static Camunda8WorkflowLifeCycleEvent map(ProcessInstanceRecordValue processInstanceRecord){
        Camunda8WorkflowLifeCycleEvent workflowLifeCycleEvent = new Camunda8WorkflowLifeCycleEvent();
        workflowLifeCycleEvent.setProcessInstanceKey(
                processInstanceRecord.getProcessInstanceKey());
        workflowLifeCycleEvent.setBpmnProcessId(
                processInstanceRecord.getBpmnProcessId());
        workflowLifeCycleEvent.setBpmnProcessVersion(
                Integer.toString(processInstanceRecord.getVersion()));
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
