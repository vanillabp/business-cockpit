package io.vanillabp.cockpit.adapter.camunda8.receiver.events;

import io.vanillabp.spi.cockpit.details.DetailsEvent;
import java.time.OffsetDateTime;
import java.util.Map;

public class Camunda8WorkflowEvent {
    private long jobKey;
    private OffsetDateTime timestamp;
    private DetailsEvent.Event event;
    private String bpmnProcessId;
    private String tenantId;
    private int processDefinitionVersion;
    private long processDefinitionKey;
    private long processInstanceKey;
    private Map<String, Object> variables;

    public String getBpmnProcessId() {
        return bpmnProcessId;
    }

    public void setBpmnProcessId(String bpmnProcessId) {
        this.bpmnProcessId = bpmnProcessId;
    }

    public DetailsEvent.Event getEvent() {
        return event;
    }

    public void setEvent(DetailsEvent.Event event) {
        this.event = event;
    }

    public long getJobKey() {
        return jobKey;
    }

    public void setJobKey(long jobKey) {
        this.jobKey = jobKey;
    }

    public long getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public void setProcessDefinitionKey(long processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
    }

    public int getProcessDefinitionVersion() {
        return processDefinitionVersion;
    }

    public void setProcessDefinitionVersion(int processDefinitionVersion) {
        this.processDefinitionVersion = processDefinitionVersion;
    }

    public long getProcessInstanceKey() {
        return processInstanceKey;
    }

    public void setProcessInstanceKey(long processInstanceKey) {
        this.processInstanceKey = processInstanceKey;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }
}
