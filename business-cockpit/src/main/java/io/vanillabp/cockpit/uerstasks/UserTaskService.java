package io.vanillabp.cockpit.uerstasks;

import io.vanillabp.cockpit.commons.mongo.OptimisticLockingUtils;
import io.vanillabp.cockpit.uerstasks.model.UserTask;
import io.vanillabp.cockpit.uerstasks.model.UserTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserTaskService {

    @Autowired
    private UserTaskRepository userTasks;
    
    public boolean userTaskCreated(
            final UserTask userTask) {
        
        try {
            userTasks.save(userTask);
            return true;
        } catch (final DuplicateKeyException | OptimisticLockingFailureException e) {
            return userTaskUpdated(userTask);
        }
        
    }
    
    public boolean userTaskUpdated(
            final UserTask userTask) {
        
        return OptimisticLockingUtils.doWithRetries(
                () -> {
                    final var userTaskFound = userTasks.findById(userTask.getId());
                    if (userTaskFound.isEmpty()) {
                        return false;
                    }
                    final var existingUserTask = userTaskFound.get();

                    // update modifiable properties
                    existingUserTask.setDueDate(userTask.getDueDate());
                    
                    userTasks.save(existingUserTask);
                    
                    return true;
                });
        
    }
    
}
