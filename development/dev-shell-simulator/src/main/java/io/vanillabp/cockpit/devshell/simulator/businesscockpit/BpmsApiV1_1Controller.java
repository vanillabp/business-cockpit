package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import io.vanillabp.cockpit.bpms.api.v1_1.BpmsApi;
import io.vanillabp.cockpit.bpms.api.v1_1.RegisterWorkflowModuleEvent;
import io.vanillabp.cockpit.bpms.api.v1_1.UserTaskCancelledEvent;
import io.vanillabp.cockpit.bpms.api.v1_1.UserTaskCompletedEvent;
import io.vanillabp.cockpit.bpms.api.v1_1.UserTaskCreatedEvent;
import io.vanillabp.cockpit.bpms.api.v1_1.UserTaskUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.v1_1.WorkflowCancelledEvent;
import io.vanillabp.cockpit.bpms.api.v1_1.WorkflowCompletedEvent;
import io.vanillabp.cockpit.bpms.api.v1_1.WorkflowCreatedEvent;
import io.vanillabp.cockpit.bpms.api.v1_1.WorkflowUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the BPMS API.
 * Allows the creation and updating of UserTasks.
 * (See BpmsApi interface for more details about each overridden method)
 */
@RestController
@RequestMapping("/bpms/api/v1_1")
public class BpmsApiV1_1Controller implements BpmsApi {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private TaskService taskService;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkflowModuleService workflowModuleService;

    @Autowired
    private OfficialApiMapper mapper;

    /**
     * Register a workflow-module or update the registration
     *
     * @param workflowModuleId    (required)
     * @param event (required)
     * @return Returns the fitting response code.
     */
    @Override
    public ResponseEntity<Void> registerWorkflowModule(
            final String workflowModuleId,
            final RegisterWorkflowModuleEvent event) {

        log.info("Registering workflow module v1.1: {}", event);

        final var workflowModule = mapper.toModel(event, workflowModuleId);
        workflowModuleService.registerWorkflowModule(workflowModuleId, workflowModule);

        return ResponseEntity.ok().build();

    }

    /**
     * Processes a user task CREATED event
     *
     * @param event (required)
     * @return Returns the fitting response code.
     */
    @Override
    public ResponseEntity<Void> userTaskCreatedEvent(
            final UserTaskCreatedEvent event) {

        log.info("Received UserTaskCreatedEvent v1.1: {}", event);

        final var userTask = mapper.toModel(event);

        // Store the task in the TaskService
        taskService.createTask(event.getUserTaskId(), userTask);

        return ResponseEntity.ok().build();

    }

    /**
     * @param userTaskId (required)
     * @param event      (required)
     * @return Returns the fitting response code.
     */
    @Override
    public ResponseEntity<Void> userTaskCancelledEvent(
            final String userTaskId,
            final UserTaskCancelledEvent event) {

        log.info("Received UserTaskCancelledEvent v1.1: {}", event);

        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null!");
        }

        taskService.cancelTask(userTaskId, userTask -> mapper.ontoApi(userTask, event));

        return ResponseEntity.ok().build();

    }

    /**
     * Processes a user task CANCELLED event
     *
     * @param userTaskId (required)
     * @param event      (required)
     * @return Returns the fitting response code.
     */
    @Override
    public ResponseEntity<Void> userTaskCompletedEvent(
            final String userTaskId,
            final UserTaskCompletedEvent event) {

        log.info("Received UserTaskCompletedEvent v1.1: {}", event);

        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null!");
        }

        taskService.completeTask(userTaskId, userTask -> mapper.ontoApi(userTask, event));

        return ResponseEntity.ok().build();

    }

    /**
     * @param userTaskId (required)
     * @param event      (required)
     * @return Returns the fitting response code.
     */
    @Override
    public ResponseEntity<Void> userTaskUpdatedEvent(
            final String userTaskId,
            final UserTaskUpdatedEvent event) {

        log.info("Received UserTaskUpdatedEvent v1.1: {}", event);

        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null!");
        }

        taskService.updateTask(userTaskId, userTask -> mapper.ontoApi(userTask, event));

        return ResponseEntity.ok().build();

    }


    /**
     * Processes a user task UPDATED event
     *
     * @param event (required)
     * @return Returns the fitting response code.
     */
    @Override
    public ResponseEntity<Void> workflowCreatedEvent(
            final WorkflowCreatedEvent event) {

        log.info("Received WorkflowCreatedEvent v1.1: {}", event);

        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null!");
        }

        final var workflow = mapper.toModel(event);
        workflowService.createWorkflow(event.getWorkflowId(), workflow);

        return ResponseEntity.ok().build();

    }

    /**
     * @param workflowId The unique key of the workflow (required)
     * @param event      (required)
     * @return Returns the fitting response code.
     */
    @Override
    public ResponseEntity<Void> workflowCancelledEvent(
            final String workflowId,
            final WorkflowCancelledEvent event) {

        log.info("Received WorkflowCancelledEvent v1.1: {}", event);

        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null!");
        }

        workflowService.cancelWorkflow(workflowId, workflow -> mapper.ontoApi(workflow, event));

        return ResponseEntity.ok().build();

    }

    /**
     * Processes a workflow CANCELLED event
     *
     * @param workflowId The unique key of the workflow (required)
     * @param event      (required)
     * @return Returns the fitting response code.
     */
    @Override
    public ResponseEntity<Void> workflowUpdatedEvent(
            final String workflowId,
            final WorkflowUpdatedEvent event) {

        log.info("Received WorkflowUpdatedEvent v1.1: {}", event);

        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null!");
        }

        workflowService.updateWorkflow(workflowId, workflow -> mapper.ontoApi(workflow, event));

        return ResponseEntity.ok().build();

    }

    /**
     * Processes a workflow COMPLETED event (see BpmsApi interface for details)
     *
     * @param workflowId The unique key of the workflow (required)
     * @param event      (required)
     * @return Returns the fitting response code.
     */
    @Override
    public ResponseEntity<Void> workflowCompletedEvent(
            final String workflowId,
            final WorkflowCompletedEvent event) {

        log.info("Received WorkflowCompletedEvent v1.1: {}", event);

        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null!");
        }

        workflowService.completeWorkflow(workflowId, workflow -> mapper.ontoApi(workflow, event));

        return ResponseEntity.ok().build();

    }

}
