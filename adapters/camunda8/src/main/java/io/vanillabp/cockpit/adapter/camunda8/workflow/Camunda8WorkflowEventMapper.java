package io.vanillabp.cockpit.adapter.camunda8.workflow;

import com.google.protobuf.Value;
import io.zeebe.exporter.proto.Schema;

import java.util.Map;

public class Camunda8WorkflowEventMapper {

    public static CreatedEventInformation map(Schema.ProcessInstanceCreationRecord processInstanceCreationRecord) {
        CreatedEventInformation createdEventInformation = new CreatedEventInformation();

        createdEventInformation.setKey(
                processInstanceCreationRecord.getMetadata().getKey());

        createdEventInformation.setTimestamp(
                processInstanceCreationRecord.getMetadata().getTimestamp());

        // TODO: replace with info from event
        createdEventInformation.setTenantId("demo");

        createdEventInformation.setVersion(
                processInstanceCreationRecord.getVersion());

        createdEventInformation.setProcessDefinitionKey(
                processInstanceCreationRecord.getProcessDefinitionKey());

        createdEventInformation.setProcessInstanceKey(
                processInstanceCreationRecord.getProcessInstanceKey());

        createdEventInformation.setBpmnProcessId(
                processInstanceCreationRecord.getBpmnProcessId());

        Map<String, Value> fieldsMap = processInstanceCreationRecord.getVariables().getFieldsMap();
        if(fieldsMap.containsKey("id")){
            createdEventInformation.setBusinessKey(fieldsMap.get("id").getStringValue());
        }

        return createdEventInformation;
    }
}
