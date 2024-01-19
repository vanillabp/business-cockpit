package io.vanillabp.cockpit.workflowlist;

import io.vanillabp.cockpit.commons.mongo.changestreams.ReactiveChangeStreamUtils;
import io.vanillabp.cockpit.util.SearchQuery;
import io.vanillabp.cockpit.util.microserviceproxy.MicroserviceProxyRegistry;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.cockpit.workflowlist.model.WorkflowRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.StringOperators;
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
import java.util.stream.Collectors;

@Service
public class WorkflowlistService {

    public enum RetrieveItemsMode {
        All,
        Active,
        Inactive
    };

    public record KwicResult(@Id String item, int count) {};

    public static final String INDEX_CUSTOM_SORT_PREFIX = "_sort_";

    private static final List<Sort.Order> DEFAULT_ORDER_ASC = List.of(
            Order.asc("createdAt"),
            Order.asc("id")
    );
    private static final List<Sort.Order> DEFAULT_ORDER_DESC= List.of(
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
    private WorkflowRepository workflowRepository;

    @Autowired
    private MicroserviceProxyRegistry microserviceProxyRegistry;

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;

    private Disposable dbChangesSubscription;

    public Mono<Boolean> createWorkflow(
            final Workflow workflow) {

        if (workflow == null) {
            return Mono.just(Boolean.FALSE);
        }

        return workflowRepository
                .save(workflow)
                .doOnNext(item -> microserviceProxyRegistry
                        .registerMicroservice(
                                item.getWorkflowModule(),
                                item.getWorkflowModuleUri()))
                .map(item -> Boolean.TRUE)
                .onErrorResume(e -> {
                    logger.error("Could not save workflow '{}'!",
                            workflow.getId(),
                            e);
                    return Mono.just(Boolean.FALSE);
                });
    }

    public Mono<Workflow> getWorkflow(
            final String workflowId) {

        return workflowRepository
                .findById(workflowId);

    }

    public Mono<Page<Workflow>> getWorkflows(
            final int pageNumber,
            final int pageSize,
            final OffsetDateTime initialTimestamp,
            final boolean includeDanglingWorkflows,
            final Collection<String> accessibleToUsers,
            final Collection<String> accessibleToGroups,
            final Collection<SearchQuery> searchQueries,
            final String sort,
            final boolean sortAscending) {

        final var orderBySort = getWorkflowListOrder(sort, sortAscending);
        final var pageRequest = PageRequest
                .ofSize(pageSize)
                .withPage(pageNumber)
                .withSort(Sort.by(orderBySort.order()));

        final var endedSince = initialTimestamp != null
                ? initialTimestamp
                : OffsetDateTime.now();

        final var query = new Query();
        final var searchCriteria = buildSearchCriteria(searchQueries);
        query.addCriteria(
                buildWorkflowlistCriteria(
                        includeDanglingWorkflows,
                        accessibleToUsers,
                        accessibleToGroups,
                        endedSince,
                        RetrieveItemsMode.Active,
                        searchCriteria));

        // prepare to retrieve data on execution
        final var numberOfWorkflowsFound = mongoTemplate
                .count(Query.of(query).limit(-1).skip(-1), Workflow.class);
        final var workflowsFound = mongoTemplate
                .find(query.with(pageRequest), Workflow.class);
        final var result = Mono
                .zip(workflowsFound.collectList(), numberOfWorkflowsFound)
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
            newIndex.on("createdAt", Sort.Direction.ASC)
                    .on("_id", Sort.Direction.ASC)
                    .named(INDEX_CUSTOM_SORT_PREFIX + sort);
            return mongoTemplate
                    .indexOps(Workflow.COLLECTION_NAME)
                    .ensureIndex(newIndex)
                    .flatMap(unknown -> {
                        sortAndFilterIndexes.add(sort);
                        return result;
                    })
                    .onErrorResume(e -> {
                        sortAndFilterIndexes.add(sort);
                        logger.error("Could not create Mongo-DB index for sorting and filtering of workflowlist", e);
                        return result;
                    });
        } finally {
            sortAndFilterIndexWriteLock.unlock();
        }

    }

    private static record WorkflowListOrder(List<Order> order, List<String> toBeIndexed) {}

    private WorkflowlistService.WorkflowListOrder getWorkflowListOrder(
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
        return new WorkflowlistService.WorkflowListOrder(order, nonDefaultSort);

    }
    public Mono<Page<Workflow>> getWorkflowsUpdated(
            final boolean includeDanglingWorkflows,
            final Collection<String> accessibleToUsers,
            final Collection<String> accessibleToGroups,
            final int size,
            final Collection<String> knownWorkflowIds,
            final OffsetDateTime initialTimestamp,
            final Collection<SearchQuery> searchQueries,
            final String sort,
            final boolean sortAscending) {

        final var orderBySort = getWorkflowListOrder(sort, sortAscending);
        final var pageRequest = PageRequest
                .ofSize(size)
                .withPage(0)
                .withSort(Sort.by(orderBySort.order()));

        final var query = new Query();
        query.fields().include("_id");
        final var searchCriteria = buildSearchCriteria(searchQueries);
        query.addCriteria(
                buildWorkflowlistCriteria(
                        includeDanglingWorkflows,
                        accessibleToUsers,
                        accessibleToGroups,
                        initialTimestamp,
                        RetrieveItemsMode.Active,
                        searchCriteria));

        final var numberOfWorkflows = mongoTemplate
                .count(Query.of(query).limit(-1).skip(-1), Workflow.class);
        final var result = mongoTemplate
                .find(query.with(pageRequest), Workflow.class);

        return result
                .flatMapSequential(workflow -> {
                    if (knownWorkflowIds.contains(workflow.getId())) {
                        return Mono.just(workflow);
                    }
                    return workflowRepository.findById(workflow.getId());
                })
                .collectList()
                .zipWith(numberOfWorkflows)
                .map(t -> new PageImpl<>(
                        t.getT1(),
                        Pageable
                                .ofSize(t.getT1().isEmpty() ? 1 : t.getT1().size())
                                .withPage(0),
                        t.getT2()));

    }


    public Mono<Boolean> updateWorkflow(
            final Workflow workflow) {

        if (workflow == null) {
            return Mono.just(Boolean.FALSE);
        }

        return workflowRepository
                .save(workflow)
                .onErrorMap(e -> {
                    logger.error("Could not save workflow '{}'!",
                            workflow.getWorkflowId(),
                            e);
                    return null;
                })
                .map(savedWorkflow -> savedWorkflow != null);

    }

    public Mono<Boolean> cancelWorkflow(
            final Workflow workflow,
            final OffsetDateTime timestamp,
            final String reason) {

        if (workflow == null) {
            Mono.just(Boolean.FALSE);
        }

        workflow.setEndedAt(timestamp);
        workflow.setComment(reason);

        return workflowRepository
                .save(workflow)
                .map(task -> Boolean.TRUE)
                .onErrorResume(e -> {
                    logger.error("Could not save workflow '{}'!",
                            workflow.getWorkflowId(),
                            e);
                    return Mono.just(Boolean.FALSE);
                });

    }


    public Mono<Boolean> completeWorkflow(
            final Workflow workflow,
            final OffsetDateTime timestamp) {

        if (workflow == null) {
            Mono.just(Boolean.FALSE);
        }

        workflow.setEndedAt(timestamp);

        return workflowRepository
                .save(workflow)
                .map(item -> Boolean.TRUE)
                .onErrorResume(e -> {
                    logger.error("Could not save workflow '{}'!",
                            workflow.getWorkflowId(),
                            e);
                    return Mono.just(Boolean.FALSE);
                });
    }

    @PostConstruct
    public void subscribeToDbChanges() {

        dbChangesSubscription = changeStreamUtils
                .subscribe(Workflow.class)
                .flatMapSequential(workflow -> Mono
                        .fromCallable(() -> WorkflowChangedNotification.build(workflow))
                        .doOnError(e -> logger
                                .warn("Error on processing workflow change-stream "
                                        + "event! Will resume stream.", e))
                        .onErrorResume(Exception.class, e -> Mono.empty()))
                .doOnNext(applicationEventPublisher::publishEvent)
                .subscribe();

        // register all URLs already known
        workflowRepository
                .findAllWorkflowModulesAndUris()
                .collectList()
                .map(modulesAndUris -> modulesAndUris
                        .stream()
                        .collect(Collectors.toMap(
                                Workflow::getWorkflowModule,
                                Workflow::getWorkflowModuleUri)))
                .doOnNext(microserviceProxyRegistry::registerMicroservices)
                .subscribe();

    }

    public Flux<KwicResult> kwic(
            final OffsetDateTime endedSince,
            final boolean includeDanglingWorkflows,
            final Collection<String> accessibleToUsers,
            final Collection<String> accessibleToGroups,
            final Collection<SearchQuery> searchQueries,
            final String path,
            final String query) {

        if (!StringUtils.hasText(query)
                || (query.length() < 3)) {
            return Flux.empty();
        }

        final var searchCriteria = new LinkedList<Criteria>();
        searchCriteria.addAll(buildSearchCriteria(searchQueries));
        searchCriteria.add(new Criteria(path).regex(query));
        final var match =
                buildWorkflowlistCriteria(
                        includeDanglingWorkflows,
                        accessibleToUsers,
                        accessibleToGroups,
                        endedSince,
                        RetrieveItemsMode.Active,
                        searchCriteria);

        /*
        db.workflow.aggregate([
            { $match: { 'title.de': { $regex: 'i', $options: 'i' } } },
            { $addFields: { 'matches': { $regexFindAll: { input: '$title.de', regex: '(\S*i\S*)', options: 'i' } } } },
            { $project: { '_id': 0, 'matches.captures': 1 } },
            { $unwind: '$captures' },
            { $unwind: '$captures' },
            { $group: { _id: '$captures', count: { $sum: 1 } } }
        ])
         */

        final var groupedQuery = "(\\S*" + query + "\\S*)"; // find entire words
        return mongoTemplate
                .aggregate(
                        Aggregation.newAggregation(
                                // limit results according to regexp and predefined limitations
                                Aggregation.match(match),
                                // add words matching as a new field
                                Aggregation.addFields().addFieldWithValue("matches", StringOperators.RegexFindAll.valueOf(path).regex(groupedQuery)).build(),
                                // drop fields not necessary
                                Aggregation.project().andExclude("_id").andInclude("matches.captures"),
                                // unwind result from find 'all'
                                Aggregation.unwind("captures"),
                                // unwind result from regex group -> may be used in future for other groups if necessary
                                Aggregation.unwind("captures"),
                                // group and count words found
                                Aggregation.group("captures").count().as("count"),
                                Aggregation.limit(21)
                        ),
                        Workflow.class,
                        KwicResult.class)
                .sort((a, b) -> {
                        if (a.count < b.count) {
                            return -1;
                        }
                        if (a.count > b.count) {
                            return 1;
                        }
                        return a.item.compareTo(b.item);
                    });

    }

    public List<Criteria> buildSearchCriteria(
            final Collection<SearchQuery> searchQueries) {

        if ((searchQueries == null)
                || searchQueries.isEmpty()) {
            return List.of();
        }

        return searchQueries
                .stream()
                .map(query -> Criteria
                        .where(StringUtils.hasText(query.path()) ? query.path() : "detailsFulltextSearch")
                        .regex(query.query()))
                .toList();

    }

    public Criteria buildWorkflowlistCriteria(
            final boolean includeDanglingWorkflows,
            final Collection<String> accessibleToUsers,
            final Collection<String> accessibleToGroups,
            final OffsetDateTime initialTimestamp,
            final RetrieveItemsMode mode,
            final List<Criteria> predefinedCriterias) {

        final var subCriterias = new LinkedList<Criteria>();

        // limit result according to users and groups

        final var userOrRestrictions = new LinkedList<Criteria>();
        if ((accessibleToUsers != null)
                && !accessibleToUsers.isEmpty()) {
            final var candidateUsersMatches = Criteria.where("accessibleToUsers").in(accessibleToUsers);
            userOrRestrictions.add(candidateUsersMatches);
        }
        if ((accessibleToGroups != null)
                && !accessibleToGroups.isEmpty()) {
            final var candidateGroupsMatches = Criteria.where("accessibleToGroups").in(accessibleToGroups);
            userOrRestrictions.add(candidateGroupsMatches);
        }

        if (!userOrRestrictions.isEmpty()) {
            if (includeDanglingWorkflows) {
                final var noAssigneeOrNoCandidate = Criteria.where("dangling").is(Boolean.TRUE);
                userOrRestrictions.add(noAssigneeOrNoCandidate);
            }
            subCriterias.add(new Criteria().orOperator(userOrRestrictions));
        }

        // limit result according to list mode

        // return consistent results across multiple requests of pages
        switch (mode) {
            case All:
                break;
            case Active:
                subCriterias.add(new Criteria().orOperator(
                        Criteria.where("endedAt").exists(false),
                        Criteria.where("endedAt").gte(initialTimestamp)));
                break;
            case Inactive:
                subCriterias.add(new Criteria().orOperator(
                        Criteria.where("endedAt").exists(true),
                        Criteria.where("endedAt").lt(initialTimestamp)));
                break;
            default:
                throw new RuntimeException("Unsupported mode '"
                        + mode
                        + "'! Did you forget to extend this switch instruction?");
        }

        // limit result according to predefined filters

        if ((predefinedCriterias != null)
                && !predefinedCriterias.isEmpty()) {
            subCriterias.addAll(predefinedCriterias);
        }

        return new Criteria().andOperator(subCriterias);

    }

    @PreDestroy
    public void cleanup() {

        dbChangesSubscription.dispose();

    }

}
