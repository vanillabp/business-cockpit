package io.vanillabp.cockpit.tasklist;

import io.vanillabp.cockpit.commons.mongo.changestreams.ReactiveChangeStreamUtils;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.tasklist.model.UserTaskRepository;
import io.vanillabp.cockpit.util.microserviceproxy.MicroserviceProxyRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.stream.Collectors;

@Service
public class UserTaskService {

    private static final Sort DEFAULT_SORT =
            Sort.by(Order.asc("dueDate").nullsLast())
            .and(Sort.by("createdAt").ascending())
            .and(Sort.by("id").ascending());
    
    @Autowired
    private Logger logger;
    
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;
    
    @Autowired
    private ReactiveChangeStreamUtils changeStreamUtils;
    
    @Autowired
    private UserTaskRepository userTasks;
    
    @Autowired
    private MicroserviceProxyRegistry microserviceProxyRegistry;
    
    private Disposable dbChangesSubscription;
    
    @PostConstruct
    public void subscribeToDbChanges() {
        
        dbChangesSubscription = changeStreamUtils
                .subscribe(UserTask.class)
                .flatMapSequential(userTask -> Mono
                        .fromCallable(() -> UserTaskChangedNotification.build(userTask))
                        .doOnError(e -> logger
                                .warn("Error on processing user-task change-stream "
                                        + "event! Will resume stream.", e))
                        .onErrorResume(Exception.class, e -> Mono.empty()))
                .doOnNext(applicationEventPublisher::publishEvent)
                .subscribe();
        
        // register all URLs already known
        userTasks
                .findAllWorkflowModulesAndUris()
                .collectList()
                .map(modulesAndUris -> modulesAndUris
                        .stream()
                        .collect(Collectors.toMap(
                                UserTask::getWorkflowModule,
                                UserTask::getWorkflowModuleUri)))
                .doOnNext(microserviceProxyRegistry::registerMicroservices)
                .subscribe();

    }
    
    @PreDestroy
    public void cleanup() {
        
        dbChangesSubscription.dispose();
        
    }
    
    public Mono<UserTask> getUserTask(
            final String userTaskId) {
        
        return userTasks.findById(userTaskId);
        
    }

    public Mono<UserTask> markAsRead(
            final String userTaskId,
            final String userId) {

        return getUserTask(userTaskId)
                .flatMap(userTask -> {
                    userTask.setReadAt(userId);
                    return userTasks.save(userTask);
                });

    }

    public Mono<UserTask> markAsUnread(
            final String userTaskId,
            final String userId) {

        return getUserTask(userTaskId)
                .flatMap(userTask -> {
                    userTask.clearReadAt(userId);
                    return userTasks.save(userTask);
                });

    }

    public Mono<Page<UserTask>> getUserTasks(
			final int pageNumber,
			final int pageSize,
			final OffsetDateTime initialTimestamp) {
    	
        final var pageRequest = PageRequest
                .ofSize(pageSize)
                .withPage(pageNumber)
                .withSort(DEFAULT_SORT);
        
        final var endedSince = (initialTimestamp != null
                ? initialTimestamp
                : OffsetDateTime.now()).toInstant();
        
    	return userTasks
    	        .findActive(endedSince, pageRequest)
    	        .collectList()
    	        .zipWith(userTasks.countActive(endedSince))
    	        .map(t -> new PageImpl<>(t.getT1(), pageRequest, t.getT2()));
    	
    }
    
    public Flux<UserTask> getUserTasksOfWorkflow(
            final boolean activeOnly,
            final String workflowId) {
        
        if (activeOnly) {
            return userTasks
                    .findActiveByWorkflowId(workflowId);
        }
        
        return userTasks
                .findAllByWorkflowId(workflowId);
        
    }
    
    public Mono<Page<UserTask>> getUserTasksUpdated(
            final int size,
            final Collection<String> knownUserTasksIds,
            final OffsetDateTime initialTimestamp) {
        
        final var pageRequest = PageRequest
                .ofSize(size)
                .withPage(0)
                .withSort(DEFAULT_SORT);
        
        final var endedSince = initialTimestamp.toInstant();
        
        final var tasks = userTasks.findIdsOfActive(
                endedSince,
                pageRequest);
        
        return tasks
                .flatMapSequential(task -> {
                    if (knownUserTasksIds.contains(task.getId())) {
                        return Mono.just(task);
                    }
                    return userTasks.findById(task.getId());
                })
                .collectList()
                .zipWith(userTasks.countActive(endedSince))
                .map(t -> new PageImpl<>(
                        t.getT1(),
                        Pageable
                                .ofSize(t.getT1().isEmpty() ? 1 : t.getT1().size())
                                .withPage(0),
                        t.getT2()));
        
    }
    
    public Mono<Boolean> completeUserTask(
            final UserTask userTask,
            final OffsetDateTime timestamp) {
        
        if (userTask == null) {
            Mono.just(Boolean.FALSE);
        }
        
        userTask.setEndedAt(timestamp);
        
        return userTasks
                .save(userTask)
                .map(task -> Boolean.TRUE)
                .onErrorResume(e -> {
                    logger.error("Could not save user task '{}'!",
                            userTask.getId(),
                            e);
                    return Mono.just(Boolean.FALSE);
                });        
    }

    public Mono<Boolean> cancelUserTask(
            final UserTask userTask,
            final OffsetDateTime timestamp,
            final String reason) {
        
        if (userTask == null) {
            Mono.just(Boolean.FALSE);
        }

        userTask.setEndedAt(timestamp);
        userTask.setComment(reason);

        return userTasks
                .save(userTask)
                .map(task -> Boolean.TRUE)
                .onErrorResume(e -> {
                    logger.error("Could not save user task '{}'!",
                            userTask.getId(),
                            e);
                    return Mono.just(Boolean.FALSE);
                });
        
    }

    public Mono<Boolean> createUserTask(
            final UserTask userTask) {
        
        if (userTask == null) {
            Mono.just(Boolean.FALSE);
        }
        
        if (userTask.getDueDate() == null) {
            // for correct sorting
            userTask.setDueDate(OffsetDateTime.MAX);
        }
        
        return userTasks
                .save(userTask)
                .doOnNext(task -> microserviceProxyRegistry
                        .registerMicroservice(
                                task.getWorkflowModule(),
                                task.getWorkflowModuleUri()))
                .map(task -> Boolean.TRUE)
                .onErrorResume(e -> {
                    logger.error("Could not save user task '{}'!",
                            userTask.getId(),
                            e);
                    return Mono.just(Boolean.FALSE);
                });
                
    }

    public Mono<Boolean> updateUserTask(
            final UserTask userTask) {
        
        if (userTask == null) {
            return Mono.just(Boolean.FALSE);
        }
        
        return userTasks
                .save(userTask)
                .onErrorMap(e -> {
                    logger.error("Could not save user task '{}'!",
                            userTask.getId(),
                            e);
                    return null;
                })
                .map(savedTask -> savedTask != null);
        
    }
    
}
