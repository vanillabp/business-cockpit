package io.vanillabp.cockpit.tasklist;

import io.vanillabp.cockpit.commons.mongo.changestreams.ReactiveChangeStreamUtils;
import io.vanillabp.cockpit.config.properties.ApplicationProperties;
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
import org.springframework.util.StringUtils;
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

    private ApplicationProperties properties;
    
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

    public Flux<UserTask> markAsRead(
            final Collection<String> userTaskIds,
            final String userId) {

        return userTasks.saveAll(
                userTasks
                        .findAllById(userTaskIds)
                        .map(userTask -> {
                            userTask.setReadAt(userId);
                            return userTask;
                        }));

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

    public Flux<UserTask> markAsUnread(
            final Collection<String> userTaskIds,
            final String userId) {

        return userTasks.saveAll(
                userTasks
                        .findAllById(userTaskIds)
                        .map(userTask -> {
                            userTask.clearReadAt(userId);
                            return userTask;
                        }));

    }

    public Mono<UserTask> assignTask(
            final String userTaskId,
            final String userId) {

        return getUserTask(userTaskId)
                .flatMap(userTask -> {
                    userTask.addCandidateUser(userId);
                    return userTasks.save(userTask);
                });

    }

    public Flux<UserTask> assignTask(
            final Collection<String> userTaskIds,
            final String userId) {

        return userTasks.saveAll(
                userTasks
                        .findAllById(userTaskIds)
                        .map(userTask -> {
                            userTask.addCandidateUser(userId);
                            return userTask;
                        }));

    }

    public Mono<UserTask> unassignTask(
            final String userTaskId,
            final String userId) {

        return getUserTask(userTaskId)
                .flatMap(userTask -> {
                    userTask.removeCandidateUser(userId);
                    return userTasks.save(userTask);
                });

    }

    public Flux<UserTask> unassignTask(
            final Collection<String> userTaskIds,
            final String userId) {

        return userTasks.saveAll(
                userTasks
                        .findAllById(userTaskIds)
                        .map(userTask -> {
                            userTask.removeCandidateUser(userId);
                            return userTask;
                        }));

    }

    public Mono<UserTask> claimTask(
            final String userTaskId,
            final String userId) {

        return getUserTask(userTaskId)
                .flatMap(userTask -> {
                    if (StringUtils.hasText(userId)
                            && !userTask.getAssignee().equals(userId)) {
                        userTask.setAssignee(userId);
                        return userTasks.save(userTask);
                    }
                    return Mono.just(userTask);
                });

    }

    public Flux<UserTask> claimTask(
            final Collection<String> userTaskIds,
            final String userId) {

        if (!StringUtils.hasText(userId)) {
            return Flux.empty();
        }
        return userTasks.saveAll(
                userTasks
                        .findAllById(userTaskIds)
                        .map(userTask -> {
                            userTask.setAssignee(userId);
                            return userTask;
                        }));

    }

    public Mono<UserTask> unclaimTask(
            final String userTaskId,
            final String userId) {

        final var userIdGiven = StringUtils.hasText(userId);
        return getUserTask(userTaskId)
                .flatMap(userTask -> {
                    if (!userIdGiven
                            || ((userTask.getAssignee() != null) && userTask.getAssignee().equals(userId))) {
                        userTask.setAssignee(null);
                        return userTasks.save(userTask);
                    }
                    return Mono.just(userTask);
                });

    }

    public Flux<UserTask> unclaimTask(
            final Collection<String> userTaskIds,
            final String userId) {

        return userTasks.saveAll(
                userTasks
                        .findAllById(userTaskIds)
                        .filter(userTask -> !StringUtils.hasText(userId)
                                || userTask.getAssignee().equals(userId))
                        .map(userTask -> {
                            userTask.setAssignee(null);
                            return userTask;
                        }));

    }

    public Mono<Page<UserTask>> getUserTasks(
            final Collection<String> assignees,
            final Collection<String> candidateUsers,
            final Collection<String> candidateGroups,
			final int pageNumber,
			final int pageSize,
			final OffsetDateTime initialTimestamp) {
    	
        final var pageRequest = PageRequest
                .ofSize(pageSize)
                .withPage(pageNumber)
                .withSort(DEFAULT_SORT);

        final var query = buildUserTasksQuery(
                new Query(),
                assignees,
                candidateUsers,
                candidateGroups,
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
            final Collection<String> assignees,
            final Collection<String> candidateUsers,
            final Collection<String> candidateGroups,
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
                assignees,
                candidateUsers,
                candidateGroups,
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

    public Query buildUserTasksQuery(
            final Query targetQuery,
            final Collection<String> assignees,
            final Collection<String> candidateUsers,
            final Collection<String> candidateGroups,
            final OffsetDateTime initialTimestamp,
            final RetrieveItemsMode mode) {

        final var subCriterias = new LinkedList<Criteria>();

        // honour user's permissions
        final var userRestrictions = new LinkedList<Criteria>();
        if ((assignees != null)
                && !assignees.isEmpty()) {
            final var assigneeMatches = Criteria.where("assignee").in(assignees);
            userRestrictions.add(assigneeMatches);
        }
        if ((candidateUsers != null)
                && !candidateUsers.isEmpty()) {
            final var candidateUsersMatches = Criteria.where("candidateUsers").in(candidateUsers);
            userRestrictions.add(candidateUsersMatches);
        }
        if ((candidateGroups != null)
                && !candidateGroups.isEmpty()) {
            final var candidateGroupsMatches = Criteria.where("candidateGroups").in(candidateGroups);
            userRestrictions.add(candidateGroupsMatches);
        }
        if (!userRestrictions.isEmpty()) {
            final var noAssigneeOrNoCandidate = Criteria.where("dangling").is(Boolean.TRUE);
            userRestrictions.add(noAssigneeOrNoCandidate);
            final Criteria permittedTasks = new Criteria().orOperator(userRestrictions);
            subCriterias.add(permittedTasks);
        }

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
