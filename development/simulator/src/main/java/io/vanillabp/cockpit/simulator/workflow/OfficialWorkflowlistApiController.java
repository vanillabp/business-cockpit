package io.vanillabp.cockpit.simulator.workflow;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devskiller.jfairy.Fairy;

import io.vanillabp.cockpit.gui.api.v1.OfficialWorkflowlistApi;
import io.vanillabp.cockpit.gui.api.v1.Workflow;
import io.vanillabp.cockpit.simulator.common.FairyHelper;
import io.vanillabp.cockpit.simulator.workflow.testdata.WorkflowTestDataGenerator;

@RestController
@RequestMapping(path = "/official-api/v1")
public class OfficialWorkflowlistApiController implements OfficialWorkflowlistApi {
    
    private Map<String, Workflow> workflows = new HashMap<>();
    
    private final static Random random = new Random(System.currentTimeMillis());
    
    private final static Map<String, Fairy> fairies = new HashMap<>();
    
    static {
        fairies.put("de", FairyHelper.buildFairy("de"));
        fairies.put("en", FairyHelper.buildFairy("en"));
    }

    @Autowired
    private OfficialWorkflowlistApiMapper mapper;
    
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
    
}
