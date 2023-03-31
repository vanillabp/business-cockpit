package io.vanillabp.cockpit.tasklist;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.vanillabp.cockpit.commons.mongo.OptimisticLockingUtils;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.tasklist.model.UserTaskRepository;

@Service
@Transactional
public class UserTaskService {

    @Autowired
    private UserTaskRepository userTasks;
    
    public Page<UserTask> getUserTasks(
			final Integer pageNumber,
			final Integer pageSize) {
    	
    	return userTasks.findAll(
    			PageRequest
    					.ofSize(pageSize)
    					.withPage(pageNumber)
    					.withSort(Direction.DESC, "timestamp"));
    	
    }
    
    public boolean processEvent_UserTaskCreated(
            final UserTask userTask) {
        
        try {
            LoggerFactory.getLogger(UserTaskService.class).info("{}", userTask.getId());
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
