package io.vanillabp.cockpit.adapter.camunda8.service;

import io.vanillabp.cockpit.adapter.camunda8.Camunda8AdapterConfiguration;
import io.vanillabp.cockpit.adapter.camunda8.deployments.ProcessInstancePersistence;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowCreatedEvent;
import io.vanillabp.cockpit.adapter.camunda8.workflow.Camunda8WorkflowEventHandler;
import io.vanillabp.cockpit.adapter.common.service.AdapterAwareBusinessCockpitService;
import io.vanillabp.cockpit.adapter.common.service.BusinessCockpitServiceImplementation;
import io.vanillabp.spi.cockpit.usertask.UserTask;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.data.repository.CrudRepository;

public class Camunda8BusinessCockpitService <WA> implements BusinessCockpitServiceImplementation<WA> {

    private final CrudRepository<WA, Object> workflowAggregateRepository;

    private final Class<WA> workflowAggregateClass;

    private final Function<WA, ?> getWorkflowAggregateId;

    private final Function<String, Object> parseWorkflowAggregateIdFromBusinessKey;

    private final String workflowAggregateIdName;

    private AdapterAwareBusinessCockpitService<WA> parent;

    private ProcessInstancePersistence processInstancePersistence;

    private Camunda8WorkflowEventHandler camunda8WorkflowEventHandler;

    public Camunda8BusinessCockpitService(CrudRepository<WA, Object> workflowAggregateRepository,
                                          Class<WA> workflowAggregateClass,
                                          Function<WA, ?> getWorkflowAggregateId,
                                          Function<String, Object> parseWorkflowAggregateIdFromBusinessKey,
                                          String workflowAggregateIdName,
                                          ProcessInstancePersistence processInstancePersistence,
                                          Camunda8WorkflowEventHandler workflowEventHandler) {

        this.workflowAggregateRepository = workflowAggregateRepository;
        this.workflowAggregateClass = workflowAggregateClass;
        this.getWorkflowAggregateId = getWorkflowAggregateId;
        this.parseWorkflowAggregateIdFromBusinessKey = parseWorkflowAggregateIdFromBusinessKey;
        this.workflowAggregateIdName = workflowAggregateIdName;
        this.processInstancePersistence = processInstancePersistence;
        this.camunda8WorkflowEventHandler = workflowEventHandler;
    }

    public void wire(
            final String workflowModuleId,
            final String bpmnProcessId,
            boolean isPrimary) {

        if (parent == null) {
            throw new RuntimeException("Not yet wired! If this occurs Spring Boot dependency of either "
                    + "VanillaBP Spring Boot support or Camunda8 adapter was changed introducing this "
                    + "lack of wiring. Please report a Github issue!");

        }

        parent.wire(
                Camunda8AdapterConfiguration.ADAPTER_ID,
                workflowModuleId,
                bpmnProcessId,
                isPrimary);
    }


    @Override
    public Class<WA> getWorkflowAggregateClass() {
        return workflowAggregateClass;
    }

    @Override
    public CrudRepository<WA, ?> getWorkflowAggregateRepository() {
        return workflowAggregateRepository;
    }

    @Override
    public void setParent(AdapterAwareBusinessCockpitService<WA> parent) {
        this.parent = parent;
    }

    @Override
    public void aggregateChanged(WA workflowAggregate) {
        String businessKey = getWorkflowAggregateId.apply(workflowAggregate).toString();

        this.processInstancePersistence
                .findByBusinessKey(businessKey)
                .stream()
                .map(processInstance -> {
                    final var camunda8WorkflowCreatedEvent = new Camunda8WorkflowCreatedEvent();
                    camunda8WorkflowCreatedEvent.setProcessInstanceKey(processInstance.getProcessInstanceKey());

                    if(processInstance.getProcessDefinitionKey() != null){
                        camunda8WorkflowCreatedEvent.setProcessDefinitionKey(processInstance.getProcessDefinitionKey());
                    }
                    camunda8WorkflowCreatedEvent.setBpmnProcessId(processInstance.getBpmnProcessId());

                    if(processInstance.getVersion() != null){
                        camunda8WorkflowCreatedEvent.setVersion(processInstance.getVersion());
                    }
                    camunda8WorkflowCreatedEvent.setTenantId(processInstance.getTenantId());
                    camunda8WorkflowCreatedEvent.setBusinessKey(processInstance.getBusinessKey());
                    return camunda8WorkflowCreatedEvent;
                })
                .forEach(camunda8WorkflowEventHandler::processWorkflowUpdateEvent);
    }

    @Override
    public void aggregateChanged(WA workflowAggregate, String... userTaskIds) {
        // TODO CKO
    }

    @Override
    public Optional<UserTask> getUserTask(WA workflowAggregate, String userTaskId) {
        // TODO CKO
        return Optional.empty();
    }

    public String getWorkflowAggregateIdName() {
        return workflowAggregateIdName;
    }
}
