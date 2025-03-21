package io.vanillabp.cockpit.adapter.camunda8.deployments.mongodb;

import io.vanillabp.cockpit.adapter.camunda8.deployments.ProcessInstance;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "CAMUNDA8_BC_PROCESS_INSTANCES")
public class ProcessInstanceEntity implements ProcessInstance {

    @Id
    @Field(name = "PROCESS_INSTANCE_KEY")
    private long processInstanceKey;

    @Field(name = "BUSINESS_KEY")
    @Indexed
    private String businessKey;

    @Field(name = "BPMN_PROCESS_ID")
    private String bpmnProcessId;

    @Field(name = "VERSION")
    private Long version;

    @Field(name = "PROCESS_DEFINITION_KEY")
    private Long processDefinitionKey;

    @Field(name = "TENANT_ID")
    private String tenantId;


    public ProcessInstanceEntity(){
    };

    public ProcessInstanceEntity(long processInstanceKey, String businessKey) {
        this.processInstanceKey = processInstanceKey;
        this.businessKey = businessKey;
    }

    public long getProcessInstanceKey() {
        return processInstanceKey;
    }

    public void setProcessInstanceKey(long processInstanceKey) {
        this.processInstanceKey = processInstanceKey;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    public String getBpmnProcessId() {
        return bpmnProcessId;
    }

    public void setBpmnProcessId(String bpmnProcessId) {
        this.bpmnProcessId = bpmnProcessId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Long getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public void setProcessDefinitionKey(Long processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
