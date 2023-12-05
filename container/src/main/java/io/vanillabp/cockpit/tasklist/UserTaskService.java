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
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class UserTaskService {

    public static final String INDEX_CUSTOM_SORT_PREFIX = "_sort_";

    public static enum RetrieveItemsMode {
        All,
        OpenTasks,
        OpenTasksWithoutFollowup,
        OpenTasksWithFollowup,
        ClosedTasksOnly
    };

    private static final List<Sort.Order> DEFAULT_ORDER_ASC = List.of(
                    Order.asc("dueDate").nullsLast(),
                    Order.asc("createdAt"),
                    Order.asc("id")
            );
    private static final List<Sort.Order> DEFAULT_ORDER_DESC= List.of(
                    Order.desc("dueDate").nullsLast(),
                    Order.desc("createdAt"),
                    Order.desc("id")
            );

    private static final Set<String> sortAndFilterIndexes = new HashSet<>();
    private static final ReadWriteLock sortAndFilterIndexesLock = new ReentrantReadWriteLock();
    private static final Lock sortAndFilterIndexWriteLock = sortAndFilterIndexesLock.writeLock();
    private static final Lock sortAndFilterIndexReadLock = sortAndFilterIndexesLock.readLock();

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

        try {
            sortAndFilterIndexWriteLock.lock();
            mongoTemplate
                    .indexOps(UserTask.COLLECTION_NAME)
                    .getIndexInfo()
                    .filter(info -> info.getName().startsWith(INDEX_CUSTOM_SORT_PREFIX))
                    .map(info -> info.getIndexFields().get(0).getKey())
                    .subscribe(field -> {
                        logger.info("Read previously created sort/filter index for '{}'", field);
                        sortAndFilterIndexes.add(field);
                    });
        } finally {
            sortAndFilterIndexWriteLock.unlock();
        }

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
                                || ((userTask.getAssignee() != null) && userTask.getAssignee().equals(userId)))
                        .map(userTask -> {
                            userTask.setAssignee(null);
                            return userTask;
                        }));

    }

    private static record UserTaskListOrder(List<Order> order, List<String> toBeIndexed) {}

    private UserTaskListOrder getUserTaskListOrder(
            final String sort,
            final boolean sortAscending) {

        List<Order> order = sortAscending ? DEFAULT_ORDER_ASC : DEFAULT_ORDER_DESC;
        final List<String> nonDefaultSort;
        if ((sort != null)
                && !sort.equals("dueDate")) {
            nonDefaultSort = new LinkedList<>();
            final var newOrder = new LinkedList<Order>();
            Arrays
                    .stream(sort.split(",")) // maybe something like 'title.de,title.en' or just simply 'assignee'
                    .peek(nonDefaultSort::add)
                    .map(languageBasedSort -> sortAscending
                            ? Order.asc(languageBasedSort)
                            : Order.desc(languageBasedSort))
                    .forEach(newOrder::add);
            newOrder.addAll(order); // default order
            order = newOrder;
        } else {
            nonDefaultSort = null;
        }
        return new UserTaskListOrder(order, nonDefaultSort);

    }

    public Mono<Page<UserTask>> getUserTasks(
            final boolean includeDanglingTasks,
            final boolean notInAssignees,
            final Collection<String> assignees,
            final Collection<String> candidateUsers,
            final Collection<String> candidateGroups,
			final int pageNumber,
			final int pageSize,
			final OffsetDateTime initialTimestamp,
            final String sort,
            final boolean sortAscending) {

        final var orderBySort = getUserTaskListOrder(sort, sortAscending);
        final var pageRequest = PageRequest
                .ofSize(pageSize)
                .withPage(pageNumber)
                .withSort(Sort.by(orderBySort.order()));

        // build query
        final var query = buildUserTasksQuery(
                Query::new,
                includeDanglingTasks,
                notInAssignees,
                assignees,
                candidateUsers,
                candidateGroups,
                initialTimestamp,
                RetrieveItemsMode.OpenTasks,
                null);

        // prepare to retrieve data on execution
        final var numberOfUserTasksFound = mongoTemplate
                .count(Query.of(query).limit(-1).skip(-1), UserTask.class);
        final var userTasksFound = mongoTemplate
                .find(query.with(pageRequest), UserTask.class);
        final var result = Mono
                .zip(userTasksFound.collectList(), numberOfUserTasksFound)
                .map(results -> PageableExecutionUtils.getPage(
                    results.getT1(),
                    pageRequest,
                    results::getT2));

        // build index before retrieving data if necessary
        if (orderBySort.toBeIndexed() == null) {
            return result;
        }
        try {
            sortAndFilterIndexReadLock.lock();
            if (sortAndFilterIndexes.contains(sort)) {
                return result;
            }
        } finally {
            sortAndFilterIndexReadLock.unlock();
        }

        try {
            sortAndFilterIndexWriteLock.lock();
            if (sortAndFilterIndexes.contains(sort)) {
                return result;
            }
            final var newIndex = new Index();
            orderBySort
                    .toBeIndexed()
                    .forEach(languageSort -> newIndex.on(languageSort, Sort.Direction.ASC));
            newIndex.on("dueDate", Sort.Direction.ASC)
                    .on("createdAt", Sort.Direction.ASC)
                    .on("_id", Sort.Direction.ASC)
                    .named(INDEX_CUSTOM_SORT_PREFIX + sort);
            return mongoTemplate
                    .indexOps(UserTask.COLLECTION_NAME)
                    .ensureIndex(newIndex)
                    .flatMap(unknown -> {
                        sortAndFilterIndexes.add(sort);
                        return result;
                    })
                    .onErrorResume(e -> {
                        sortAndFilterIndexes.add(sort);
                        logger.error("Could not create Mongo-DB index for sorting and filtering of tasklist", e);
                        return result;
                    });
        } finally {
            sortAndFilterIndexWriteLock.unlock();
        }

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
            final boolean includeDanglingTasks,
            final boolean notInAssignees,
            final Collection<String> assignees,
            final Collection<String> candidateUsers,
            final Collection<String> candidateGroups,
            final int size,
            final Collection<String> knownUserTasksIds,
            final OffsetDateTime initialTimestamp,
            final String sort,
            final boolean sortAscending) {

        final var orderBySort = getUserTaskListOrder(sort, sortAscending);
        final var pageRequest = PageRequest
                .ofSize(size)
                .withPage(0)
                .withSort(Sort.by(orderBySort.order()));

        final var query = buildUserTasksQuery(
                () -> {
                    final var newQuery = new Query();
                    newQuery.fields().include("_id");
                    return newQuery;
                },
                includeDanglingTasks,
                notInAssignees,
                assignees,
                candidateUsers,
                candidateGroups,
                initialTimestamp,
                RetrieveItemsMode.OpenTasks,
                null);

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
            final Supplier<Query> querySupplier,
            final boolean includeDanglingTasks,
            final boolean notInAssignees,
            final Collection<String> assignees,
            final Collection<String> candidateUsers,
            final Collection<String> candidateGroups,
            final OffsetDateTime initialTimestamp,
            final RetrieveItemsMode mode,
            final List<Criteria> predefinedCriterias) {

        final var subCriterias = new LinkedList<Criteria>();

        // honour user's permissions

        final var userAndRestrictions = new LinkedList<Criteria>();
        final var userOrRestrictions = new LinkedList<Criteria>();
        if ((assignees != null)
                && !assignees.isEmpty()) {
            if (notInAssignees) {
                final var assigneeMatches = Criteria.where("assignee").not().in(assignees);
                userAndRestrictions.add(assigneeMatches);
            } else {
                final var assigneeMatches = Criteria.where("assignee").in(assignees);
                userOrRestrictions.add(assigneeMatches);
            }
        } else if (notInAssignees) {
            final var assigneeMatches = Criteria.where("assignee").exists(false);
            userAndRestrictions.add(assigneeMatches);
        }

        if ((candidateUsers != null)
                && !candidateUsers.isEmpty()) {
            final var candidateUsersMatches = Criteria.where("candidateUsers").in(candidateUsers);
            userOrRestrictions.add(candidateUsersMatches);
        }
        if ((candidateGroups != null)
                && !candidateGroups.isEmpty()) {
            final var candidateGroupsMatches = Criteria.where("candidateGroups").in(candidateGroups);
            userOrRestrictions.add(candidateGroupsMatches);
        }
        if (!userAndRestrictions.isEmpty()
                || !userOrRestrictions.isEmpty()) {
            if (includeDanglingTasks) {
                final var noAssigneeOrNoCandidate = Criteria.where("dangling").is(Boolean.TRUE);
                userOrRestrictions.add(noAssigneeOrNoCandidate);
            }
            final Criteria permittedTasks = new Criteria().orOperator(userOrRestrictions);
            subCriterias.add(permittedTasks);
            subCriterias.addAll(userAndRestrictions);
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

        // limit result according to predefined filters

        if (predefinedCriterias != null) {
            subCriterias.addAll(predefinedCriterias);
        }

        return querySupplier
                .get()
                .addCriteria(new Criteria().andOperator(subCriterias));

    }

}
