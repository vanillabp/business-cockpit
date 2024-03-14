package io.vanillabp.cockpit.adapter.camunda8.receiver.redis;

import com.google.protobuf.Value;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowCreatedEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowLifeCycleEvent;
import io.zeebe.exporter.proto.Schema;

import java.util.Map;

public class WorkflowEventProtobufMapper {

    public static Camunda8WorkflowCreatedEvent map(Schema.ProcessInstanceCreationRecord processInstanceCreationRecord) {
        Camunda8WorkflowCreatedEvent workflowCreatedEvent = new Camunda8WorkflowCreatedEvent();

        workflowCreatedEvent.setKey(
                processInstanceCreationRecord.getMetadata().getKey());
        workflowCreatedEvent.setTimestamp(
                processInstanceCreationRecord.getMetadata().getTimestamp());
        // TODO: replace with info from event
        workflowCreatedEvent.setTenantId("demo");
        workflowCreatedEvent.setVersion(
                processInstanceCreationRecord.getVersion());
        workflowCreatedEvent.setProcessDefinitionKey(
                processInstanceCreationRecord.getProcessDefinitionKey());
        workflowCreatedEvent.setProcessInstanceKey(
                processInstanceCreationRecord.getProcessInstanceKey());
        workflowCreatedEvent.setBpmnProcessId(
                processInstanceCreationRecord.getBpmnProcessId());

        setBusinessKey(processInstanceCreationRecord, workflowCreatedEvent);

        return workflowCreatedEvent;
    }

    private static void setBusinessKey(Schema.ProcessInstanceCreationRecord processInstanceCreationRecord, Camunda8WorkflowCreatedEvent workflowCreatedEvent) {
        if(!processInstanceCreationRecord.hasVariables()){
            return;
        }
        Map<String, Value> fieldsMap = processInstanceCreationRecord.getVariables().getFieldsMap();

        if(fieldsMap.containsKey("id")){
            workflowCreatedEvent.setBusinessKey(fieldsMap.get("id").getStringValue());
        }
    }

    public static Camunda8WorkflowLifeCycleEvent map(Schema.ProcessInstanceRecord processInstanceRecord){
        Camunda8WorkflowLifeCycleEvent workflowLifeCycleEvent = new Camunda8WorkflowLifeCycleEvent();

        workflowLifeCycleEvent.setKey(
                processInstanceRecord.getMetadata().getKey());
        workflowLifeCycleEvent.setTimestamp(
                processInstanceRecord.getMetadata().getTimestamp());
        workflowLifeCycleEvent.setProcessInstanceKey(
                processInstanceRecord.getProcessInstanceKey());
        workflowLifeCycleEvent.setBpmnProcessId(
                processInstanceRecord.getBpmnProcessId());
        workflowLifeCycleEvent.setBpmnProcessVersion(
                workflowLifeCycleEvent.getBpmnProcessVersion());

        workflowLifeCycleEvent.setIntent(
                getIntent(processInstanceRecord)
        );

        return workflowLifeCycleEvent;
    }

    private static Camunda8WorkflowLifeCycleEvent.Intent getIntent(Schema.ProcessInstanceRecord processInstanceRecord) {
        String intent = processInstanceRecord.getMetadata().getIntent();
        if(intent.equals("ELEMENT_COMPLETED")){
            return Camunda8WorkflowLifeCycleEvent.Intent.COMPLETED;
        }
        if(intent.equals("ELEMENT_TERMINATED")){
            return Camunda8WorkflowLifeCycleEvent.Intent.CANCELLED;
        }
        return null;
    }
}
