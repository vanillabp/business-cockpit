package io.vanillabp.cockpit.adapter.camunda8.service;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.search.enums.ProcessInstanceState;
import io.camunda.client.api.search.response.ProcessInstance;
import io.vanillabp.cockpit.adapter.camunda8.Camunda8AdapterConfiguration;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8AggregateChangedEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8UserTaskEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowEvent;
import io.vanillabp.cockpit.adapter.camunda8.usertask.Camunda8UserTaskEventHandler;
import io.vanillabp.cockpit.adapter.camunda8.usertask.Camunda8UserTaskWiring;
import io.vanillabp.cockpit.adapter.camunda8.workflow.Camunda8WorkflowEventHandler;
import io.vanillabp.cockpit.adapter.common.service.AdapterAwareBusinessCockpitService;
import io.vanillabp.cockpit.adapter.common.service.BusinessCockpitServiceImplementation;
import io.vanillabp.spi.cockpit.details.DetailsEvent;
import io.vanillabp.spi.cockpit.usertask.UserTask;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.repository.CrudRepository;

public class Camunda8BusinessCockpitService<WA> implements BusinessCockpitServiceImplementation<WA> {

    private static final Logger logger = LoggerFactory.getLogger(Camunda8BusinessCockpitService.class);

    /**
     * How long to leave the cluster alone between two attempts to find a workflow which its
     * secondary storage does not know yet. Short enough that a cockpit event is not noticeably late,
     * long enough that waiting out the default visibility timeout costs a handful of requests
     * instead of a flood.
     */
    private static final Duration TIME_BETWEEN_TWO_ATTEMPTS = Duration.ofMillis(250);

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

    private Duration workflowVisibilityTimeout = Duration.ofSeconds(10);

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

        announceTheChangeAndLookItUpAfterTheCommit(
                () -> notifyTheCockpitAboutTheWorkflowsOf(businessKey));

    }

    private void notifyTheCockpitAboutTheWorkflowsOf(
            final Object businessKey) {

        final var processesFound = awaitRunningWorkflows(businessKey);
        if (processesFound.isEmpty()) {
            reportTheCockpitIsLaggingBehind(businessKey);
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

    /**
     * Keeps asking the secondary storage of Camunda 8 for the running workflows of this aggregate
     * while it reports none, until {@link #workflowVisibilityTimeout} is used up. An aggregate is
     * most often reported as changed right after its workflow was started, which is exactly the
     * moment the exporter has not written that workflow yet, so the first answer being empty says
     * "not yet" far more often than it says "there is none".
     */
    private List<ProcessInstance> awaitRunningWorkflows(
            final Object businessKey) {

        final var giveUpAt = System.nanoTime() + workflowVisibilityTimeout.toNanos();
        while (true) {
            final var processesFound = searchRunningWorkflows(businessKey);
            if (!processesFound.isEmpty()) {
                return processesFound;
            }
            if (!waitBeforeAskingAgain(giveUpAt)) {
                return List.of();
            }
        }

    }

    private List<ProcessInstance> searchRunningWorkflows(
            final Object businessKey) {

        return client
                .newProcessInstanceSearchRequest()
                .filter(filter -> {
                    filter.processDefinitionId(bpmnProcessId);
                    // completed and terminated instances are still kept in secondary storage and
                    // would be reported as additional workflows sharing the same business key
                    filter.state(ProcessInstanceState.ACTIVE);
                    filter.variables(
                            Map.of(getWorkflowAggregateIdName(), "\"" + businessKey + "\""));
                    if (tenantId != null) {
                        filter.tenantId(tenantId);
                    }
                })
                .send()
                .join()
                .items()
                .stream()
                // call-activity children inherit the business key, only the root is a workflow
                .filter(processInstance -> processInstance.getParentProcessInstanceKey() == null)
                .toList();

    }

    @Override
    public void aggregateChanged(
            final WA workflowAggregate,
            final String... userTaskIds) {

        final var businessKey = getWorkflowAggregateId.apply(workflowAggregate);

        final var userTaskKeys = Arrays
                .stream(userTaskIds)
                .map(Long::parseLong)
                .toList();

        announceTheChangeAndLookItUpAfterTheCommit(
                () -> notifyTheCockpitAboutTheUserTasksOf(businessKey, userTaskKeys));

    }

    private void notifyTheCockpitAboutTheUserTasksOf(
            final Object businessKey,
            final List<Long> userTaskKeys) {

        final var userTasksFound = awaitUserTasks(userTaskKeys);
        if (userTasksFound.size() < userTaskKeys.size()) {
            final var userTasksMissing = userTaskKeys
                    .stream()
                    .filter(userTaskKey -> !userTasksFound.containsKey(userTaskKey))
                    .toList();
            reportTheCockpitIsLaggingBehind(businessKey, userTasksMissing);
        }

        userTasksFound
                .values()
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

    /**
     * Keeps asking for the user tasks the application named while some of them are still missing,
     * until {@link #workflowVisibilityTimeout} is used up. A user task the application has just been
     * told about by a task delivery is known to the broker but not necessarily to the secondary
     * storage this search reads.
     *
     * @return what was found, in the order the application named it
     */
    private Map<Long, io.camunda.client.api.search.response.UserTask> awaitUserTasks(
            final List<Long> userTaskKeys) {

        final var userTasksFound = new LinkedHashMap<Long, io.camunda.client.api.search.response.UserTask>();
        final var giveUpAt = System.nanoTime() + workflowVisibilityTimeout.toNanos();
        while (true) {
            userTaskKeys
                    .stream()
                    .filter(userTaskKey -> !userTasksFound.containsKey(userTaskKey))
                    .forEach(userTaskKey -> searchUserTask(userTaskKey)
                            .ifPresent(userTask -> userTasksFound.put(userTaskKey, userTask)));
            if (userTasksFound.size() == userTaskKeys.size()) {
                return userTasksFound;
            }
            if (!waitBeforeAskingAgain(giveUpAt)) {
                return userTasksFound;
            }
        }

    }

    /**
     * Asks for one user task by its key. Filtering by key beats walking the pages of every user task
     * of the process: it is one precise request per task the application named, and it stays precise
     * however many user tasks the process has.
     */
    private Optional<io.camunda.client.api.search.response.UserTask> searchUserTask(
            final long userTaskKey) {

        return client
                .newUserTaskSearchRequest()
                .filter(filter -> {
                    filter.bpmnProcessId(bpmnProcessId);
                    filter.userTaskKey(userTaskKey);
                    if (tenantId != null) {
                        filter.tenantId(tenantId);
                    }
                })
                .send()
                .join()
                .items()
                .stream()
                .findFirst();

    }

    /**
     * Hands the lookup to {@link Camunda8AggregateChangedEvent}, which the cockpit's support service
     * runs once the caller's transaction committed. Doing it right here would ask a cluster which
     * cannot answer yet: the workflow may be started by this very transaction, and VanillaBP's
     * Camunda 8 adapter sends that command after the commit.
     *
     * <p>This is also what keeps the waiting harmless. Nothing of the business operation depends on
     * the answer any more, so the wait costs the caller time it no longer needs the cluster for,
     * rather than holding a database transaction open while an exporter catches up.
     */
    private void announceTheChangeAndLookItUpAfterTheCommit(
            final Runnable findWorkflowsAndNotifyTheCockpit) {

        applicationEventPublisher.publishEvent(
                new Camunda8AggregateChangedEvent(findWorkflowsAndNotifyTheCockpit));

    }

    /**
     * @return whether there is time left to ask the cluster once more
     */
    private boolean waitBeforeAskingAgain(
            final long giveUpAt) {

        if (System.nanoTime() >= giveUpAt) {
            return false;
        }
        try {
            Thread.sleep(TIME_BETWEEN_TWO_ATTEMPTS.toMillis());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

    }

    private void reportTheCockpitIsLaggingBehind(
            final Object businessKey) {

        logger.warn(
                "The Business Cockpit is lagging behind this application: within {} Camunda 8 did not report "
                + "any running workflow of BPMN process '{}'{} for workflow aggregate '{}'. The change of that "
                + "aggregate was not sent, so the cockpit keeps showing the older state until something else "
                + "happens to the workflow. If the workflow is running then the cluster needs longer to export "
                + "it: raise 'vanillabp.workflow-modules.<workflow-module-id>.adapters.{}."
                + "workflow-visibility-timeout'. If the workflow has ended or was never started then nothing "
                + "is missing and the call was superfluous.",
                workflowVisibilityTimeout,
                bpmnProcessId,
                ofTenant(),
                businessKey,
                Camunda8AdapterConfiguration.ADAPTER_ID);

    }

    private void reportTheCockpitIsLaggingBehind(
            final Object businessKey,
            final List<Long> userTasksMissing) {

        logger.warn(
                "The Business Cockpit is lagging behind this application: within {} Camunda 8 did not report "
                + "the user tasks {} of BPMN process '{}'{} for workflow aggregate '{}'. Their changes were not "
                + "sent, so the cockpit keeps showing the older state until something else happens to those "
                + "tasks. If the tasks are active then the cluster needs longer to export them: raise "
                + "'vanillabp.workflow-modules.<workflow-module-id>.adapters.{}.workflow-visibility-timeout'. "
                + "If they were completed or cancelled then nothing is missing and the call was superfluous.",
                workflowVisibilityTimeout,
                userTasksMissing,
                bpmnProcessId,
                ofTenant(),
                businessKey,
                Camunda8AdapterConfiguration.ADAPTER_ID);

    }

    private String ofTenant() {

        return tenantId == null ? "" : " of tenant '" + tenantId + "'";

    }

    /**
     * Unlike {@code aggregateChanged} this call answers the caller, so it cannot be deferred past
     * the caller's transaction and no waiting would help: whoever asks needs the user task now. The
     * request reads the secondary storage like the searches above, so a user task created moments
     * ago may not be there yet.
     */
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

    /**
     * A property given without a value binds to null, and the ten seconds of the property's own
     * default are the better answer than an exception in the middle of a business operation.
     */
    public void setWorkflowVisibilityTimeout(
            final Duration workflowVisibilityTimeout) {
        if (workflowVisibilityTimeout != null) {
            this.workflowVisibilityTimeout = workflowVisibilityTimeout;
        }
    }

}
