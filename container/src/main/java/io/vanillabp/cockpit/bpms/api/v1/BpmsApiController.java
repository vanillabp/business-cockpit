package io.vanillabp.cockpit.bpms.api.v1;

import io.vanillabp.cockpit.bpms.BpmsApiWebSecurityConfiguration;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import io.vanillabp.cockpit.workflowmodules.WorkflowModuleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
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

    private Mono<Boolean> userTaskCreatedMono(
            final Mono<UserTaskCreatedOrUpdatedEvent> userTaskCreatedEvent) {

        return userTaskCreatedEvent
                .map(userTaskMapper::toNewTask)
                .flatMap(userTaskService::createUserTask);

    }

    @Override
    public Mono<ResponseEntity<Void>> userTaskCreatedEvent(
            final @Valid Mono<UserTaskCreatedOrUpdatedEvent> userTaskCreatedEvent,
            final ServerWebExchange exchange) {

        return userTaskCreatedMono(userTaskCreatedEvent)
                .map(created -> created
                        ? ResponseEntity.ok().build()
                        : ResponseEntity.badRequest().build());
        
    }

    @Override
    public Mono<ResponseEntity<Void>> userTaskUpdatedEvent(
            final String userTaskId,
            final @Valid Mono<UserTaskCreatedOrUpdatedEvent> userTaskUpdatedEvent,
            final ServerWebExchange exchange) {
        
        return userTaskService
                .getUserTask(userTaskId)
                .zipWith(userTaskUpdatedEvent)
                .map(t -> userTaskMapper.toUpdatedTask(t.getT2(), t.getT1()))
                .flatMap(userTaskService::updateUserTask)
                .switchIfEmpty(userTaskCreatedMono(userTaskUpdatedEvent))
                .map(created -> created
                        ? ResponseEntity.ok().build()
                        : ResponseEntity.badRequest().build());

    }
    
    @Override
    public Mono<ResponseEntity<Void>> userTaskCompletedEvent(
            final String userTaskId,
            final @Valid Mono<UserTaskCompletedEvent> userTaskCompletedEvent,
            final ServerWebExchange exchange) {
        
        return userTaskService
                .getUserTask(userTaskId)
                .zipWith(userTaskCompletedEvent)
                .flatMap(t -> {
                    final var task = t.getT1();
                    final var completedEvent = t.getT2();
                    
                    task.setEndedAt(
                            completedEvent.getTimestamp());
                    
                    return userTaskService.completeUserTask(
                            task,
                            completedEvent.getTimestamp());
                })
                .map(completed -> completed
                        ? ResponseEntity.ok().build()
                        : ResponseEntity.badRequest().build());
        
    }
    
    @Override
    public Mono<ResponseEntity<Void>> userTaskCancelledEvent(
            final String userTaskId,
            final @Valid Mono<UserTaskCancelledEvent> userTaskCancelledEvent,
            final ServerWebExchange exchange) {

        return userTaskService
                .getUserTask(userTaskId)
                .zipWith(userTaskCancelledEvent)
                .flatMap(t -> {
                    final var task = t.getT1();
                    final var completedEvent = t.getT2();
                    
                    task.setEndedAt(
                            completedEvent.getTimestamp());
                    
                    return userTaskService.cancelUserTask(
                            task,
                            completedEvent.getTimestamp(),
                            completedEvent.getComment());
                })
                .map(completed -> completed
                        ? ResponseEntity.ok().build()
                        : ResponseEntity.badRequest().build());
        
    }

    private Mono<Boolean> workflowCreatedMono(
            final @Valid Mono<WorkflowCreatedOrUpdatedEvent> workflowCreatedEvent) {

        return workflowCreatedEvent
                .map(workflowMapper::toNewWorkflow)
                .flatMap(workflowlistService::createWorkflow);

    }

    @Override
    public Mono<ResponseEntity<Void>> workflowCreatedEvent(
            final @Valid Mono<WorkflowCreatedOrUpdatedEvent> workflowCreatedEvent,
            final ServerWebExchange exchange) {

        return workflowCreatedMono(workflowCreatedEvent)
                .map(created -> created
                        ? ResponseEntity.ok().build()
                        : ResponseEntity.badRequest().build());
    }

    @Override
    public Mono<ResponseEntity<Void>> workflowCancelledEvent(String workflowId, Mono<WorkflowCancelledEvent> workflowCancelledEvent, ServerWebExchange exchange) {
        return workflowlistService
                .getWorkflow(workflowId)
                .zipWith(workflowCancelledEvent)
                .flatMap(t -> {
                    final var workflow = t.getT1();
                    final var completedEvent = t.getT2();

                    workflow.setEndedAt(
                            completedEvent.getTimestamp());

                    return workflowlistService.cancelWorkflow(
                            workflow,
                            completedEvent.getTimestamp(),
                            completedEvent.getComment());
                })
                .map(completed -> completed
                        ? ResponseEntity.ok().build()
                        : ResponseEntity.badRequest().build());
    }

    @Override
    public Mono<ResponseEntity<Void>> workflowCompletedEvent(
            final String workflowId,
            final Mono<WorkflowCompletedEvent> workflowCompletedEvent,
            final ServerWebExchange exchange) {

        return workflowlistService
                .getWorkflow(workflowId)
                .zipWith(workflowCompletedEvent)
                .flatMap(t -> {
                    final var task = t.getT1();
                    final var completedEvent = t.getT2();

                    task.setEndedAt(
                            completedEvent.getTimestamp());

                    return workflowlistService.completeWorkflow(
                            task,
                            completedEvent.getTimestamp());
                })
                .map(completed -> completed
                        ? ResponseEntity.ok().build()
                        : ResponseEntity.badRequest().build());
        
    }


    @Override
    public Mono<ResponseEntity<Void>> workflowUpdatedEvent(
            final String workflowId,
            final Mono<WorkflowCreatedOrUpdatedEvent> workflowUpdatedEvent,
            final ServerWebExchange exchange) {
        
        return workflowlistService
                .getWorkflow(workflowId)
                .zipWith(workflowUpdatedEvent)
                .map(t -> workflowMapper.toUpdatedWorkflow(t.getT2(), t.getT1()))
                .flatMap(workflowlistService::updateWorkflow)
                .switchIfEmpty(workflowCreatedMono(workflowUpdatedEvent))
                .map(created -> created
                        ? ResponseEntity.ok().build()
                        : ResponseEntity.badRequest().build());
        
    }

    @Override
    public Mono<ResponseEntity<Void>> registerWorkflowModule(
            final String id,
            final Mono<RegisterWorkflowModuleEvent> registerWorkflowModuleEvent,
            final ServerWebExchange exchange) {

        return registerWorkflowModuleEvent
                .flatMap(event -> workflowModuleService.registerOrUpdateWorkflowModule(
                        id,
                        event.getUri(),
                        event.getTaskProviderApiUriPath(),
                        event.getWorkflowProviderApiUriPath()))
                .map(module -> ResponseEntity.ok().<Void>build());

    }

}
