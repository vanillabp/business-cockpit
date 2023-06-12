package io.vanillabp.cockpit.bpms.api.v1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import io.vanillabp.cockpit.bpms.WebSecurityConfiguration;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import jakarta.validation.Valid;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = BpmsApiController.BPMS_API_URL_PREFIX)
@Secured(WebSecurityConfiguration.BPMS_API_AUTHORITY)
public class BpmsApiController implements BpmsApi {

	public static final String BPMS_API_URL_PREFIX = "/bpms/api/v1";
	
    @Autowired
    private UserTaskMapper userTaskMapper;
    
    @Autowired
    private UserTaskService userTaskService;
    
    @Override
    public Mono<ResponseEntity<Void>> userTaskCreatedEvent(
            final @Valid Mono<UserTaskCreatedOrUpdatedEvent> userTaskCreatedEvent,
            final ServerWebExchange exchange) {

        return userTaskCreatedEvent
                .map(userTaskMapper::toModel)
                .flatMap(userTaskService::createUserTask)
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
                .map(t -> {
                    final var task = t.getT1();
                    final var updatedEvent = t.getT2();
                    
                    // update modifiable properties
                    task.setDueDate(updatedEvent.getDueDate());
                    
                    return task;
                })
                .flatMap(userTaskService::updateUserTask)
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
    
}
