package io.vanillabp.cockpit.adapter.camunda8.service;

import io.camunda.client.CamundaClient;
import io.vanillabp.cockpit.adapter.camunda8.Camunda8AdapterConfiguration;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8UserTaskEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowEvent;
import io.vanillabp.cockpit.adapter.camunda8.usertask.Camunda8UserTaskEventHandler;
import io.vanillabp.cockpit.adapter.camunda8.usertask.Camunda8UserTaskWiring;
import io.vanillabp.cockpit.adapter.camunda8.workflow.Camunda8WorkflowEventHandler;
import io.vanillabp.cockpit.adapter.common.service.AdapterAwareBusinessCockpitService;
import io.vanillabp.cockpit.adapter.common.service.BusinessCockpitServiceImplementation;
import io.vanillabp.spi.cockpit.details.DetailsEvent;
import io.vanillabp.spi.cockpit.usertask.UserTask;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.repository.CrudRepository;

public class Camunda8BusinessCockpitService<WA> implements BusinessCockpitServiceImplementation<WA> {

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

    private final Camunda8UserTaskEventHandler camunda8UserTaskEventHandler;

    private CamundaClient client;

    private final ApplicationEventPublisher applicationEventPublisher;

    public Camunda8BusinessCockpitService(CrudRepository<WA, Object> workflowAggregateRepository,
                                          Class<WA> workflowAggregateClass,
                                          Function<WA, ?> getWorkflowAggregateId,
                                          Function<String, Object> parseWorkflowAggregateIdFromBusinessKey,
                                          String workflowAggregateIdName,
                                          ApplicationEventPublisher applicationEventPublisher,
                                          Camunda8WorkflowEventHandler workflowEventHandler,
                                          Camunda8UserTaskEventHandler userTaskEventHandler) {

        this.workflowAggregateRepository = workflowAggregateRepository;
        this.workflowAggregateClass = workflowAggregateClass;
        this.getWorkflowAggregateId = getWorkflowAggregateId;
        this.parseWorkflowAggregateIdFromBusinessKey = parseWorkflowAggregateIdFromBusinessKey;
        this.workflowAggregateIdName = workflowAggregateIdName;
        this.camunda8WorkflowEventHandler = workflowEventHandler;
        this.camunda8UserTaskEventHandler = userTaskEventHandler;
        this.applicationEventPublisher = applicationEventPublisher;

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
    public void aggregateChanged(
            final WA workflowAggregate) {

        final var businessKey = getWorkflowAggregateId.apply(workflowAggregate);

        final var processesFound = client
                .newProcessInstanceSearchRequest()
                .filter(filter -> {
                    filter.processDefinitionId(bpmnProcessId);
                    filter.variables(
                            Map.of(getWorkflowAggregateIdName(), "\"" + businessKey + "\""));
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
                    final var event = new Camunda8WorkflowEvent();
                    event.setEvent(DetailsEvent.Event.UPDATED);
                    event.setTimestamp(OffsetDateTime.now());
                    event.setBpmnProcessId(processInstance.getProcessDefinitionId());
                    event.setProcessDefinitionKey(processInstance.getProcessDefinitionKey());
                    event.setProcessInstanceKey(processInstance.getProcessInstanceKey());
                    event.setProcessDefinitionVersion(processInstance.getProcessDefinitionVersion());
                    event.setTenantId(processInstance.getTenantId());
                    event.setVariables(Map.of(workflowAggregateIdName, businessKey));
                    return event;
                })
                .forEach(applicationEventPublisher::publishEvent);

    }

    @Override
    public void aggregateChanged(
            final WA workflowAggregate,
            final String... userTaskIds) {

        final var businessKey = getWorkflowAggregateId.apply(workflowAggregate);

        final var userTaskIdsConverted = Arrays
                .stream(userTaskIds)
                .map(Long::parseLong)
                .toList();

        final var userTasksFound = new HashSet<io.camunda.client.api.search.response.UserTask>();
        boolean nextPage = true;
        while (nextPage) {
            final var result = client
                    .newUserTaskSearchRequest()
                    .filter(filter -> {
                        filter.bpmnProcessId(bpmnProcessId);
                        /*
                        filter.processInstanceVariables(
                                Map.of(idName, businessKey));
                         */
                        if (tenantId != null) {
                            filter.tenantId(tenantId);
                        }
                    })
                    .page(request -> request.limit(100))
                    .execute();
            result
                    .items()
                    .stream()
                    .filter(userTask -> userTaskIdsConverted.contains(userTask.getUserTaskKey()))
                    .forEach(userTasksFound::add);
            if ((userTasksFound.size() == userTaskIds.length)
                    || !result.page().hasMoreTotalItems()) {
                nextPage = false;
            }
        }
        if (userTasksFound.isEmpty()) {
            if (tenantId == null) {
                logger.warn("Could not found user tasks {} for business key '{}', BPMN process ID '{}'!",
                        userTaskIdsConverted, businessKey, bpmnProcessId);
            } else {
                logger.warn("Could not found user tasks {} for business key '{}', BPMN process ID '{}', tenant '{}'!",
                        userTaskIdsConverted, businessKey, bpmnProcessId, tenantId);
            }
            return;
        }

        userTasksFound
                .stream()
                .map(userTask -> {
                    final var event = new Camunda8UserTaskEvent();
                    event.setEvent(DetailsEvent.Event.UPDATED);
                    event.setTimestamp(OffsetDateTime.now());
                    event.setBpmnProcessId(userTask.getBpmnProcessId());
                    event.setElementId(userTask.getElementId());
                    event.setProcessDefinitionKey(userTask.getProcessDefinitionKey());
                    event.setProcessInstanceKey(userTask.getProcessInstanceKey());
                    event.setProcessDefinitionVersion(userTask.getProcessDefinitionVersion());
                    event.setTenantId(userTask.getTenantId());
                    event.setUserTaskKey(userTask.getUserTaskKey());
                    // TODO
                    //event.setFollowUpDate(job.getUserTask().getFollowUpDate());
                    event.setAssignee(userTask.getAssignee());
                    event.setCandidateUsers(userTask.getCandidateUsers());
                    event.setCandidateGroups(userTask.getCandidateGroups());
                    // TODO
                    //event.setDueDate(job.getUserTask().getDueDate());
                    event.setVariables(Map.of(workflowAggregateIdName, businessKey));

                    final var formKey = userTask.getExternalFormReference();
                    final var taskDefinition = formKey.startsWith(Camunda8UserTaskWiring.JOBTYPE_DETAILSPROVIDER)
                            ? formKey.substring(Camunda8UserTaskWiring.JOBTYPE_DETAILSPROVIDER.length())
                            : formKey;
                    event.setTaskDefinition(taskDefinition);
                    return event;
                })
                .forEach(applicationEventPublisher::publishEvent);

    }

    @Override
    public Optional<UserTask> getUserTask(
            final WA workflowAggregate,
            final String userTaskId) {

        final var userTask = client
                .newUserTaskGetRequest(Long.parseLong(userTaskId))
                .execute();
        if (userTask == null) {
            return Optional.empty();
        }

        final var event = new Camunda8UserTaskEvent();
        event.setUserTaskKey(userTask.getUserTaskKey());
        event.setTimestamp(OffsetDateTime.now());
        event.setProcessInstanceKey(userTask.getProcessInstanceKey());
        event.setProcessDefinitionKey(userTask.getProcessDefinitionKey());
        event.setProcessDefinitionVersion(userTask.getProcessDefinitionVersion());
        event.setBpmnProcessId(userTask.getBpmnProcessId());
        event.setTenantId(userTask.getTenantId());
        event.setElementId(userTask.getElementId());
        event.setEvent(DetailsEvent.Event.UPDATED);
        event.setVariables(Map.of(
                workflowAggregateIdName, getWorkflowAggregateId.apply(workflowAggregate)));

        event.setAssignee(userTask.getAssignee());
        event.setCandidateGroups(userTask.getCandidateGroups());
        event.setCandidateUsers(userTask.getCandidateUsers());
        event.setDueDate(userTask.getDueDate());
        event.setFollowUpDate(userTask.getFollowUpDate());

        final var taskDefinition = userTask.getExternalFormReference().startsWith(Camunda8UserTaskWiring.JOBTYPE_DETAILSPROVIDER)
                ? userTask.getExternalFormReference().substring(Camunda8UserTaskWiring.JOBTYPE_DETAILSPROVIDER.length())
                : userTask.getExternalFormReference();
        event.setTaskDefinition(taskDefinition);

        return Optional.of(new UserTaskImpl(camunda8UserTaskEventHandler.getUserTaskEvent(event, workflowAggregate)));

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
