package io.vanillabp.cockpit.adapter.camunda8.service;

import io.camunda.client.CamundaClient;
import io.vanillabp.cockpit.adapter.camunda8.Camunda8AdapterConfiguration;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowCreatedEvent;
import io.vanillabp.cockpit.adapter.camunda8.workflow.Camunda8WorkflowEventHandler;
import io.vanillabp.cockpit.adapter.common.service.AdapterAwareBusinessCockpitService;
import io.vanillabp.cockpit.adapter.common.service.BusinessCockpitServiceImplementation;
import io.vanillabp.spi.cockpit.usertask.UserTask;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.CrudRepository;

public class Camunda8BusinessCockpitService <WA> implements BusinessCockpitServiceImplementation<WA> {

    private static final Logger logger = LoggerFactory.getLogger(Camunda8BusinessCockpitService.class);

    private final CrudRepository<WA, Object> workflowAggregateRepository;

    private final Class<WA> workflowAggregateClass;

    private final Function<WA, ?> getWorkflowAggregateId;

    private final Function<String, Object> parseWorkflowAggregateIdFromBusinessKey;

    private final String workflowAggregateIdName;

    private AdapterAwareBusinessCockpitService<WA> parent;

    private String tenantId;

    private String bpmnProcessId;

    private final Camunda8WorkflowEventHandler camunda8WorkflowEventHandler;

    private CamundaClient client;

    public Camunda8BusinessCockpitService(CrudRepository<WA, Object> workflowAggregateRepository,
                                          Class<WA> workflowAggregateClass,
                                          Function<WA, ?> getWorkflowAggregateId,
                                          Function<String, Object> parseWorkflowAggregateIdFromBusinessKey,
                                          String workflowAggregateIdName,
                                          Camunda8WorkflowEventHandler workflowEventHandler) {

        this.workflowAggregateRepository = workflowAggregateRepository;
        this.workflowAggregateClass = workflowAggregateClass;
        this.getWorkflowAggregateId = getWorkflowAggregateId;
        this.parseWorkflowAggregateIdFromBusinessKey = parseWorkflowAggregateIdFromBusinessKey;
        this.workflowAggregateIdName = workflowAggregateIdName;
        this.camunda8WorkflowEventHandler = workflowEventHandler;

    }

    public void wire(
            final CamundaClient client,
            final String workflowModuleId,
            final String bpmnProcessId,
            boolean isPrimary) {

        if (parent == null) {
            throw new RuntimeException("Not yet wired! If this occurs Spring Boot dependency of either "
                    + "VanillaBP Spring Boot support or Camunda8 adapter was changed introducing this "
                    + "lack of wiring. Please report a Github issue!");

        }

        this.client = client;
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

        final var businessKey = getWorkflowAggregateId.apply(workflowAggregate);

        final var processesFound = client
                .newProcessInstanceSearchRequest()
                .filter(filter -> {
                    filter.processDefinitionId(bpmnProcessId);
                    filter.variables(
                            Map.of(getWorkflowAggregateIdName(), businessKey));
                    if (tenantId != null) {
                        filter.tenantId(tenantId);
                    }
                })
                .send()
                .join()
                .items();
        if (processesFound.isEmpty()) {
            if (tenantId == null) {
                logger.warn("Could not found process instance for business key '{}', BPMN process ID '{}'!",
                        businessKey, bpmnProcessId);
            } else {
                logger.warn("Could not found process instance for business key '{}', BPMN process ID '{}', tenant '{}'!",
                        businessKey, bpmnProcessId, tenantId);
            }
            return;
        }

        processesFound
                .stream()
                .map(processInstance -> {
                    final var camunda8WorkflowCreatedEvent = new Camunda8WorkflowCreatedEvent();
                    camunda8WorkflowCreatedEvent.setProcessInstanceKey(processInstance.getProcessInstanceKey());

                    if(processInstance.getProcessDefinitionKey() != null){
                        camunda8WorkflowCreatedEvent.setProcessDefinitionKey(processInstance.getProcessDefinitionKey());
                    }
                    camunda8WorkflowCreatedEvent.setBpmnProcessId(processInstance.getProcessDefinitionId());

                    final var versionTag = processInstance.getProcessDefinitionVersionTag();
                    final var version = versionTag != null
                            ? "%s:%d".formatted(versionTag, processInstance.getProcessDefinitionVersion())
                            : "%d".formatted(processInstance.getProcessDefinitionVersion());
                    camunda8WorkflowCreatedEvent.setVersion(version);

                    camunda8WorkflowCreatedEvent.setTenantId(processInstance.getTenantId());
                    camunda8WorkflowCreatedEvent.setBusinessKey(businessKey.toString());
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

    public void setBpmnProcessId(
            final String primaryBpmnProcessId) {
        this.bpmnProcessId = primaryBpmnProcessId;
    }

    public void setTenantId(
            final String tenantId) {
        this.tenantId = tenantId;
    }

}
