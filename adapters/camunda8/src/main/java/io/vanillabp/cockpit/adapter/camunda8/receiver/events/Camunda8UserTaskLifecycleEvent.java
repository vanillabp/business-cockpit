package io.vanillabp.cockpit.adapter.camunda8.receiver.events;

public class Camunda8UserTaskLifecycleEvent {

    public enum Intent {
        CANCELED,
        COMPLETED,

    }

    private long key;
    private long timestamp;
    private Intent intent;
    private String formKey;
    private String elementId;
    private long elementInstanceKey;
    private String bpmnProcessId;
    private int workflowDefinitionVersion;
    private long processInstanceKey;
    private long processDefinitionKey;

    public long getKey() {
        return key;
    }

    public void setKey(long key) {
        this.key = key;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Intent getIntent() {
        return intent;
    }

    public void setIntent(Intent intent) {
        this.intent = intent;
    }

    public String getFormKey() {
        return formKey;
    }

    public void setFormKey(String formKey) {
        this.formKey = formKey;
    }

    public String getElementId() {
        return elementId;
    }

    public void setElementId(String elementId) {
        this.elementId = elementId;
    }

    public long getElementInstanceKey() {
        return elementInstanceKey;
    }

    public void setElementInstanceKey(long elementInstanceKey) {
        this.elementInstanceKey = elementInstanceKey;
    }

    public String getBpmnProcessId() {
        return bpmnProcessId;
    }

    public void setBpmnProcessId(String bpmnProcessId) {
        this.bpmnProcessId = bpmnProcessId;
    }

    public int getWorkflowDefinitionVersion() {
        return workflowDefinitionVersion;
    }

    public void setWorkflowDefinitionVersion(int workflowDefinitionVersion) {
        this.workflowDefinitionVersion = workflowDefinitionVersion;
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
}
