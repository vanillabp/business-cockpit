package io.vanillabp.cockpit.tasklist;

import io.vanillabp.cockpit.commons.mongo.changestreams.ReactiveChangeStreamUtils;
import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
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
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedList;
import java.util.stream.Collectors;

@Service
public class UserTaskService {

    public static enum RetrieveItemsMode {
        All,
        OpenTasks,
        OpenTasksWithoutFollowup,
        OpenTasksWithFollowup,
        ClosedTasksOnly
    };

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

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;

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
            final UserDetails user,
			final int pageNumber,
			final int pageSize,
			final OffsetDateTime initialTimestamp) {
    	
        final var pageRequest = PageRequest
                .ofSize(pageSize)
                .withPage(pageNumber)
                .withSort(DEFAULT_SORT);

        final var query = buildUserTasksQuery(
                new Query(),
                user,
                initialTimestamp,
                RetrieveItemsMode.OpenTasks);

        final var numberOfUserTasks = mongoTemplate
                .count(Query.of(query).limit(-1).skip(-1), UserTask.class);
        final var result = mongoTemplate
                .find(query.with(pageRequest), UserTask.class);
        return Mono
                .zip(result.collectList(), numberOfUserTasks)
                .map(results -> PageableExecutionUtils.getPage(
                    results.getT1(),
                    pageRequest,
                    results::getT2));

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
            final UserDetails user,
            final int size,
            final Collection<String> knownUserTasksIds,
            final OffsetDateTime initialTimestamp) {
        
        final var pageRequest = PageRequest
                .ofSize(size)
                .withPage(0)
                .withSort(DEFAULT_SORT);
        final var query = new Query();
        query.fields().include("_id");

        buildUserTasksQuery(
                query,
                user,
                initialTimestamp,
                RetrieveItemsMode.OpenTasks);

        final var numberOfUserTasks = mongoTemplate
                .count(Query.of(query).limit(-1).skip(-1), UserTask.class);
        final var result = mongoTemplate
                .find(query.with(pageRequest), UserTask.class);

        return result
                .flatMapSequential(task -> {
                    if (knownUserTasksIds.contains(task.getId())) {
                        return Mono.just(task);
                    }
                    return userTasks.findById(task.getId());
                })
                .collectList()
                .zipWith(numberOfUserTasks)
                .map(results -> new PageImpl<>(
                        results.getT1(),
                        Pageable
                                .ofSize(results.getT1().isEmpty() ? 1 : results.getT1().size())
                                .withPage(0),
                        results.getT2()));
        
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

    private Query buildUserTasksQuery(
            final Query targetQuery,
            final UserDetails user,
            final OffsetDateTime initialTimestamp,
            final RetrieveItemsMode mode) {

        final var subCriterias = new LinkedList<Criteria>();

        // honour user's permissions
        final var assigneeMatches = Criteria.where("assignee").is(user.getId());
        final var candidateUsersMatches = Criteria.where("candidateUsers").in(user.getId());
        final var candidateGroupsMatches = Criteria.where("candidateGroups").in(user.getAuthorities());
        final var noAssigneeOrNoCandidate = Criteria.where("dangling").is(Boolean.TRUE);
        final Criteria permittedTasks = new Criteria()
                .orOperator(noAssigneeOrNoCandidate, assigneeMatches, candidateUsersMatches, candidateGroupsMatches);
        subCriterias.add(permittedTasks);

        // limit result according to list mode

        // return consistent results across multiple requests of pages
        switch (mode) {
            case All:
            case OpenTasks:
            case OpenTasksWithFollowup:
            case OpenTasksWithoutFollowup:
                subCriterias.add(new Criteria().orOperator(
                        Criteria.where("endedAt").exists(false),
                        Criteria.where("endedAt").gte(initialTimestamp)));
                break;
            case ClosedTasksOnly:
                subCriterias.add(new Criteria().orOperator(
                        Criteria.where("endedAt").exists(true),
                        Criteria.where("endedAt").lt(initialTimestamp)));
                break;
            default:
                throw new RuntimeException("Unsupported mode '"
                        + mode
                        + "'! Did you forget to extend this switch instruction?");
        }

        // take followup-date into account
        switch (mode) {
            case OpenTasksWithFollowup: {
                final Criteria inFuture = Criteria.where("followupDate").gt(initialTimestamp);
                subCriterias.add(inFuture);
                break;
            }
            case OpenTasksWithoutFollowup: {
                final Criteria notSet = Criteria.where("followupDate").exists(false);
                final Criteria inPast = Criteria.where("followupDate").lte(OffsetDateTime.now());
                final Criteria excludeFollowUps = new Criteria().orOperator(notSet, inPast);
                subCriterias.add(excludeFollowUps);
                break;
            }
        }

        targetQuery.addCriteria(
                new Criteria().andOperator(subCriterias));

        return targetQuery;

    }

}
