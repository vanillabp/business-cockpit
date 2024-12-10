package io.vanillabp.cockpit.adapter.camunda8.wiring;

import io.camunda.zeebe.model.bpmn.instance.Process;
import io.vanillabp.springboot.adapter.Connectable;

public class Camunda8UserTaskConnectable implements Connectable {
    private final Process process;

    private final String elementId;

    private final String taskDefinition;

    private final String title;

    private final String tenantId;
    
    public Camunda8UserTaskConnectable(
            final String tenantId,
            final Process process,
            final String elementId,
            final String taskDefinition,
            final String title) {

        this.tenantId = tenantId;
        this.process = process;
        this.elementId = elementId;
        this.taskDefinition = taskDefinition;
        this.title = title;
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
    
    @Override
    public String getTaskDefinition() {

        return taskDefinition;

    }

    public String getTitle() {
        return title;
    }
    
}