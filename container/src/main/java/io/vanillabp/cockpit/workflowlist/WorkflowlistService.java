package io.vanillabp.cockpit.workflowlist;

import io.vanillabp.cockpit.commons.mongo.changestreams.ReactiveChangeStreamUtils;
import io.vanillabp.cockpit.util.microserviceproxy.MicroserviceProxyRegistry;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.cockpit.workflowlist.model.WorkflowRepository;
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
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
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
            final String sort,
            final boolean sortAscending) {

        final var orderBySort = getWorkflowListOrder(sort, sortAscending);
        final var pageRequest = PageRequest
                .ofSize(pageSize)
                .withPage(pageNumber)
                .withSort(Sort.by(orderBySort.order()));
        
        final var endedSince = (initialTimestamp != null
                ? initialTimestamp
                : OffsetDateTime.now()).toInstant();
        
        final var result = workflowRepository
                .findActive(endedSince, pageRequest)
                .collectList()
                .zipWith(workflowRepository.countActive(endedSince))
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
            final int size,
            final Collection<String> knownWorkflowIds,
            final OffsetDateTime initialTimestamp,
            final String sort,
            final boolean sortAscending) {

        final var orderBySort = getWorkflowListOrder(sort, sortAscending);
        final var pageRequest = PageRequest
                .ofSize(size)
                .withPage(0)
                .withSort(Sort.by(orderBySort.order()));

        final var endedSince = initialTimestamp.toInstant();
        
        final var workflows = workflowRepository.findIdsOfActive(endedSince, pageRequest);

        return workflows
                .flatMapSequential(workflow -> {
                    if (knownWorkflowIds.contains(workflow.getId())) {
                        return Mono.just(workflow);
                    }
                    return workflowRepository.findById(workflow.getId());
                })
                .collectList()
                .zipWith(workflowRepository.countActive(endedSince))
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

    @PreDestroy
    public void cleanup() {

        dbChangesSubscription.dispose();

    }

}
