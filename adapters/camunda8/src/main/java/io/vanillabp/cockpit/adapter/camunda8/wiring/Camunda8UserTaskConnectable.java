package io.vanillabp.cockpit.adapter.camunda8.wiring;

import io.camunda.zeebe.model.bpmn.instance.Process;
import io.vanillabp.springboot.adapter.Connectable;

public class Camunda8UserTaskConnectable implements Connectable {

    private final String workflowModuleId;

    private final Process process;

    private String versionInfo;

    private final String elementId;

    private final String taskDefinition;

    private final String title;

    private final String tenantId;
    
    public Camunda8UserTaskConnectable(
            final String workflowModuleId,
            final String tenantId,
            final Process process,
            final String versionInfo,
            final String elementId,
            final String taskDefinition,
            final String title) {

        this.workflowModuleId = workflowModuleId;
        this.tenantId = tenantId;
        this.process = process;
        this.versionInfo = versionInfo;
        this.elementId = elementId;
        this.taskDefinition = taskDefinition;
        this.title = title;
    }

    public String getWorkflowModuleId() {
        return workflowModuleId;
    }

    public String getTenantId() {
        return tenantId;
    }

    @Override
    public String getElementId() {

        return elementId;

    }
    
    @Override
    public boolean isExecutableProcess() {

        return process.isExecutable();

    }

    @Override
    public String getBpmnProcessId() {

        return process.getId();

    }

    public String getBpmnProcessName() {
        return process.getName();
    }

    @Override
    public String getVersionInfo() {

        return versionInfo;

    }

    public void updateVersionInfo(
            final String versionInfo) {

        this.versionInfo = versionInfo;

    }

    @Override
    public String getTaskDefinition() {

        return taskDefinition;

    }

    public String getTitle() {
        return title;
    }
    
}