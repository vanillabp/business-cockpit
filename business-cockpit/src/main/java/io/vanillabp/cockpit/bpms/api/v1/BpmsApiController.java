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
            final @Valid Mono<UserTaskCreatedEvent> userTaskCreatedEvent,
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
            final @Valid Mono<UserTaskUpdatedEvent> userTaskUpdatedEvent,
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
    
}
