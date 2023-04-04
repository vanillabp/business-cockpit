package io.vanillabp.cockpit.bpms.api.v1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.vanillabp.cockpit.bpms.WebSecurityConfiguration;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import jakarta.validation.Valid;

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
    public ResponseEntity<Void> userTaskCreatedEvent(
            final @Valid UserTaskCreatedEvent userTaskCreatedEvent) {
        
        final var created = userTaskService.createUserTask(
                userTaskMapper.toModel(userTaskCreatedEvent));
        
        if (created) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().build();
        }
        
    }

    @Override
    public ResponseEntity<Void> userTaskUpdatedEvent(
            final String userTaskId,
            final @Valid UserTaskUpdatedEvent userTaskUpdatedEvent) {
        
        
        final var userTaskFound = userTaskService.getUserTask(userTaskId);
        if (userTaskFound.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        final var existingUserTask = userTaskFound.get();

        // update modifiable properties
        existingUserTask.setDueDate(userTaskUpdatedEvent.getDueDate());

        final var updated = userTaskService.updateUserTask(
                existingUserTask);
        
        if (updated) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().build();
        }

    }
    
    @Override
    public ResponseEntity<Void> userTaskCompletedEvent(
            final String userTaskId,
            final @Valid UserTaskLifecycleEvent userTaskLifecycleEvent) {
        // TODO Auto-generated method stub
        return BpmsApi.super.userTaskCompletedEvent(userTaskId, userTaskLifecycleEvent);
    }
    
    @Override
    public ResponseEntity<Void> userTaskCancelledEvent(
            final String userTaskId,
            final @Valid UserTaskLifecycleEvent userTaskLifecycleEvent) {
        // TODO Auto-generated method stub
        return BpmsApi.super.userTaskCancelledEvent(userTaskId, userTaskLifecycleEvent);
    }
    
    @Override
    public ResponseEntity<Void> userTaskSuspendedEvent(
            final String userTaskId,
            final @Valid UserTaskLifecycleEvent userTaskLifecycleEvent) {
        // TODO Auto-generated method stub
        return BpmsApi.super.userTaskSuspendedEvent(userTaskId, userTaskLifecycleEvent);
    }
    
    @Override
    public ResponseEntity<Void> userTaskActivatedEvent(
            final String userTaskId,
            final @Valid UserTaskLifecycleEvent userTaskLifecycleEvent) {
        // TODO Auto-generated method stub
        return BpmsApi.super.userTaskActivatedEvent(userTaskId, userTaskLifecycleEvent);
    }
    
}
