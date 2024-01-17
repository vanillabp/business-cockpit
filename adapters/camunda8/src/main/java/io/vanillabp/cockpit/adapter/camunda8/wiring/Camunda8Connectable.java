package io.vanillabp.cockpit.adapter.camunda8.wiring;

import io.camunda.zeebe.model.bpmn.instance.Process;
import io.camunda.zeebe.model.bpmn.instance.zeebe.ZeebeLoopCharacteristics;
import io.vanillabp.springboot.adapter.Connectable;

public class Camunda8Connectable implements Connectable {
    
    public enum Type {
        TASK, USERTASK
    };

    private final Process process;

    private final String elementId;
    
    private final Type type;

    private final String taskDefinition;
    
    public Camunda8Connectable(
            final Process process,
            final String elementId,
            final Type type,
            final String taskDefinition,
            final ZeebeLoopCharacteristics loopCharacteristics) {

        this.process = process;
        this.elementId = elementId;
        this.taskDefinition = taskDefinition;
        this.type = type;

    }
    
    @Override
    public String getElementId() {

        return elementId;

    }
    
    public Type getType() {
        
        return type;
        
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
    
}