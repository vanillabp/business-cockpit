package io.vanillabp.cockpit.tasklist;

import io.vanillabp.cockpit.commons.mongo.changestreams.ReactiveChangeStreamUtils;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.tasklist.model.UserTaskRepository;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.util.SearchCriteriaHelper;
import io.vanillabp.cockpit.util.SearchQuery;
import io.vanillabp.cockpit.util.kwic.KwicResult;
import io.vanillabp.cockpit.util.kwic.KwicService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
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
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
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
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class UserTaskService {

    public static final String INDEX_CUSTOM_SORT_PREFIX = "_sort_";
    public static final String PROPERTY_DUEDATE = "dueDate";
    public static final String PROPERTY_CREATEDAT = "createdAt";
    public static final String PROPERTY_ID = "id";

    public static enum RetrieveItemsMode {
        All,
        OpenTasks,
        OpenTasksWithoutFollowup,
        OpenTasksWithFollowup,
        ClosedTasksOnly
    }

    private static final List<Sort.Order> DEFAULT_ORDER_ASC = List.of(
                    Order.asc(PROPERTY_DUEDATE),
                    Order.asc(PROPERTY_CREATEDAT),
                    Order.asc(PROPERTY_ID)
            );
    private static final List<Sort.Order> DEFAULT_ORDER_DESC = List.of(
                    Order.desc(PROPERTY_DUEDATE),
                    Order.desc(PROPERTY_CREATEDAT),
                    Order.desc(PROPERTY_ID)
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
    private KwicService kwicService;

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;

    private Disposable dbChangesSubscription;

    @PostConstruct
    protected void initializeTrackingOfIndexes() {

        mongoTemplate
                .indexOps(UserTask.COLLECTION_NAME)
                .getIndexInfo()
                .filter(indexInfo -> indexInfo.getName().startsWith(INDEX_CUSTOM_SORT_PREFIX))
                .map(indexInfo -> indexInfo.getName().substring(INDEX_CUSTOM_SORT_PREFIX.length()))
                .collectList()
                .subscribe(sort -> {
                    try {
                        sortAndFilterIndexWriteLock.lock();
                        sortAndFilterIndexes.addAll(sort);
                    } finally {
                        sortAndFilterIndexWriteLock.unlock();
                    }
                });

    }

    @EventListener
    public void subscribeToDbChanges(
            final ApplicationStartedEvent event) {

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
            final Person person) {

        return getUserTask(userTaskId)
                .flatMap(userTask -> {
                    userTask.addCandidatePerson(person);
                    return userTasks.save(userTask);
                });

    }

    public Flux<UserTask> assignTask(
            final Collection<String> userTaskIds,
            final Person person) {

        return userTasks.saveAll(
                userTasks
                        .findAllById(userTaskIds)
                        .map(userTask -> {
                            userTask.addCandidatePerson(person);
                            return userTask;
                        }));

    }

    public Mono<UserTask> unassignTask(
            final String userTaskId,
            final String personId) {

        return getUserTask(userTaskId)
                .flatMap(userTask -> {
                    userTask.removeCandidatePerson(personId);
                    return userTasks.save(userTask);
                });

    }

    public Flux<UserTask> unassignTask(
            final Collection<String> userTaskIds,
            final String personId) {

        return userTasks.saveAll(
                userTasks
                        .findAllById(userTaskIds)
                        .map(userTask -> {
                            userTask.removeCandidatePerson(personId);
                            return userTask;
                        }));

    }

    public Mono<UserTask> claimTask(
            final String userTaskId,
            final Person person) {

        return getUserTask(userTaskId)
                .flatMap(userTask -> {
                    if ((userTask.getAssignee() == null)
                        || !userTask.getAssignee().getId().equals(person.getId())) {
                        userTask.setAssignee(person);
                        return userTasks.save(userTask);
                    }
                    return Mono.just(userTask);
                });

    }

    public Flux<UserTask> claimTask(
            final Collection<String> userTaskIds,
            final Person person) {

        return userTasks.saveAll(
                userTasks
                        .findAllById(userTaskIds)
                        .map(userTask -> {
                            userTask.setAssignee(person);
                            return userTask;
                        }));

    }

    public Mono<UserTask> unclaimTask(
            final String userTaskId,
            final String personId) {

        final var query = new Query();
        query.addCriteria(Criteria.where("id").is(userTaskId));
        query.addCriteria(Criteria.where("assignee.id").is(personId));
        final var update = new Update();
        update.unset("assignee");

        return mongoTemplate
                .updateFirst(query, update, UserTask.class)
                .single()
                .flatMap(result -> userTasks.findById(result.getUpsertedId().asString().getValue()));

    }

    public Flux<UserTask> unclaimTask(
            final Collection<String> userTaskIds,
            final String personId) {

        final var query = new Query();
        query.addCriteria(Criteria.where("id").in(userTaskIds));
        query.addCriteria(Criteria.where("assignee.id").is(personId));
        final var update = new Update();
        update.unset("assignee");

        final var findQuery = new Query();
        findQuery.addCriteria(Criteria.where("id").in(userTaskIds));

        return mongoTemplate
                .updateMulti(query, update, UserTask.class)
                .thenMany(mongoTemplate.find(findQuery, UserTask.class));

    }

    private record UserTaskListOrder(List<Order> order, String indexName, List<String> toBeIndexed) {}

    private UserTaskListOrder getUserTaskListOrder(
            final String _sort,
            final boolean sortAscending) {

        final var sort = _sort == null
                ? PROPERTY_DUEDATE
                : _sort;

        final var order = new LinkedList<Order>();
        final var defaultOrdering = new LinkedList<>(sortAscending ? DEFAULT_ORDER_ASC : DEFAULT_ORDER_DESC);
        final var indexProps = new LinkedList<String>();
        Arrays
                .stream(sort.split(",")) // maybe something like 'title.de,title.en' or just simply 'assignee'
                .filter(StringUtils::hasText)
                .peek(languageBasedSort -> {
                    indexProps.add(languageBasedSort);
                    final var defaultOrder = defaultOrdering
                            .stream()
                            .filter(propertyOrder -> propertyOrder.getProperty().equals(languageBasedSort))
                            .findFirst();
                    defaultOrder.ifPresent(defaultOrdering::remove);
                })
                .map(languageBasedSort -> sortAscending
                        ? Order.asc(languageBasedSort).nullsLast()
                        : Order.desc(languageBasedSort).nullsLast())
                .forEach(order::add);

        defaultOrdering
                .forEach(defaultOrder -> {
                    order.add(defaultOrder);
                    if (defaultOrder.getProperty().equals(PROPERTY_ID)) {
                        indexProps.add("_id");
                    } else {
                        indexProps.add(defaultOrder.getProperty());
                    }
                });

        return new UserTaskListOrder(order, sort, indexProps);

    }

    public Mono<Page<UserTask>> getUserTasks(
            final boolean includeDanglingTasks,
            final boolean notInAssignees,
            final Collection<String> assignees,
            final Collection<String> candidateUsers,
            final Collection<String> candidateGroups,
            final Collection<String> candidateUsersToBeExcluded,
            final int pageNumber,
            final int pageSize,
            final OffsetDateTime initialTimestamp,
            final Collection<SearchQuery> searchQueries,
            final String sort,
            final boolean sortAscending) {

        return retrieveUserTasks(
                includeDanglingTasks,
                notInAssignees,
                assignees,
                candidateUsers,
                candidateGroups,
                candidateUsersToBeExcluded,
                pageNumber,
                pageSize,
                initialTimestamp,
                searchQueries,
                sort,
                sortAscending,
                null,
                RetrieveItemsMode.OpenTasks);

    }

    protected Mono<Page<UserTask>> retrieveUserTasks(
            final boolean includeDanglingTasks,
            final boolean notInAssignees,
            final Collection<String> assignees,
            final Collection<String> candidateUsers,
            final Collection<String> candidateGroups,
            final Collection<String> candidateUsersToBeExcluded,
            final int pageNumber,
            final int pageSize,
            final OffsetDateTime initialTimestamp,
            final Collection<SearchQuery> searchQueries,
            final String sort,
            final boolean sortAscending,
            final List<Criteria> predefinedCriterias,
            final RetrieveItemsMode mode) {

        final var orderBySort = getUserTaskListOrder(sort, sortAscending);
        final var pageRequest = PageRequest
                .ofSize(pageSize)
                .withPage(pageNumber)
                .withSort(Sort.by(orderBySort.order()));

        // build query
        final var query = new Query();
        final var searchCriteria = SearchCriteriaHelper.buildSearchCriteria(searchQueries);
        query.addCriteria(
                buildUserTasksCriteria(
                        includeDanglingTasks,
                        notInAssignees,
                        assignees,
                        candidateUsers,
                        candidateGroups,
                        candidateUsersToBeExcluded,
                        initialTimestamp,
                        mode,
                        predefinedCriterias));
        if (searchCriteria != null) {
            searchCriteria.forEach(query::addCriteria);
        }

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
        try {
            sortAndFilterIndexReadLock.lock();
            if (sortAndFilterIndexes.contains(orderBySort.indexName)) {
                return result;
            }
        } finally {
            sortAndFilterIndexReadLock.unlock();
        }

        try {
            sortAndFilterIndexWriteLock.lock();
            if (sortAndFilterIndexes.contains(orderBySort.indexName)) {
                return result;
            }
            final var newIndex = new Index();
            orderBySort
                    .toBeIndexed()
                    .forEach(languageSort -> newIndex.on(languageSort, Sort.Direction.ASC));
            newIndex.named(INDEX_CUSTOM_SORT_PREFIX + orderBySort.indexName);
            return mongoTemplate
                    .indexOps(UserTask.COLLECTION_NAME)
                    .ensureIndex(newIndex)
                    .flatMap(unknown -> {
                        sortAndFilterIndexes.add(orderBySort.indexName);
                        return result;
                    })
                    .onErrorResume(e -> {
                        sortAndFilterIndexes.add(orderBySort.indexName);
                        logger.error("Could not create Mongo-DB index for sorting and filtering of tasklist", e);
                        return result;
                    });
        } finally {
            sortAndFilterIndexWriteLock.unlock();
        }

    }

    public Flux<KwicResult> kwic(
            final boolean includeDanglingTasks,
            final boolean notInAssignees,
            final Collection<String> assignees,
            final Collection<String> candidateUsers,
            final Collection<String> candidateGroups,
            final Collection<String> candidatesToBeExcluded,
            final OffsetDateTime initialTimestamp,
            final Collection<SearchQuery> searchQueries,
            final String path,
            final String query) {

        if (!StringUtils.hasText(query)
                || (query.length() < 3)) {
            return Flux.empty();
        }

        final var searchCriteria = new LinkedList<Criteria>();
        searchCriteria.add(new Criteria(path).regex(query, "i"));
        final var match =
                buildUserTasksCriteria(
                        includeDanglingTasks,
                        notInAssignees,
                        assignees,
                        candidateUsers,
                        candidateGroups,
                        candidatesToBeExcluded,
                        initialTimestamp,
                        RetrieveItemsMode.OpenTasks,
                        searchCriteria);

        return kwicService.getKwicAggregatedResults(UserTask.class, match, searchQueries, path, query);
    }
    
    public Flux<UserTask> getUserTasksOfWorkflow(
            final String workflowId,
            final boolean activeOnly,
            final boolean limitListAccordingToCurrentUsersPermissions,
            final String currentUser,
            final Collection<String> currentUserGroups,
            final int size,
            final String sort,
            final boolean sortAscending) {

        return retrieveUserTasks(
                    true,
                    false,
                    limitListAccordingToCurrentUsersPermissions ? List.of(currentUser) : null,
                    limitListAccordingToCurrentUsersPermissions ? List.of(currentUser) : null,
                    limitListAccordingToCurrentUsersPermissions ? currentUserGroups : null,
                    limitListAccordingToCurrentUsersPermissions ? List.of(currentUser) : null,
                    0,
                    size,
                    OffsetDateTime.now(),
                    null,
                    sort,
                    sortAscending,
                    List.of(Criteria.where("workflowId").is(workflowId)),
                    activeOnly ? RetrieveItemsMode.OpenTasks : RetrieveItemsMode.All
                ).map(Page::getContent)
                .flatMapMany(Flux::fromIterable);
        
    }
    
    public Mono<Page<UserTask>> getUserTasksUpdated(
            final boolean includeDanglingTasks,
            final boolean notInAssignees,
            final Collection<String> assignees,
            final Collection<String> candidateUsers,
            final Collection<String> candidateGroups,
            final Collection<String> candidatesToBeExcluded,
            final int size,
            final Collection<String> knownUserTasksIds,
            final OffsetDateTime initialTimestamp,
            final Collection<SearchQuery> searchQueries,
            final String sort,
            final boolean sortAscending) {

        final var orderBySort = getUserTaskListOrder(sort, sortAscending);
        final var pageRequest = PageRequest
                .ofSize(size)
                .withPage(0)
                .withSort(Sort.by(orderBySort.order()));

        final var query = new Query();
        query.fields().include("_id");
        query.addCriteria(
                buildUserTasksCriteria(
                        includeDanglingTasks,
                        notInAssignees,
                        assignees,
                        candidateUsers,
                        candidateGroups,
                        candidatesToBeExcluded,
                        initialTimestamp,
                        RetrieveItemsMode.OpenTasks,
                        null));
        final var searchCriteria = SearchCriteriaHelper.buildSearchCriteria(searchQueries);
        if (searchCriteria != null) {
            searchCriteria.forEach(query::addCriteria);
        }
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
            return Mono.just(Boolean.FALSE);
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
            return Mono.just(Boolean.FALSE);
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
            return Mono.just(Boolean.FALSE);
        }
        
        if (userTask.getDueDate() == null) {
            // for correct sorting
            userTask.setDueDate(OffsetDateTime.MAX);
        }
        
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

    public Criteria buildUserTasksCriteria(
            final boolean includeDanglingTasks,
            final boolean notInAssignees,
            final Collection<String> assignees,
            final Collection<String> candidateUsers,
            final Collection<String> candidateGroups,
            final Collection<String> candidatesToBeExcluded,
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
                final var assigneeMatches = Criteria.where("assignee.id").not().in(assignees);
                userAndRestrictions.add(assigneeMatches);
            } else {
                final var assigneeMatches = Criteria.where("assignee.id").in(assignees);
                userOrRestrictions.add(assigneeMatches);
            }
        } else if (notInAssignees) {
            final var assigneeMatches = Criteria.where("assignee").exists(false);
            userAndRestrictions.add(assigneeMatches);
        }

        if ((candidateUsers != null)
                && !candidateUsers.isEmpty()) {
            final var candidateUsersMatches = Criteria.where("candidateUsers.id").in(candidateUsers);
            userOrRestrictions.add(candidateUsersMatches);
        }
        if ((candidateGroups != null)
                && !candidateGroups.isEmpty()) {
            final var candidateGroupsMatches = Criteria.where("candidateGroups.id").in(candidateGroups);
            userOrRestrictions.add(candidateGroupsMatches);
        }

        if(candidatesToBeExcluded != null && !candidatesToBeExcluded.isEmpty()){
            final var candidateUserExclusions =
                    Criteria.where("excludedCandidateUsers.id")
                            .not().in(candidatesToBeExcluded);
            userAndRestrictions.add(candidateUserExclusions);
        }

        if (!userAndRestrictions.isEmpty()
                || !userOrRestrictions.isEmpty()) {
            if (includeDanglingTasks) {
                final var noAssigneeOrNoCandidate = Criteria.where("dangling").is(Boolean.TRUE);
                userOrRestrictions.add(noAssigneeOrNoCandidate);
            }
            if (!userOrRestrictions.isEmpty()) {
                subCriterias.add(new Criteria().orOperator(userOrRestrictions));
            }
            subCriterias.addAll(userAndRestrictions);
        }

        // limit result according to list mode

        // return consistent results across multiple requests of pages
        switch (mode) {
            case All:
                break;
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

        return new Criteria().andOperator(subCriterias);

    }

}
