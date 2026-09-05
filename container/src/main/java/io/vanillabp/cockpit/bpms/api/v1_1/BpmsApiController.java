package io.vanillabp.cockpit.bpms.api.v1_1;

import io.vanillabp.cockpit.bpms.BpmsApiWebSecurityConfiguration;
import io.vanillabp.cockpit.tasklist.UserTaskService;
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
                userTaskService.createUserTask(
                        userTaskMapper.toNewTask(userTaskCreatedEvent)));

    }

    @Override
    public ResponseEntity<Void> userTaskUpdatedEvent(
            final String userTaskId,
            final @Valid UserTaskUpdatedEvent userTaskUpdatedEvent) {

        final var knownTask = userTaskService.getUserTask(userTaskId);
        // reporting an update for a task the cockpit never saw creates it, so a cockpit added to a
        // running system does not stay blind to the tasks that existed before
        if (knownTask == null) {
            return okOrBadRequest(
                    userTaskService.createUserTask(
                            userTaskMapper.toNewTask(userTaskUpdatedEvent)));
        }

        return okOrBadRequest(
                userTaskService.updateUserTask(
                        userTaskMapper.toUpdatedTask(userTaskUpdatedEvent, knownTask)));

    }

    @Override
    public ResponseEntity<Void> userTaskCompletedEvent(
            final String userTaskId,
            final @Valid UserTaskCompletedEvent userTaskCompletedEvent) {

        final var task = userTaskService.getUserTask(userTaskId);
        if (task == null) {
            return ResponseEntity.ok().build();
        }

        task.setEndedAt(
                userTaskCompletedEvent.getTimestamp());
        // capture who completed the task so the notification poller can tell a
        // completion by another user apart from a self-completion (AC func 2c).
        // 'initiator' and not 'updatedBy': the latter is audit information overwritten
        // by UpdateInformationEventListener on every save.
        task.setInitiator(userTaskCompletedEvent.getInitiator());

        return okOrBadRequest(
                userTaskService.completeUserTask(
                        task,
                        userTaskCompletedEvent.getTimestamp()));

    }

    @Override
    public ResponseEntity<Void> userTaskCancelledEvent(
            final String userTaskId,
            final @Valid UserTaskCancelledEvent userTaskCancelledEvent) {

        final var task = userTaskService.getUserTask(userTaskId);
        if (task == null) {
            return ResponseEntity.ok().build();
        }

        task.setEndedAt(
                userTaskCancelledEvent.getTimestamp());
        task.setInitiator(userTaskCancelledEvent.getInitiator());

        return okOrBadRequest(
                userTaskService.cancelUserTask(
                        task,
                        userTaskCancelledEvent.getTimestamp(),
                        userTaskCancelledEvent.getComment()));

    }

    @Override
    public ResponseEntity<Void> workflowCreatedEvent(
            final @Valid io.vanillabp.cockpit.bpms.api.v1_1.WorkflowCreatedEvent workflowCreatedEvent) {

        return okOrBadRequest(
                workflowlistService.createWorkflow(
                        workflowMapper.toNewWorkflow(workflowCreatedEvent)));

    }

    @Override
    public ResponseEntity<Void> workflowCancelledEvent(
            final String workflowId,
            final WorkflowCancelledEvent workflowCancelledEvent) {

        final var workflow = workflowlistService.getWorkflow(workflowId);
        if (workflow == null) {
            return ResponseEntity.ok().build();
        }

        workflow.setEndedAt(
                workflowCancelledEvent.getTimestamp());

        return okOrBadRequest(
                workflowlistService.cancelWorkflow(
                        workflow,
                        workflowCancelledEvent.getTimestamp(),
                        workflowCancelledEvent.getComment()));

    }

    @Override
    public ResponseEntity<Void> workflowCompletedEvent(
            final String workflowId,
            final WorkflowCompletedEvent workflowCompletedEvent) {

        final var workflow = workflowlistService.getWorkflow(workflowId);
        if (workflow == null) {
            return ResponseEntity.ok().build();
        }

        workflow.setEndedAt(
                workflowCompletedEvent.getTimestamp());

        return okOrBadRequest(
                workflowlistService.completeWorkflow(
                        workflow,
                        workflowCompletedEvent.getTimestamp()));

    }


    @Override
    public ResponseEntity<Void> workflowUpdatedEvent(
            final String workflowId,
            final WorkflowUpdatedEvent workflowUpdatedEvent) {

        final var knownWorkflow = workflowlistService.getWorkflow(workflowId);
        // see userTaskUpdatedEvent: an update of an unknown workflow creates it
        if (knownWorkflow == null) {
            return okOrBadRequest(
                    workflowlistService.createWorkflow(
                            workflowMapper.toNewWorkflow(workflowUpdatedEvent)));
        }

        return okOrBadRequest(
                workflowlistService.updateWorkflow(
                        workflowMapper.toUpdatedWorkflow(workflowUpdatedEvent, knownWorkflow)));

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
