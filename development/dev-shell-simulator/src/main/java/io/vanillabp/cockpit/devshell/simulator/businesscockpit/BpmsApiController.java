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

    @Override
    public ResponseEntity<Void> registerWorkflowModule(
            final String id,
            final RegisterWorkflowModuleEvent event) {

        log.info("Registering workflow module {}", id);

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> userTaskCreatedEvent(
            final UserTaskCreatedOrUpdatedEvent event) {

        log.info("Received UserTaskCreatedEvent for ID: {}", event.getId());

        // Store the task in the TaskService
        taskService.createTask(event.getUserTaskId(), event);

        return ResponseEntity.ok().build();
    }



    @Override
    public ResponseEntity<Void> userTaskUpdatedEvent(
            final String userTaskId,
            final UserTaskCreatedOrUpdatedEvent event) {

        taskService.updateTask(userTaskId, event.getDetails());

        return ResponseEntity.ok().build();
    }



    @Override
    public ResponseEntity<Void> workflowCreatedEvent(
            final WorkflowCreatedOrUpdatedEvent event) {

        workflowService.createWorkflow(event.getWorkflowId(), event);

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> workflowUpdatedEvent(
            final String workflowId,
            final WorkflowCreatedOrUpdatedEvent event) {

        workflowService.updateWorkflow(workflowId, event.getDetails());

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> workflowCompletedEvent(
            final String workflowId,
            final WorkflowCompletedEvent event) {


        return ResponseEntity.ok().build();
    }

}
