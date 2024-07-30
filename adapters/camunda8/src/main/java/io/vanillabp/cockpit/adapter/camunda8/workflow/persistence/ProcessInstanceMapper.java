package io.vanillabp.cockpit.adapter.camunda8.workflow.persistence;

import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowCreatedEvent;

public class ProcessInstanceMapper {
    public static ProcessInstanceEntity map(Camunda8WorkflowCreatedEvent camunda8WorkflowCreatedEvent) {
        ProcessInstanceEntity processInstanceEntity = new ProcessInstanceEntity();
        processInstanceEntity.setProcessInstanceKey(camunda8WorkflowCreatedEvent.getProcessInstanceKey());
        processInstanceEntity.setProcessDefinitionKey(camunda8WorkflowCreatedEvent.getProcessDefinitionKey());
        processInstanceEntity.setBpmnProcessId(camunda8WorkflowCreatedEvent.getBpmnProcessId());
        processInstanceEntity.setVersion(camunda8WorkflowCreatedEvent.getVersion());
        processInstanceEntity.setTenantId(camunda8WorkflowCreatedEvent.getTenantId());
        processInstanceEntity.setBusinessKey(camunda8WorkflowCreatedEvent.getBusinessKey());
        return processInstanceEntity;

    }

    public static Camunda8WorkflowCreatedEvent map(ProcessInstanceEntity processInstanceEntity) {
        Camunda8WorkflowCreatedEvent camunda8WorkflowCreatedEvent = new Camunda8WorkflowCreatedEvent();
        camunda8WorkflowCreatedEvent.setProcessInstanceKey(processInstanceEntity.getProcessInstanceKey());

        if(processInstanceEntity.getProcessDefinitionKey() != null){
            camunda8WorkflowCreatedEvent.setProcessDefinitionKey(processInstanceEntity.getProcessDefinitionKey());
        }
        camunda8WorkflowCreatedEvent.setBpmnProcessId(processInstanceEntity.getBpmnProcessId());

        if(processInstanceEntity.getVersion() != null){
            camunda8WorkflowCreatedEvent.setVersion(processInstanceEntity.getVersion());
        }
        camunda8WorkflowCreatedEvent.setTenantId(processInstanceEntity.getTenantId());
        camunda8WorkflowCreatedEvent.setBusinessKey(processInstanceEntity.getBusinessKey());
        return camunda8WorkflowCreatedEvent;
    }
}
