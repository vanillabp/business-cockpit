package io.vanillabp.cockpit.adapter.camunda8.deployments.jpa;

import io.vanillabp.cockpit.adapter.camunda8.deployments.ProcessInstance;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "CAMUNDA8_BC_PROCESS_INSTANCES")
public class ProcessInstanceEntity implements ProcessInstance {

    @Id
    @Column(name = "PROCESS_INSTANCE_KEY")
    private long processInstanceKey;

    @Column(name = "BUSINESS_KEY")
    private String businessKey;

    @Column(name = "BPMN_PROCESS_ID")
    private String bpmnProcessId;

    @Column(name = "VERSION")
    private Long version;

    @Column(name = "PROCESS_DEFINITION_KEY")
    private Long processDefinitionKey;

    @Column(name = "TENANT_ID")
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
