package io.vanillabp.cockpit.simulator.usertask;

import com.devskiller.jfairy.Fairy;
import io.vanillabp.cockpit.gui.api.v1.OfficialTasklistApi;
import io.vanillabp.cockpit.gui.api.v1.UserTask;
import io.vanillabp.cockpit.simulator.common.FairyHelper;
import io.vanillabp.cockpit.simulator.usertask.testdata.UserTaskTestDataGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping(path = "/official-api/v1")
public class OfficialTasklistApiController implements OfficialTasklistApi {
    
    private Map<String, UserTask> userTasks = new HashMap<>();
    
    private final static Random random = new Random(System.currentTimeMillis());
    
    private final static Map<String, Fairy> fairies = new HashMap<>();
    
    static {
        fairies.put("de", FairyHelper.buildFairy("de"));
        fairies.put("en", FairyHelper.buildFairy("en"));
    }

    @Autowired
    private OfficialTasklistApiMapper mapper;
    
    @Override
    public ResponseEntity<UserTask> getUserTask(
            final String userTaskId,
            final Boolean markAsRead) {
        
        final var existingUserTask = userTasks.get(userTaskId);
        if (existingUserTask != null) {
            return ResponseEntity.ok(existingUserTask);
        }
        
        final var createdEvent = UserTaskTestDataGenerator
                .buildCreatedEvent(random, fairies, null, null, null);
        
        final var result = mapper.toApi(createdEvent);
        result.setId(userTaskId);
        
        userTasks.put(userTaskId, result);
        
        return ResponseEntity.ok(result);
        
    }
    
}
