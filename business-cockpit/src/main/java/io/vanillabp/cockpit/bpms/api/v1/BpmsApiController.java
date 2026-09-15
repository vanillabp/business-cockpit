package io.vanillabp.cockpit.bpms.api.v1;

import io.vanillabp.cockpit.bpms.BpmsApiWebSecurityConfiguration;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.model.UserTaskEndReason;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import io.vanillabp.cockpit.workflowmodules.WorkflowModuleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("bpmsApiControllerV1")
@RequestMapping(path = BpmsApiController.BPMS_API_URL_PREFIX)
@Secured(BpmsApiWebSecurityConfiguration.BPMS_API_AUTHORITY)
public class BpmsApiController implements BpmsApi {

	public static final String BPMS_API_URL_PREFIX = "/bpms/api/v1";

    @Autowired
    private UserTaskMapper userTaskMapper;

    @Autowired
    private WorkflowMapper workflowMapper;

    @Autowired
    private UserTaskService userTaskService;

    @Autowired
    private WorkflowlistService workflowlistService;

    @Autowired
    private WorkflowModuleService workflowModuleService;

    @Override
    public ResponseEntity<Void> userTaskCreatedEvent(
            final @Valid UserTaskCreatedOrUpdatedEvent userTaskCreatedEvent) {

        return okOrBadRequest(
                userTaskService.reportCreatedUserTask(
                        userTaskCreatedEvent.getUserTaskId(),
                        userTaskCreatedEvent.getTimestamp(),
                        () -> userTaskMapper.toNewTask(userTaskCreatedEvent)));

    }

    @Override
    public ResponseEntity<Void> userTaskUpdatedEvent(
            final String userTaskId,
            final @Valid UserTaskCreatedOrUpdatedEvent userTaskUpdatedEvent) {

        return okOrBadRequest(
                userTaskService.reportChangedUserTask(
                        userTaskId,
                        userTaskUpdatedEvent.getTimestamp(),
                        () -> userTaskMapper.toNewTask(userTaskUpdatedEvent),
                        task -> userTaskMapper.toUpdatedTask(userTaskUpdatedEvent, task)));

    }

    /**
     * Version 1 of the API reports an end without the fields a change carries. A task the cockpit
     * hears of by its end alone is therefore stored with the end and nothing else. The creation is
     * still on its way and fills the rest in when it arrives.
     * <p>
     * For the same reason an end is not mapped here. An end of this version says who ended the
     * task, and for a cancellation it also says why. Both are written by hand below. So an end of
     * this version can never take the due date, the title or the business data away from a stored
     * task, whatever the reporting side leaves out.
     */
    @Override
    public ResponseEntity<Void> userTaskCompletedEvent(
            final String userTaskId,
            final @Valid UserTaskCompletedEvent userTaskCompletedEvent) {

        return okOrBadRequest(
                userTaskService.reportEndedUserTask(
                        userTaskId,
                        userTaskCompletedEvent.getTimestamp(),
                        UserTaskEndReason.COMPLETED,
                        // who completed the task, as the application reported it. The
                        // notification poller reads it to skip a self-completion
                        task -> task.setInitiator(userTaskCompletedEvent.getInitiator())));

    }

    /** @see #userTaskCompletedEvent(String, UserTaskCompletedEvent) */
    @Override
    public ResponseEntity<Void> userTaskCancelledEvent(
            final String userTaskId,
            final @Valid UserTaskCancelledEvent userTaskCancelledEvent) {

        return okOrBadRequest(
                userTaskService.reportEndedUserTask(
                        userTaskId,
                        userTaskCancelledEvent.getTimestamp(),
                        UserTaskEndReason.CANCELLED,
                        task -> {
                            task.setInitiator(userTaskCancelledEvent.getInitiator());
                            task.setComment(userTaskCancelledEvent.getComment());
                        }));

    }

    @Override
    public ResponseEntity<Void> workflowCreatedEvent(
            final @Valid WorkflowCreatedOrUpdatedEvent workflowCreatedEvent) {

        return okOrBadRequest(
                workflowlistService.reportCreatedWorkflow(
                        workflowCreatedEvent.getWorkflowId(),
                        workflowCreatedEvent.getTimestamp(),
                        () -> workflowMapper.toNewWorkflow(workflowCreatedEvent)));

    }

    /** @see #userTaskCompletedEvent(String, UserTaskCompletedEvent) */
    @Override
    public ResponseEntity<Void> workflowCancelledEvent(
            final String workflowId,
            final WorkflowCancelledEvent workflowCancelledEvent) {

        return okOrBadRequest(
                workflowlistService.reportEndedWorkflow(
                        workflowId,
                        workflowCancelledEvent.getTimestamp(),
                        workflow -> workflow.setComment(workflowCancelledEvent.getComment())));

    }

    /** @see #userTaskCompletedEvent(String, UserTaskCompletedEvent) */
    @Override
    public ResponseEntity<Void> workflowCompletedEvent(
            final String workflowId,
            final WorkflowCompletedEvent workflowCompletedEvent) {

        return okOrBadRequest(
                workflowlistService.reportEndedWorkflow(
                        workflowId,
                        workflowCompletedEvent.getTimestamp(),
                        // version 1 reports nothing about a completed case but that it completed
                        workflow -> { }));

    }


    @Override
    public ResponseEntity<Void> workflowUpdatedEvent(
            final String workflowId,
            final WorkflowCreatedOrUpdatedEvent workflowUpdatedEvent) {

        return okOrBadRequest(
                workflowlistService.reportChangedWorkflow(
                        workflowId,
                        workflowUpdatedEvent.getTimestamp(),
                        () -> workflowMapper.toNewWorkflow(workflowUpdatedEvent),
                        workflow -> workflowMapper.toUpdatedWorkflow(workflowUpdatedEvent, workflow)));

    }

    @Override
    public ResponseEntity<Void> registerWorkflowModule(
            final String id,
            final RegisterWorkflowModuleEvent registerWorkflowModuleEvent) {

        workflowModuleService.registerOrUpdateWorkflowModule(
                id,
                registerWorkflowModuleEvent.getUri(),
                registerWorkflowModuleEvent.getTaskProviderApiUriPath(),
                registerWorkflowModuleEvent.getWorkflowProviderApiUriPath(),
                registerWorkflowModuleEvent.getAccessibleToGroups(),
                null);

        return ResponseEntity.ok().build();

    }

    private static ResponseEntity<Void> okOrBadRequest(
            final boolean succeeded) {

        return succeeded
                ? ResponseEntity.ok().build()
                : ResponseEntity.badRequest().build();

    }

}
