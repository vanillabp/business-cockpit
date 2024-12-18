package io.vanillabp.cockpit.adapter.camunda8.receiver.events;

import java.time.OffsetDateTime;

public class Camunda8WorkflowLifeCycleEvent {

    public enum Intent {
        CANCELLED,
        COMPLETED,
    }

    private long key;
    private OffsetDateTime timestamp;
    private String deleteReason;
    private long processInstanceKey;
    private String bpmnProcessId;
    private String bpmnProcessVersion;
    private Intent intent;

    private String tenantId;

    public long getKey() {
        return key;
    }

    public void setKey(long key) {
        this.key = key;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Intent getIntent() {
        return intent;
    }

    public void setIntent(Intent intent) {
        this.intent = intent;
    }

    public String getDeleteReason() {
        return deleteReason;
    }

    public void setDeleteReason(String deleteReason) {
        this.deleteReason = deleteReason;
    }

    public long getProcessInstanceKey() {
        return processInstanceKey;
    }

    public void setProcessInstanceKey(long processInstanceKey) {
        this.processInstanceKey = processInstanceKey;
    }

    public String getBpmnProcessId() {
        return bpmnProcessId;
    }

    public void setBpmnProcessId(String bpmnProcessId) {
        this.bpmnProcessId = bpmnProcessId;
    }

    public String getBpmnProcessVersion() {
        return bpmnProcessVersion;
    }

    public void setBpmnProcessVersion(String bpmnProcessVersion) {
        this.bpmnProcessVersion = bpmnProcessVersion;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

}
