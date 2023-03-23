package io.vanillabp.cockpit.api.v1;

import io.vanillabp.cockpit.bpms.api.v1.BpmsApi;
import io.vanillabp.cockpit.bpms.api.v1.UserTaskCreatedEvent;
import io.vanillabp.cockpit.uerstasks.UserTaskService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1")
public class BpmsApiController implements BpmsApi {

    @Autowired
    private UserTaskMapper userTaskMapper;
    
    @Autowired
    private UserTaskService userTaskService;
    
    @Override
    public ResponseEntity<Void> processUserTaskCreatedEvent(
            @Valid UserTaskCreatedEvent userTaskCreatedEvent) {
        
        final var created = userTaskService
                .userTaskCreated(
                        userTaskMapper.toModel(userTaskCreatedEvent));
        
        if (created) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().build();
        }
        
    }
    
}
