package io.vanillabp.cockpit.bpms.api.v1;

import io.vanillabp.cockpit.bpms.AnswerToAReport;
import io.vanillabp.cockpit.bpms.BpmsApiWebSecurityConfiguration;
import io.vanillabp.cockpit.bpms.PathAndBody;
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

        return AnswerToAReport.afterStoring(
                userTaskService.reportCreatedUserTask(
                        userTaskCreatedEvent.getUserTaskId(),
                        userTaskCreatedEvent.getTimestamp(),
                        () -> userTaskMapper.toNewTask(userTaskCreatedEvent)));

    }

    @Override
    public ResponseEntity<Void> userTaskUpdatedEvent(
            final String userTaskId,
            final @Valid UserTaskCreatedOrUpdatedEvent userTaskUpdatedEvent) {

        if (!PathAndBody.nameTheSameRecord("user task", userTaskId, userTaskUpdatedEvent.getUserTaskId())) {
            return AnswerToAReport.refused();
        }
        return AnswerToAReport.afterStoring(
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

        if (!PathAndBody.nameTheSameRecord("user task", userTaskId, userTaskCompletedEvent.getUserTaskId())) {
            return AnswerToAReport.refused();
        }
        return AnswerToAReport.afterStoring(
                userTaskService.reportEndedUserTask(
                        userTaskId,
                        userTaskCompletedEvent.getTimestamp(),
                        UserTaskEndReason.COMPLETED,
                        userTaskCompletedEvent.getCreatedAt(),
                        // who completed the task, as the application reported it. The
                        // notification poller reads it to skip a self-completion
                        task -> task.setInitiator(userTaskCompletedEvent.getInitiator())));

    }

    /** @see #userTaskCompletedEvent(String, UserTaskCompletedEvent) */
    @Override
    public ResponseEntity<Void> userTaskCancelledEvent(
            final String userTaskId,
            final @Valid UserTaskCancelledEvent userTaskCancelledEvent) {

        if (!PathAndBody.nameTheSameRecord("user task", userTaskId, userTaskCancelledEvent.getUserTaskId())) {
            return AnswerToAReport.refused();
        }
        return AnswerToAReport.afterStoring(
                userTaskService.reportEndedUserTask(
                        userTaskId,
                        userTaskCancelledEvent.getTimestamp(),
                        UserTaskEndReason.CANCELLED,
                        userTaskCancelledEvent.getCreatedAt(),
                        task -> {
                            task.setInitiator(userTaskCancelledEvent.getInitiator());
                            task.setComment(userTaskCancelledEvent.getComment());
                        }));

    }

    @Override
    public ResponseEntity<Void> workflowCreatedEvent(
            final @Valid WorkflowCreatedOrUpdatedEvent workflowCreatedEvent) {

        return AnswerToAReport.afterStoring(
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

        if (!PathAndBody.nameTheSameRecord("workflow", workflowId, workflowCancelledEvent.getWorkflowId())) {
            return AnswerToAReport.refused();
        }
        return AnswerToAReport.afterStoring(
                workflowlistService.reportEndedWorkflow(
                        workflowId,
                        workflowCancelledEvent.getTimestamp(),
                        workflowCancelledEvent.getCreatedAt(),
                        workflow -> workflow.setComment(workflowCancelledEvent.getComment())));

    }

    /** @see #userTaskCompletedEvent(String, UserTaskCompletedEvent) */
    @Override
    public ResponseEntity<Void> workflowCompletedEvent(
            final String workflowId,
            final WorkflowCompletedEvent workflowCompletedEvent) {

        if (!PathAndBody.nameTheSameRecord("workflow", workflowId, workflowCompletedEvent.getWorkflowId())) {
            return AnswerToAReport.refused();
        }
        return AnswerToAReport.afterStoring(
                workflowlistService.reportEndedWorkflow(
                        workflowId,
                        workflowCompletedEvent.getTimestamp(),
                        workflowCompletedEvent.getCreatedAt(),
                        // version 1 reports nothing about a completed case but that it completed
                        workflow -> { }));

    }


    @Override
    public ResponseEntity<Void> workflowUpdatedEvent(
            final String workflowId,
            final WorkflowCreatedOrUpdatedEvent workflowUpdatedEvent) {

        if (!PathAndBody.nameTheSameRecord("workflow", workflowId, workflowUpdatedEvent.getWorkflowId())) {
            return AnswerToAReport.refused();
        }
        return AnswerToAReport.afterStoring(
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

}
