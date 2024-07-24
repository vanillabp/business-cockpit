package io.vanillabp.cockpit.adapter.camunda8.service;

import io.vanillabp.cockpit.adapter.camunda8.Camunda8AdapterConfiguration;
import io.vanillabp.cockpit.adapter.camunda8.workflow.Camunda8WorkflowEventHandler;
import io.vanillabp.cockpit.adapter.camunda8.workflow.persistence.ProcessInstanceMapper;
import io.vanillabp.cockpit.adapter.camunda8.workflow.persistence.ProcessInstanceRepository;
import io.vanillabp.cockpit.adapter.common.service.AdapterAwareBusinessCockpitService;
import io.vanillabp.cockpit.adapter.common.service.BusinessCockpitServiceImplementation;
import io.vanillabp.spi.cockpit.usertask.UserTask;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.function.Function;

public class Camunda8BusinessCockpitService <WA> implements BusinessCockpitServiceImplementation<WA> {

    private final CrudRepository<WA, Object> workflowAggregateRepository;

    private final Class<WA> workflowAggregateClass;

    private final Function<WA, ?> getWorkflowAggregateId;

    private final Function<String, Object> parseWorkflowAggregateIdFromBusinessKey;

    private final String workflowAggregateIdName;

    private AdapterAwareBusinessCockpitService<WA> parent;

    private ProcessInstanceRepository processInstanceRepository;

    private Camunda8WorkflowEventHandler camunda8WorkflowEventHandler;

    public Camunda8BusinessCockpitService(CrudRepository<WA, Object> workflowAggregateRepository,
                                          Class<WA> workflowAggregateClass,
                                          Function<WA, ?> getWorkflowAggregateId,
                                          Function<String, Object> parseWorkflowAggregateIdFromBusinessKey,
                                          String workflowAggregateIdName,
                                          ProcessInstanceRepository processInstanceRepository,
                                          Camunda8WorkflowEventHandler workflowEventHandler) {

        this.workflowAggregateRepository = workflowAggregateRepository;
        this.workflowAggregateClass = workflowAggregateClass;
        this.getWorkflowAggregateId = getWorkflowAggregateId;
        this.parseWorkflowAggregateIdFromBusinessKey = parseWorkflowAggregateIdFromBusinessKey;
        this.workflowAggregateIdName = workflowAggregateIdName;
        this.processInstanceRepository = processInstanceRepository;
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

        this.processInstanceRepository
                .findProcessInstanceEntityByBusinessKey(businessKey)
                .stream()
                .map(ProcessInstanceMapper::map)
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
