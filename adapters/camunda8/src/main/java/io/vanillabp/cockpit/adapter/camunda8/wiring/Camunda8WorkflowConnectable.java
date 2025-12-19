package io.vanillabp.cockpit.adapter.camunda8.wiring;

import io.camunda.zeebe.model.bpmn.instance.Process;
import io.vanillabp.springboot.adapter.Connectable;

public class Camunda8WorkflowConnectable implements Connectable {

    private final String workflowModuleId;

    private final io.camunda.zeebe.model.bpmn.instance.Process process;

    private String versionInfo;

    private final String tenantId;

    public Camunda8WorkflowConnectable(
            final String workflowModuleId,
            final String tenantId,
            final Process process,
            final String versionInfo) {

        this.workflowModuleId = workflowModuleId;
        this.tenantId = tenantId;
        this.process = process;
        this.versionInfo = versionInfo;
    }

    public String getWorkflowModuleId() {
        return workflowModuleId;
    }

    public String getTenantId() {
        return tenantId;
    }

    @Override
    public String getElementId() {

        return process.getId();

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

        return process.getId();

    }

}
