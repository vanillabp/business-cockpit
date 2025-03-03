package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import io.vanillabp.cockpit.bpms.api.v1.*;
import io.vanillabp.cockpit.gui.api.v1.UserTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the BPMS API.
 * Allows the creation and updating of UserTasks.
 * (See BpmsApi interface for more details about each overridden method)
 */
@RestController
@RequestMapping("/bpms/api/v1")
public class BpmsApiController implements BpmsApi {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final TaskService taskService;

    private final WorkflowService workflowService;

    /**
     * Constructor for usertask.BpmsApiController.
     *
     * @param taskService The usertask.TaskService for managing UserTasks.
     */
    @Autowired
    public BpmsApiController(
            final TaskService taskService,
            final WorkflowService workflowService) {

        this.taskService = taskService;
        this.workflowService = workflowService;
    }

    /**
     * Register a workflow-module or update the registration
     *
     * @param id    (required)
     * @param event (required)
     * @return Returns the fitting response code.
     */
    @Override
    public ResponseEntity<Void> registerWorkflowModule(
            final String id,
            final RegisterWorkflowModuleEvent event) {

        log.info("Registering workflow module {}", id);

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
            final UserTaskCreatedOrUpdatedEvent event) {

        log.info("Received UserTaskCreatedEvent for ID: {}", event.getId());

        // Store the task in the TaskService
        taskService.createTask(event.getUserTaskId(), event);

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

        taskService.removeTask(userTaskId);
        log.info("Received UserTaskCancelledEvent for ID: {}", event.getId());

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

        log.info("Received UserTaskCompletedEvent for ID: {}", event.getId());
        taskService.completeTask(userTaskId, event);

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
            final UserTaskCreatedOrUpdatedEvent event) {

        taskService.updateTask(userTaskId, event);
        log.info("Received UserTaskUpdatedEvent for ID: {}", event.getId());

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
            final WorkflowCreatedOrUpdatedEvent event) {

        workflowService.createWorkflow(event.getWorkflowId(), event);
        log.info("Received WorkflowCreatedEvent for ID: {}", event.getId());

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

        workflowService.removeWorkflow(workflowId);
        log.info("Received WorkflowCancelledEvent for ID: {}", event.getId());

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
            final WorkflowCreatedOrUpdatedEvent event) {

        workflowService.updateWorkflow(workflowId, event);
        log.info("Received WorkflowUpdatedEvent for ID: {}", event.getId());

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

        workflowService.removeWorkflow(workflowId);
        log.info("Received WorkflowCompletedEvent for ID: {}", event.getId());

        return ResponseEntity.ok().build();
    }

}
