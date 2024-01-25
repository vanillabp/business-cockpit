package io.vanillabp.cockpit.adapter.camunda8.service;

import io.vanillabp.cockpit.adapter.camunda8.Camunda8AdapterConfiguration;
import io.vanillabp.cockpit.adapter.common.service.AdapterAwareBusinessCockpitService;
import io.vanillabp.cockpit.adapter.common.service.BusinessCockpitServiceImplementation;
import org.springframework.data.repository.CrudRepository;

import java.util.function.Function;

public class Camunda8BusinessCockpitService <WA> implements BusinessCockpitServiceImplementation<WA> {

    private final CrudRepository<WA, Object> workflowAggregateRepository;

    private final Class<WA> workflowAggregateClass;

    private final Function<WA, ?> getWorkflowAggregateId;

    private final Function<String, Object> parseWorkflowAggregateIdFromBusinessKey;

    private AdapterAwareBusinessCockpitService<WA> parent;

    public Camunda8BusinessCockpitService(CrudRepository<WA, Object> workflowAggregateRepository,
                                          Class<WA> workflowAggregateClass,
                                          Function<WA, ?> getWorkflowAggregateId,
                                          Function<String, Object> parseWorkflowAggregateIdFromBusinessKey) {

        this.workflowAggregateRepository = workflowAggregateRepository;
        this.workflowAggregateClass = workflowAggregateClass;
        this.getWorkflowAggregateId = getWorkflowAggregateId;
        this.parseWorkflowAggregateIdFromBusinessKey = parseWorkflowAggregateIdFromBusinessKey;
    }


    public void wire(
            final String workflowModuleId,
            final String bpmnProcessId) {

        if (parent == null) {
            throw new RuntimeException("Not yet wired! If this occurs Spring Boot dependency of either "
                    + "VanillaBP Spring Boot support or Camunda7 adapter was changed introducing this "
                    + "lack of wiring. Please report a Github issue!");

        }

        parent.wire(
                Camunda8AdapterConfiguration.ADAPTER_ID,
                workflowModuleId,
                bpmnProcessId);
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

    }

    @Override
    public void aggregateChanged(WA workflowAggregate, String... userTaskIds) {

    }
}
