package io.vanillabp.cockpit.adapter.camunda7.usertask;

import java.util.HashMap;
import java.util.Map;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Camunda7UserTaskEventHandler implements TaskListener {

    private static final Logger logger = LoggerFactory
            .getLogger(Camunda7UserTaskEventHandler.class);
    
    private static final Map<Camunda7Connectable, Camunda7UserTaskHandler> taskHandlers = new HashMap<>();

    public void addTaskHandler(
            final Camunda7Connectable connectable,
            final Camunda7UserTaskHandler taskHandler) {
        
        taskHandlers.put(connectable, taskHandler);
        
    }

    @Override
    public void notify(
            final DelegateTask delegateTask) {
        
        notify((TaskEntity) delegateTask, delegateTask.getEventName());
        
    }
    
    public void notify(
            final TaskEntity task,
            final String eventName) {

        final var execution = (ExecutionEntity) task.getExecution();
        final var processDefinition = execution
                .getProcessDefinition();
        final var bpmnProcessId = processDefinition
                .getKey();
        
        final var connectableFound = new Camunda7Connectable[1];
        taskHandlers
                .entrySet()
                .stream()
                .filter(entry -> {
                    final var connectable = entry.getKey();
                    
                    if (!connectable.getBpmnProcessId().equals(bpmnProcessId)) {
                        return false;
                    }
                    
                    final var element = execution.getBpmnModelElementInstance();
                    if (element == null) {
                        return false;
                    }
                    
                    return connectable.applies(
                            element.getId(),
                            ((UserTask) element).getCamundaFormKey());
                })
                // found handler-reference
                .findFirst()
                .map(entry -> entry.getValue())
                .ifPresentOrElse(
                        handler -> handler.notify(task, eventName),
                        () -> logger.debug(
                                "Unmapped event '{}'! "
                                + "If you need to process this event add a parameter "
                                + "'@TaskEvent Event event' to the method annotated by "
                                + "'@UserTaskDetailsProvider(taskDefinition = \"{}\") in any class "
                                + "annotated by '@WorkflowService(bpmnProcess = @BpmnProcess(bpmnProcessId = \"{}\"))'.",
                                task.getEventName(),
                                connectableFound[0].getTaskDefinition(),
                                connectableFound[0].getBpmnProcessId()));
        
    }
    
}
