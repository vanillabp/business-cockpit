package io.vanillabp.cockpit.adapter.camunda7.usertask;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
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

    private static final Object dummyReturnValue = new Object();

    public void notify(
            final TaskEntity task,
            final String eventName) {

        handleProcessEntity(task, handler -> {
                handler.notify(task, eventName);
                return dummyReturnValue; // action needs to return something, otherwise misleading warning is printed
            });

    }

    public io.vanillabp.spi.cockpit.usertask.UserTask getUserTask(
            final TaskEntity task) {

        return handleProcessEntity(
                task,
                handler -> handler.getUserTask(task));

    }

    private <R> R handleProcessEntity(
            final TaskEntity task,
            final Function<Camunda7UserTaskHandler, R> action) {

        final var execution = (ExecutionEntity) task.getExecution();
        final var processDefinition = execution
                .getProcessDefinition();
        final var bpmnProcessId = processDefinition
                .getKey();

        final var connectableFound = new Camunda7Connectable[1];
        return taskHandlers
                .entrySet()
                .stream()
                .filter(entry -> {
                    final var connectable = entry.getKey();

                    if (!connectable.getBpmnProcessId().equals(bpmnProcessId)) {
                        return false;
                    }

                    final var element = task.getBpmnModelElementInstance();
                    if (element == null) {
                        return false;
                    }

                    return connectable.applies(
                            element.getId(),
                            ((UserTask) element).getCamundaFormKey());
                })
                // found handler-reference
                .peek(entry -> {
                    connectableFound[0] = entry.getKey();
                })
                .findFirst()
                .map(Map.Entry::getValue)
                .map(action)
                .orElseGet(() -> {
                        logger.debug(
                                "Unmapped event '{}'! "
                                        + "If you need to process this event add a parameter "
                                        + "'@TaskEvent Event event' to the method annotated by "
                                        + "'@UserTaskDetailsProvider(taskDefinition = \"{}\") in any class "
                                        + "annotated by '@WorkflowService(bpmnProcess = @BpmnProcess(bpmnProcessId = \"{}\"))'.",
                                task.getEventName(),
                                connectableFound[0].getTaskDefinition(),
                                connectableFound[0].getBpmnProcessId());
                        return null;
                });

    }
    
}
