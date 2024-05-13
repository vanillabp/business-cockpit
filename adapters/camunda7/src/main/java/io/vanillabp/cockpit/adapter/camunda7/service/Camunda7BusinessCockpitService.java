package io.vanillabp.cockpit.adapter.camunda7.service;

import io.vanillabp.cockpit.adapter.camunda7.Camunda7AdapterConfiguration;
import io.vanillabp.cockpit.adapter.camunda7.usertask.Camunda7UserTaskEventHandler;
import io.vanillabp.cockpit.adapter.camunda7.workflow.Camunda7WorkflowEventHandler;
import io.vanillabp.cockpit.adapter.common.service.AdapterAwareBusinessCockpitService;
import io.vanillabp.cockpit.adapter.common.service.BusinessCockpitServiceImplementation;
import io.vanillabp.spi.cockpit.usertask.UserTask;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.function.Function;

public class Camunda7BusinessCockpitService<WA> implements BusinessCockpitServiceImplementation<WA> {
    
    private final CrudRepository<WA, Object> workflowAggregateRepository;
    
    private final Class<WA> workflowAggregateClass;
    
    private final Function<WA, ?> getWorkflowAggregateId;
    
    private final Function<String, Object> parseWorkflowAggregateIdFromBusinessKey;

    private AdapterAwareBusinessCockpitService<WA> parent;
    
    private final Camunda7UserTaskEventHandler userTaskEventHandler;
    
    private final TaskService taskService;
    
    private final RuntimeService runtimeService;

    private final ProcessEngine processEngine;
    
    private final Camunda7WorkflowEventHandler workflowEventHandler;
    
    public Camunda7BusinessCockpitService(
            final ProcessEngine processEngine,
            final TaskService taskService,
            final RuntimeService runtimeService,
            final Function<WA, ?> getWorkflowAggregateId,
            final CrudRepository<WA, Object> workflowAggregateRepository,
            final Class<WA> workflowAggregateClass,
            final Function<String, Object> parseWorkflowAggregateIdFromBusinessKey,
            final Camunda7UserTaskEventHandler userTaskEventHandler,
            final Camunda7WorkflowEventHandler workflowEventHandler) {

        this.workflowAggregateRepository = workflowAggregateRepository;
        this.workflowAggregateClass = workflowAggregateClass;
        this.getWorkflowAggregateId = getWorkflowAggregateId;
        this.parseWorkflowAggregateIdFromBusinessKey = parseWorkflowAggregateIdFromBusinessKey;
        this.userTaskEventHandler = userTaskEventHandler;
        this.processEngine = processEngine;
        this.taskService = taskService;
        this.runtimeService = runtimeService;
        this.workflowEventHandler = workflowEventHandler;

    }

    public void wire(
            final String workflowModuleId,
            final String bpmnProcessId,
            final boolean isPrimary) {

        if (parent == null) {
            throw new RuntimeException("Not yet wired! If this occurs Spring Boot dependency of either "
                    + "VanillaBP Spring Boot support or Camunda7 adapter was changed introducing this "
                    + "lack of wiring. Please report a Github issue!");
            
        }

        parent.wire(
                Camunda7AdapterConfiguration.ADAPTER_ID,
                workflowModuleId,
                bpmnProcessId,
                isPrimary);
        
    }
    
    @Override
    public void setParent(AdapterAwareBusinessCockpitService<WA> parent) {
        
        this.parent = parent;
        
    }
    
    @Override
    public Class<WA> getWorkflowAggregateClass() {

        return workflowAggregateClass;

    }

    @Override
    public CrudRepository<WA, Object> getWorkflowAggregateRepository() {

        return workflowAggregateRepository;

    }
    
    public Object getWorkflowAggregateIdFromBusinessKey(
            final String businessKey) {
        
        return parseWorkflowAggregateIdFromBusinessKey.apply(businessKey);
        
    }

    @Override
    public void aggregateChanged(
            final WA workflowAggregate) {
        
        runtimeService
                .createProcessInstanceQuery()
                .processInstanceBusinessKey(getWorkflowAggregateId.apply(workflowAggregate).toString())
                .list()
                .stream()
                .filter(pi -> (pi.getRootProcessInstanceId() == null) || pi.getRootProcessInstanceId().equals(pi.getId()))
                .map(ProcessInstance::getId)
                .forEach(workflowEventHandler::triggerEventAfterTransaction);
        
    }

    @Override
    public void aggregateChanged(
            final WA workflowAggregate,
            final String... userTaskIds) {

        ((ProcessEngineConfigurationImpl) processEngine
                .getProcessEngineConfiguration())
                .getCommandExecutorTxRequired()
                .<Void>execute(commandContext -> {
                    taskService
                            .createTaskQuery()
                            .taskIdIn(userTaskIds)
                            .list()
                            .forEach(task -> userTaskEventHandler.notify(
                                    (TaskEntity) task,
                                    TaskListener.EVENTNAME_UPDATE)
                            );
                    return null;
                });

    }

    @Override
    public Optional<UserTask> getUserTask(
            final WA workflowAggregate,
            final String userTaskId) {

        return ((ProcessEngineConfigurationImpl) processEngine
                .getProcessEngineConfiguration())
                .getCommandExecutorTxRequired()
                .execute(commandContext -> Optional
                            .ofNullable(
                                    taskService
                                            .createTaskQuery()
                                            .taskId(userTaskId)
                                            .singleResult())
                        .map(task -> (TaskEntity) task)
                        .map(userTaskEventHandler::getUserTask));

    }

}
