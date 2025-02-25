package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import io.vanillabp.cockpit.bpms.api.v1.BpmsApi;
import io.vanillabp.cockpit.bpms.api.v1.RegisterWorkflowModuleEvent;
import io.vanillabp.cockpit.bpms.api.v1.UserTaskCancelledEvent;
import io.vanillabp.cockpit.bpms.api.v1.UserTaskCompletedEvent;
import io.vanillabp.cockpit.bpms.api.v1.UserTaskCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.v1.WorkflowCancelledEvent;
import io.vanillabp.cockpit.bpms.api.v1.WorkflowCompletedEvent;
import io.vanillabp.cockpit.bpms.api.v1.WorkflowCreatedOrUpdatedEvent;
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
@RequestMapping("/bpms/api/v1")
public class BpmsApiController implements BpmsApi {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private TaskService taskService;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkflowModuleService workflowModuleService;

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

        log.info("Registering workflow module: {}", event);

        workflowModuleService.registerWorkflowModule(id, event);

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

        log.info("Received UserTaskCreatedEvent: {}", event);

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

        log.info("Received UserTaskCancelledEvent: {}", event);

        taskService.cancelTask(userTaskId, event);

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

        log.info("Received UserTaskCompletedEvent: {}", event);

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

        log.info("Received UserTaskUpdatedEvent: {}", event);

        taskService.updateTask(userTaskId, event);

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

        log.info("Received WorkflowCreatedEvent: {}", event);

        workflowService.createWorkflow(event.getWorkflowId(), event);

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

        log.info("Received WorkflowCancelledEvent: {}", event);

        workflowService.cancelWorkflow(workflowId, event);

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

        log.info("Received WorkflowUpdatedEvent: {}", event);

        workflowService.updateWorkflow(workflowId, event);

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

        log.info("Received WorkflowCompletedEvent: {}", event);

        workflowService.completeWorkflow(workflowId, event);

        return ResponseEntity.ok().build();

    }

}
