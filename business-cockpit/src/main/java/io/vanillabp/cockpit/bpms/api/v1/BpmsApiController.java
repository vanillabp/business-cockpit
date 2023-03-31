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
            @Valid UserTaskCreatedEvent userTaskCreatedEvent) {
        
        final var created = userTaskService
                .processEvent_UserTaskCreated(
                        userTaskMapper.toModel(userTaskCreatedEvent));
        
        if (created) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().build();
        }
        
    }
    
}
