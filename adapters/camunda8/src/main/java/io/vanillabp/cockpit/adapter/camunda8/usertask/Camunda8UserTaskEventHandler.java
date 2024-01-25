package io.vanillabp.cockpit.adapter.camunda8.usertask;

import io.vanillabp.cockpit.adapter.camunda8.wiring.Camunda8UserTaskConnectable;
import io.zeebe.exporter.proto.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class Camunda8UserTaskEventHandler{

    private static final Logger logger = LoggerFactory
            .getLogger(Camunda8UserTaskEventHandler.class);
    
    private final Map<Camunda8UserTaskConnectable, Camunda8UserTaskHandler> taskHandlers;

    public Camunda8UserTaskEventHandler() {
        this.taskHandlers = new HashMap<>();
    }

    public void addTaskHandler(
            final Camunda8UserTaskConnectable connectable,
            final Camunda8UserTaskHandler taskHandler) {
        
        taskHandlers.put(connectable, taskHandler);
        
    }
    
    public void notify(
            final Schema.JobRecord task) {

        final var connectableFound = new Camunda8UserTaskConnectable[1];
        taskHandlers
                .entrySet()
                .stream()
                // TODO use correct connectible
//                .filter(entry -> {
//                    final var connectable = entry.getKey();
//
//                    if (!connectable.getBpmnProcessId().equals(bpmnProcessId)) {
//                        return false;
//                    }
//
//                    final var element = execution.getBpmnModelElementInstance();
//                    if (element == null) {
//                        return false;
//                    }
//
//                    return connectable.applies(
//                            element.getId(),
//                            ((UserTask) element).getCamundaFormKey());
//                })
                // found handler-reference
                .findFirst()
                .map(Map.Entry::getValue)
                .ifPresentOrElse(
                        handler -> handler.notify(task),
                        () -> logger.debug(
                                "Unmapped event '{}'! "
                                + "If you need to process this event add a parameter "
                                + "'@TaskEvent Event event' to the method annotated by "
                                + "'@UserTaskDetailsProvider(taskDefinition = \"{}\") in any class "
                                + "annotated by '@WorkflowService(bpmnProcess = @BpmnProcess(bpmnProcessId = \"{}\"))'.",
                                task.getElementId(),
                                connectableFound[0].getTaskDefinition(),
                                connectableFound[0].getBpmnProcessId()));

    }
}
