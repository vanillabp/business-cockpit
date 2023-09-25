package io.vanillabp.cockpit.adapter.camunda7.wiring;

import java.util.LinkedList;
import java.util.List;

import io.vanillabp.cockpit.adapter.camunda7.workflow.Camunda7WorkflowWiring;
import org.camunda.bpm.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;
import org.camunda.bpm.engine.impl.task.TaskDefinition;
import org.camunda.bpm.engine.impl.util.xml.Element;

import io.vanillabp.cockpit.adapter.camunda7.usertask.Camunda7Connectable;
import io.vanillabp.cockpit.adapter.camunda7.usertask.Camunda7UserTaskEventHandler;
import io.vanillabp.cockpit.adapter.camunda7.usertask.Camunda7UserTaskWiring;

public class WiringBpmnParseListener extends AbstractBpmnParseListener {
    
    static final ThreadLocal<String> workflowModuleId = new ThreadLocal<>();
    
    private List<Camunda7Connectable> connectables = new LinkedList<>();

    private List<ToBeWired> toBeWired = new LinkedList<>();
    
    private final Camunda7UserTaskWiring userTaskWiring;

    private final Camunda7WorkflowWiring workflowWiring;
    
    private final boolean userTasksEnabled;
    
    private final Camunda7UserTaskEventHandler userTaskEventHandler;

    static class ToBeWired {
        String workflowModuleId;
        String bpmnProcessId;
        List<Camunda7Connectable> connectables;
    };
    
    public WiringBpmnParseListener(
            final boolean userTasksEnabled,
            final Camunda7UserTaskWiring userTaskWiring,
            final Camunda7UserTaskEventHandler userTaskEventHandler,
            final Camunda7WorkflowWiring workflowWiring) {

        this.userTasksEnabled = userTasksEnabled;
        this.userTaskWiring = userTaskWiring;
        this.userTaskEventHandler = userTaskEventHandler;
        this.workflowWiring = workflowWiring;
        
    }

    @Override
    public void parseProcess(
            final Element processElement,
            final ProcessDefinitionEntity processDefinition) {
        
        final var process = new ToBeWired();
        process.bpmnProcessId = processDefinition.getKey();
        process.workflowModuleId = workflowModuleId.get();
        process.connectables = connectables;
        toBeWired.add(process);

        connectables = new LinkedList<>();
        
    }
    
    @Override
    public void parseUserTask(
            final Element userTaskElement,
            final ScopeImpl scope,
            final ActivityImpl activity) {

        if (!userTasksEnabled) {
            return;
        }
        
        final var taskDefinition = getTaskDefinition(activity);

        taskDefinition.addBuiltInTaskListener(
                org.camunda.bpm.engine.delegate.TaskListener.EVENTNAME_CREATE,
                userTaskEventHandler);
        taskDefinition.addBuiltInTaskListener(
                org.camunda.bpm.engine.delegate.TaskListener.EVENTNAME_DELETE,
                userTaskEventHandler);
        taskDefinition.addBuiltInTaskListener(
                org.camunda.bpm.engine.delegate.TaskListener.EVENTNAME_COMPLETE,
                userTaskEventHandler);
        taskDefinition.addBuiltInTaskListener(
                org.camunda.bpm.engine.delegate.TaskListener.EVENTNAME_UPDATE,
                userTaskEventHandler);
        
        final var bpmnProcessId = ((ProcessDefinitionEntity) activity.getProcessDefinition()).getKey();

        final var connectable = new Camunda7Connectable(
                bpmnProcessId,
                activity.getId(),
                taskDefinition.getFormKey() != null ? taskDefinition.getFormKey().getExpressionText() : null);
        
        connectables.add(connectable);
        
    }
    
    @Override
    public void parseRootElement(
            final Element rootElement,
            final List<ProcessDefinitionEntity> processDefinitions) {
        
        toBeWired
                .forEach(tbw -> {
                    if (!userTasksEnabled) {
                        return;
                    }
                    tbw.connectables
                            .forEach(connectable -> userTaskWiring.wireTask(tbw.workflowModuleId, connectable));
                });

        toBeWired
                .forEach(tbw -> {
                    workflowWiring.wireService(
                            tbw.workflowModuleId,
                            tbw.bpmnProcessId);
                    
                    workflowWiring.wireWorkflow(
                            tbw.workflowModuleId,
                            tbw.bpmnProcessId);
                });

    }
    
    /**
     * Retrieves task definition.
     *
     * @param activity the taskActivity
     * @return taskDefinition for activity
     */
    private TaskDefinition getTaskDefinition(
            final ActivityImpl activity) {
        
        final UserTaskActivityBehavior activityBehavior = (UserTaskActivityBehavior) activity.getActivityBehavior();
        return activityBehavior.getTaskDefinition();
        
    }

}
