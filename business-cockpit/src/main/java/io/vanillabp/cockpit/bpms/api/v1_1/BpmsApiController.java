package io.vanillabp.cockpit.bpms.api.v1_1;

import io.vanillabp.cockpit.bpms.BpmsApiWebSecurityConfiguration;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.model.UserTaskEndReason;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import io.vanillabp.cockpit.workflowmodules.WorkflowModuleService;
import jakarta.validation.Valid;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("bpmsApiControllerV1_1")
@RequestMapping(path = BpmsApiController.BPMS_API_URL_PREFIX)
@Secured(BpmsApiWebSecurityConfiguration.BPMS_API_AUTHORITY)
public class BpmsApiController implements BpmsApi {

	public static final String BPMS_API_URL_PREFIX = "/bpms/api/v1_1";

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
            final @Valid UserTaskCreatedEvent userTaskCreatedEvent) {

        return okOrBadRequest(
                userTaskService.reportCreatedUserTask(
                        userTaskCreatedEvent.getUserTaskId(),
                        userTaskCreatedEvent.getTimestamp(),
                        () -> userTaskMapper.toNewTask(userTaskCreatedEvent)));

    }

    @Override
    public ResponseEntity<Void> userTaskUpdatedEvent(
            final String userTaskId,
            final @Valid UserTaskUpdatedEvent userTaskUpdatedEvent) {

        return okOrBadRequest(
                userTaskService.reportChangedUserTask(
                        userTaskId,
                        userTaskUpdatedEvent.getTimestamp(),
                        () -> userTaskMapper.toNewTask(userTaskUpdatedEvent),
                        task -> userTaskMapper.toUpdatedTask(userTaskUpdatedEvent, task)));

    }

    @Override
    public ResponseEntity<Void> userTaskCompletedEvent(
            final String userTaskId,
            final @Valid UserTaskCompletedEvent userTaskCompletedEvent) {

        return okOrBadRequest(
                userTaskService.reportEndedUserTask(
                        userTaskId,
                        userTaskCompletedEvent.getTimestamp(),
                        UserTaskEndReason.COMPLETED,
                        task -> {
                            // an end carries the same fields as a change, so the list of finished work
                            // shows what the task was finished with instead of what the last change
                            // happened to say
                            userTaskMapper.toEndedTask(userTaskCompletedEvent, task);
                            // capture who completed the task so the notification poller can tell a
                            // completion by another user apart from a self-completion (AC func 2c).
                            // 'initiator' and not 'updatedBy': the latter is audit information
                            // overwritten by UpdateInformationEventListener on every save.
                            task.setInitiator(userTaskCompletedEvent.getInitiator());
                        }));

    }

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
                            // see userTaskCompletedEvent
                            userTaskMapper.toEndedTask(userTaskCancelledEvent, task);
                            task.setInitiator(userTaskCancelledEvent.getInitiator());
                        }));

    }

    @Override
    public ResponseEntity<Void> workflowCreatedEvent(
            final @Valid io.vanillabp.cockpit.bpms.api.v1_1.WorkflowCreatedEvent workflowCreatedEvent) {

        return okOrBadRequest(
                workflowlistService.reportCreatedWorkflow(
                        workflowCreatedEvent.getWorkflowId(),
                        workflowCreatedEvent.getTimestamp(),
                        () -> workflowMapper.toNewWorkflow(workflowCreatedEvent)));

    }

    @Override
    public ResponseEntity<Void> workflowCancelledEvent(
            final String workflowId,
            final WorkflowCancelledEvent workflowCancelledEvent) {

        return okOrBadRequest(
                workflowlistService.reportEndedWorkflow(
                        workflowId,
                        workflowCancelledEvent.getTimestamp(),
                        workflow -> workflowMapper.toEndedWorkflow(workflowCancelledEvent, workflow)));

    }

    @Override
    public ResponseEntity<Void> workflowCompletedEvent(
            final String workflowId,
            final WorkflowCompletedEvent workflowCompletedEvent) {

        return okOrBadRequest(
                workflowlistService.reportEndedWorkflow(
                        workflowId,
                        workflowCompletedEvent.getTimestamp(),
                        // see userTaskCompletedEvent: an end says what the case ended with
                        workflow -> workflowMapper.toEndedWorkflow(workflowCompletedEvent, workflow)));

    }


    @Override
    public ResponseEntity<Void> workflowUpdatedEvent(
            final String workflowId,
            final WorkflowUpdatedEvent workflowUpdatedEvent) {

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
                Optional
                        .ofNullable(registerWorkflowModuleEvent.getGroupHierarchy())
                        .map(hierarchies -> hierarchies
                                .stream()
                                .collect(Collectors.toMap(
                                        GroupHierarchy::getGroup,
                                        hierarchy -> (Collection<String>) hierarchy.getTargets())))
                        .orElse(null));

        return ResponseEntity.ok().build();

    }

    private static ResponseEntity<Void> okOrBadRequest(
            final boolean succeeded) {

        return succeeded
                ? ResponseEntity.ok().build()
                : ResponseEntity.badRequest().build();

    }

}
