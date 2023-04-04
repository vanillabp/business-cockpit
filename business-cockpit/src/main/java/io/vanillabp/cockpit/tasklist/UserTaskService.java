package io.vanillabp.cockpit.tasklist;

import java.util.Collection;
import java.util.stream.Collectors;

import org.bson.Document;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.messaging.Message;
import org.springframework.data.mongodb.core.messaging.Subscription;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mongodb.client.model.changestream.ChangeStreamDocument;

import io.vanillabp.cockpit.commons.mongo.ChangeStreamUtils;
import io.vanillabp.cockpit.commons.mongo.OptimisticLockingUtils;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.tasklist.model.UserTaskRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
@Transactional
public class UserTaskService {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;
    
    @Autowired
    private ChangeStreamUtils changeStreamUtils;
    
    @Autowired
    private UserTaskRepository userTasks;
    
    private Subscription userTasksChangedSubscription;
    
    @PostConstruct
    public void subscribeToDbChanges() {
        
        userTasksChangedSubscription = changeStreamUtils.subscribe(
                UserTask.class,
                this::processUserTaskRepositoryChanged);
        
    }
    
    @PreDestroy
    public void unsubscribeFromDbChanges() {
        
        changeStreamUtils
                .unsubscribe(userTasksChangedSubscription);
        
    }
    
    private void processUserTaskRepositoryChanged(
            final Message<ChangeStreamDocument<Document>, UserTask> message) {
        
        applicationEventPublisher.publishEvent(
                new UserTaskChangedNotification(message));
        
    }
    
    public Page<UserTask> getUserTasks(
			final int pageNumber,
			final int pageSize) {
    	
    	return userTasks.findAll(
    			PageRequest
    					.ofSize(pageSize)
    					.withPage(pageNumber)
    					.withSort(Direction.DESC, "createdAt"));
    	
    }
    
    public Page<UserTask> getUserTasksUpdated(
            final int size,
            final Collection<String> updatedTasksIds) {
        
        final var tasks = userTasks.findAllIds(
                PageRequest
                        .ofSize(size)
                        .withPage(0)
                        .withSort(Direction.DESC, "createdAt"));
        
        return new PageImpl<UserTask>(
                tasks
                        .stream()
                        .map(task -> {
                            if (updatedTasksIds.contains(task.getId())) {
                                return userTasks.findById(task.getId()).get();
                            } else {
                                return task;
                            }
                        })
                        .collect(Collectors.toList()),
                Pageable
                        .ofSize(tasks.size())
                        .withPage(0),
                userTasks.count());
        
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
