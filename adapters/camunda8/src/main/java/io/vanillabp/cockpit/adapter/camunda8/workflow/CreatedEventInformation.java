package io.vanillabp.cockpit.adapter.camunda8.workflow;

import java.util.Optional;

public class CreatedEventInformation {
    private long key;

    private long timestamp;

    private String bpmnProcessId;

    private long version;

    private long processInstanceKey;

    private long processDefinitionKey;

    private String businessKey;

    private String tenantId;

    public long getKey() {
        return key;
    }

    public void setKey(long key) {
        this.key = key;
    }

    public long getProcessInstanceKey() {
        return processInstanceKey;
    }

    public void setProcessInstanceKey(long processInstanceKey) {
        this.processInstanceKey = processInstanceKey;
    }

    public long getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public void setProcessDefinitionKey(long processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getBpmnProcessId() {
        return bpmnProcessId;
    }

    public void setBpmnProcessId(String bpmnProcessId) {
        this.bpmnProcessId = bpmnProcessId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }
}
