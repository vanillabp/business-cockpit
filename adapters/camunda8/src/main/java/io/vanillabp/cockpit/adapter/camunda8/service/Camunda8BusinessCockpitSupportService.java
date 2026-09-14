package io.vanillabp.cockpit.adapter.camunda8.service;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.search.response.ProcessInstance;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8UserTaskEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowEvent;
import io.vanillabp.cockpit.adapter.camunda8.usertask.Camunda8UserTaskEventHandler;
import io.vanillabp.cockpit.adapter.camunda8.workflow.Camunda8WorkflowEventHandler;
import io.vanillabp.spi.process.WorkflowNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

public class Camunda8BusinessCockpitSupportService {

    private static final Logger logger = LoggerFactory.getLogger(Camunda8BusinessCockpitSupportService.class);

    private final Camunda8WorkflowEventHandler workflowEventHandler;

    private final Camunda8UserTaskEventHandler userTaskEventHandler;

    public Camunda8BusinessCockpitSupportService(Camunda8WorkflowEventHandler workflowEventHandler, Camunda8UserTaskEventHandler userTaskEventHandler) {
        this.workflowEventHandler = workflowEventHandler;
        this.userTaskEventHandler = userTaskEventHandler;
    }

    @TransactionalEventListener(
            value = Camunda8WorkflowEvent.class,
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processAggregateChangedEvent(
            final Camunda8WorkflowEvent event) {

        workflowEventHandler.processEvent(
                event.getTenantId(),
                event.getBpmnProcessId(),
                event.getProcessDefinitionVersion(),
                event.getProcessInstanceKey(),
                camunda8WorkflowHandler -> camunda8WorkflowHandler.notify(event));

    }

    @TransactionalEventListener(
            value = Camunda8UserTaskEvent.class,
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processAggregateChangedEvent(
            final Camunda8UserTaskEvent event) {

        userTaskEventHandler.processEvent(
                event.getTenantId(),
                event.getElementId(),
                event.getBpmnProcessId(),
                event.getProcessDefinitionVersion(),
                event.getProcessInstanceKey(),
                event.getTaskDefinition(),
                camunda8UserTaskHandler -> camunda8UserTaskHandler.notify(event));

    }

    @Retryable(retryFor = WorkflowNotFoundException.class,
            maxAttempts = 8,
            backoff = @Backoff(delay = 1000, multiplier = 1.25, maxDelay = 2))
    public List<ProcessInstance> getProcessInstances(
            final CamundaClient client,
            final String tenantId,
            final String bpmnProcessId,
            final String businessKey,
            final String workflowAggregateIdName) {

        final var processes = client
                .newProcessInstanceSearchRequest()
                .filter(filter -> {
                    filter.processDefinitionId(bpmnProcessId);
                    filter.variables(
                            Map.of(workflowAggregateIdName, "\"" + businessKey + "\""));
                    if (tenantId != null) {
                        filter.tenantId(tenantId);
                    }
                })
                .send()
                .join()
                .items();

        if (processes.isEmpty()) {
            if (tenantId == null) {
                throw new WorkflowNotFoundException(
                        "Could not find process instance for business key '%s', BPMN process ID '%s'!"
                                .formatted(businessKey, bpmnProcessId));
            } else {
                throw new WorkflowNotFoundException("Could not find process instance for business key '%s', BPMN process ID '%s', tenant '%s'!"
                                .formatted(businessKey, bpmnProcessId, tenantId));
            }
        }

        return processes;

    }

    @Retryable(retryFor = WorkflowNotFoundException.class,
            maxAttempts = 8,
            backoff = @Backoff(delay = 1000, multiplier = 1.25, maxDelay = 2))
    public List<io.camunda.client.api.search.response.UserTask> getUserTasks(
            final CamundaClient client,
            final String tenantId,
            final String bpmnProcessId,
            final String businessKey,
            final List<Long> userTaskIds) {

        final var userTasksFound = new HashSet<io.camunda.client.api.search.response.UserTask>();
        String afterCursor = null;
        boolean nextPage = true;
        while (nextPage) {
            var after = afterCursor;
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
                    .page(request -> {
                        request.limit(100);
                        if (after != null) {
                            request.after(after);
                        }
                    })
                    .execute();
            result
                    .items()
                    .stream()
                    .filter(userTask -> userTaskIds.contains(userTask.getUserTaskKey()))
                    .forEach(userTasksFound::add);
            afterCursor = result.page().endCursor();
            if (
                    userTasksFound.size() == userTaskIds.size() // All user tasks found
                    || result.items().isEmpty()                 // No more items
                    || afterCursor == null                      // No more pages
            ) {
                nextPage = false;
            }
        }

        if (userTasksFound.isEmpty()) {
            if (tenantId == null) {
                throw new WorkflowNotFoundException(
                        "Could not find user tasks %s for business key '%s', BPMN process ID '%s'!"
                                .formatted(userTaskIds, businessKey, bpmnProcessId));
            } else {
                throw new WorkflowNotFoundException(
                        "Could not find user tasks %s for business key '%s', BPMN process ID '%s', tenant '%s'!"
                                .formatted(userTaskIds, businessKey, bpmnProcessId, tenantId));
            }
        }

        return List.copyOf(userTasksFound);

    }

    @Recover
    public List<io.camunda.client.api.search.response.UserTask> getUserTasks(
            final WorkflowNotFoundException cause,
            final CamundaClient client,
            final String tenantId,
            final String bpmnProcessId,
            final String businessKey,
            final List<Long> userTaskIds) {

        if (tenantId == null) {
            logger.warn("Could not found user tasks {} for business key '{}', BPMN process ID '{}'!",
                    userTaskIds, businessKey, bpmnProcessId);
        } else {
            logger.warn("Could not found user tasks {} for business key '{}', BPMN process ID '{}', tenant '{}'!",
                    userTaskIds, businessKey, bpmnProcessId, tenantId);
        }
        return List.of();

    }

    @Retryable(retryFor = WorkflowNotFoundException.class,
            maxAttempts = 8,
            backoff = @Backoff(delay = 1000, multiplier = 1.25, maxDelay = 2))
    public io.camunda.client.api.search.response.UserTask getUserTask(
            final CamundaClient client,
            final String tenantId,
            final String bpmnProcessId,
            final long userTaskId) {

        final var userTask = client
                .newUserTaskGetRequest(userTaskId)
                .execute();

        if (userTask == null) {
            if (tenantId == null) {
                throw new WorkflowNotFoundException(
                        "Could not find user task '%d' for BPMN process ID '%s'!"
                                .formatted(userTaskId, bpmnProcessId));
            } else {
                throw new WorkflowNotFoundException(
                        "Could not find user task '%d' for BPMN process ID '%s', tenant '%s'!"
                                .formatted(userTaskId, bpmnProcessId, tenantId));
            }
        }

        return userTask;

    }

}
