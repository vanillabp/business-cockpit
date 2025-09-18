package io.vanillabp.cockpit.adapter.camunda7.usertask;

import io.vanillabp.springboot.adapter.Connectable;

public class Camunda7Connectable implements Connectable {
    
    private final String bpmnProcessId;
    private final String versionInfo;
    private final String elementId;
    private final String taskDefinition;
    
    public Camunda7Connectable(
            final String bpmnProcessId,
            final String versionInfo,
            final String elementId,
            final String taskDefinition) {

        this.bpmnProcessId = bpmnProcessId;
        this.versionInfo = versionInfo;
        this.elementId = elementId;
        this.taskDefinition = taskDefinition;

    }
    
    public boolean applies(
            final String elementId,
            final String taskDefinition) {

        return getElementId().equals(elementId)
                || getTaskDefinition().equals(taskDefinition);
        
    }
    
    @Override
    public boolean isExecutableProcess() {
        
        return true;
        
    }
    
    @Override
    public String getElementId() {
        
        return elementId;
        
    }
    
    @Override
    public String getBpmnProcessId() {
        
        return bpmnProcessId;
        
    }

    @Override
    public String getVersionInfo() {

        return versionInfo;

    }

    @Override
    public String getTaskDefinition() {
        
        return taskDefinition;
        
    }
    
}