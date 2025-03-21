package io.vanillabp.cockpit.adapter.camunda8.deployments;

public interface ProcessInstance {

    long getProcessInstanceKey();

    String getBusinessKey();

    String getBpmnProcessId();

    Long getVersion();

    Long getProcessDefinitionKey();

    String getTenantId();

}
