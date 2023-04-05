package io.vanillabp.cockpit.tasklist;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bson.Document;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
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

    private static final Sort DEFAULT_SORT =
            Sort.by(Order.asc("dueDate").nullsLast())
            .and(Sort.by("createdAt").ascending());
    
    @Autowired
    private Logger logger;
    
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
                UserTaskChangedNotification.build(message));
        
    }
    
    public Optional<UserTask> getUserTask(
            final String userTaskId) {
        
        return userTasks.findById(userTaskId);
        
    }
    
    public Page<UserTask> getUserTasks(
			final int pageNumber,
			final int pageSize) {
    	
    	return userTasks.findAll(
    			PageRequest
    					.ofSize(pageSize)
    					.withPage(pageNumber)
    					.withSort(Sort.by(Order.asc("dueDate").nullsFirst())));
    	
    }
    
    public Page<UserTask> getUserTasksUpdated(
            final int size,
            final Collection<String> knownUserTasksIds) {
        
        final var tasks = userTasks.findAllIds(
                PageRequest
                        .ofSize(size)
                        .withPage(0)
                        .withSort(DEFAULT_SORT));
        
        return new PageImpl<UserTask>(
                tasks
                        .stream()
                        .map(task -> {
                            if (knownUserTasksIds.contains(task.getId())) {
                                return task;
                            } else {
                                return userTasks.findById(task.getId()).get();
                            }
                        })
                        .collect(Collectors.toList()),
                Pageable
                        .ofSize(tasks.isEmpty() ? 1 : tasks.size())
                        .withPage(0),
                userTasks.count());
        
    }
    
    public boolean createUserTask(
            final UserTask userTask) {
        
        if (userTask.getDueDate() == null) {
            // for correct sorting
            userTask.setDueDate(OffsetDateTime.MAX);
        }
        
        try {
            userTasks.save(userTask);
            return true;
        } catch (final DuplicateKeyException | OptimisticLockingFailureException e) {
            return false;
        }
        
    }

    public boolean updateUserTask(
            final UserTask userTask) {
        
        try {
            
            return OptimisticLockingUtils.doWithRetries(
                    () -> {
                        userTasks.save(userTask);
                        return true;
                    });
            
        } catch (OptimisticLockingFailureException e) {
            
            logger.warn("Could not update usertask '{}'!", userTask.getId(), e);
            return false;
            
        }
        
    }
    
}
