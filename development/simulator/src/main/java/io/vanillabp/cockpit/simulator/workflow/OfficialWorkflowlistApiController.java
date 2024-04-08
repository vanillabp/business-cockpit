package io.vanillabp.cockpit.simulator.workflow;

import com.devskiller.jfairy.Fairy;
import io.vanillabp.cockpit.gui.api.v1.OfficialWorkflowlistApi;
import io.vanillabp.cockpit.gui.api.v1.UserTask;
import io.vanillabp.cockpit.gui.api.v1.UserTaskRetrieveMode;
import io.vanillabp.cockpit.gui.api.v1.UserTasksRequest;
import io.vanillabp.cockpit.gui.api.v1.Workflow;
import io.vanillabp.cockpit.simulator.common.FairyHelper;
import io.vanillabp.cockpit.simulator.usertask.OfficialTasklistApiMapper;
import io.vanillabp.cockpit.simulator.usertask.testdata.UserTaskTestDataGenerator;
import io.vanillabp.cockpit.simulator.workflow.testdata.WorkflowTestDataGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping(path = "/official-api/v1")
public class OfficialWorkflowlistApiController implements OfficialWorkflowlistApi {
    
    private Map<String, Workflow> workflows = new HashMap<>();
    
    private Map<String, List<UserTask>> userTasks = new HashMap<>();

    private final static Random random = new Random(System.currentTimeMillis());
    
    private final static Map<String, Fairy> fairies = new HashMap<>();
    
    static {
        fairies.put("de", FairyHelper.buildFairy("de"));
        fairies.put("en", FairyHelper.buildFairy("en"));
    }

    @Autowired
    private OfficialWorkflowlistApiMapper mapper;
    
    @Autowired
    private OfficialTasklistApiMapper userTaskMapper;
    
    @Override
    public ResponseEntity<Workflow> getWorkflow(
            final String workflowId) {
        
        final var existingWorkflow = workflows.get(workflowId);
        if (existingWorkflow != null) {
            return ResponseEntity.ok(existingWorkflow);
        }
        
        final var createdEvent = WorkflowTestDataGenerator
                .buildCreatedEvent(random, fairies);
        
        final var result = mapper.toApi(createdEvent);
        result.setId(workflowId);
        
        workflows.put(workflowId, result);
        
        return ResponseEntity.ok(result);
        
    }

    @Override
    public ResponseEntity<List<UserTask>> getUserTasksOfWorkflow(
            final String workflowId,
            final Boolean llatcup,
            final UserTasksRequest userTasksRequest) {

        final var existingUserTasks = userTasks.get(workflowId);
        if (existingUserTasks != null) {
            return ResponseEntity.ok(existingUserTasks);
        }
        
        final var result = new LinkedList<UserTask>();
        userTasks.put(workflowId, result);
        
        final var num = random.nextInt(userTasksRequest.getMode() ==  UserTaskRetrieveMode.OPENTASKS ? 5 : 10);
        String businessId = null;
        for (int i = 0; i < num; ++i) {
            
            final var createdEvent = UserTaskTestDataGenerator
                    .buildCreatedEvent(random, fairies, null, null, null);
            createdEvent.setWorkflowId(workflowId);
            if (businessId == null) {
                businessId = createdEvent.getBusinessId();
            } else {
                createdEvent.setBusinessId(businessId);
            }
            result.add(userTaskMapper.toApi(createdEvent));
            
        }
        
        return ResponseEntity.ok(result);
        
    }
    
}
