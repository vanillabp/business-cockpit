package io.vanillabp.cockpit.workflowlist;

import com.mongodb.client.model.changestream.ChangeStreamDocument;
import io.vanillabp.cockpit.commons.mongo.changestreams.ChangeStreamUtils;
import io.vanillabp.cockpit.util.SearchCriteriaHelper;
import io.vanillabp.cockpit.util.SearchQuery;
import io.vanillabp.cockpit.util.kwic.KwicResult;
import io.vanillabp.cockpit.util.kwic.KwicService;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.cockpit.workflowlist.model.WorkflowRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.bson.Document;
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
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.messaging.Message;
import org.springframework.data.mongodb.core.messaging.Subscription;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WorkflowlistService {

    public enum RetrieveItemsMode {
        All,
        Active,
        Inactive
    }

    public static final String INDEX_CUSTOM_SORT_PREFIX = "_sort_";
    public static final String PROPERTY_CREATEDAT = "createdAt";
    public static final String PROPERTY_ID = "id";

    private static final List<Sort.Order> DEFAULT_ORDER_ASC = List.of(
            Order.asc(PROPERTY_CREATEDAT),
            Order.asc(PROPERTY_ID)
    );
    private static final List<Sort.Order> DEFAULT_ORDER_DESC= List.of(
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
    private ChangeStreamUtils changeStreamUtils;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private KwicService kwicService;

    private Subscription dbChangesSubscription;

    @PostConstruct
    protected void initializeTrackingOfIndexes() {

        final var knownSorts = mongoTemplate
                .indexOps(Workflow.COLLECTION_NAME)
                .getIndexInfo()
                .stream()
                .map(indexInfo -> indexInfo.getName())
                .filter(name -> name.startsWith(INDEX_CUSTOM_SORT_PREFIX))
                .map(name -> name.substring(INDEX_CUSTOM_SORT_PREFIX.length()))
                .toList();

        try {
            sortAndFilterIndexWriteLock.lock();
            sortAndFilterIndexes.addAll(knownSorts);
        } finally {
            sortAndFilterIndexWriteLock.unlock();
        }

    }

    public boolean createWorkflow(
            final Workflow workflow) {

        if (workflow == null) {
            return false;
        }

        // the cockpit's own clock, see Workflow#getReportedAt
        workflow.setReportedAt(OffsetDateTime.now());

        try {
            workflowRepository.save(workflow);
            return true;
        } catch (Exception e) {
            logger.error("Could not save workflow '{}'!",
                    workflow.getId(),
                    e);
            return false;
        }

    }

    public Workflow getWorkflow(
            final String workflowId) {

        return workflowRepository
                .findById(workflowId)
                .orElse(null);

    }

    public Page<Workflow> getWorkflows(
            final int pageNumber,
            final int pageSize,
            final OffsetDateTime initialTimestamp,
            final boolean includeDanglingWorkflows,
            final Collection<String> accessibleToUsers,
            final Collection<String> accessibleToGroups,
            final Collection<String> businessIds,
            final Collection<SearchQuery> searchQueries,
            final String sort,
            final boolean sortAscending,
            final WorkflowlistService.RetrieveItemsMode mode) {

        return getWorkflows(
                pageNumber,
                pageSize,
                initialTimestamp,
                includeDanglingWorkflows,
                accessibleToUsers,
                accessibleToGroups,
                mode,
                businessIds,
                searchQueries,
                sort,
                sortAscending);
    }

    public Page<Workflow> getWorkflows(
            final int pageNumber,
            final int pageSize,
            final OffsetDateTime initialTimestamp,
            final boolean includeDanglingWorkflows,
            final Collection<String> accessibleToUsers,
            final Collection<String> accessibleToGroups,
            final RetrieveItemsMode mode,
            final Collection<String> businessIds,
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
        query.addCriteria(
                buildWorkflowlistCriteria(
                        includeDanglingWorkflows,
                        accessibleToUsers,
                        accessibleToGroups,
                        endedSince,
                        mode,
                        null,
                        businessIds));
        final var searchCriteria = SearchCriteriaHelper.buildSearchCriteria(searchQueries);
        if (searchCriteria != null) {
            searchCriteria.forEach(query::addCriteria);
        }

        // build index before retrieving data if necessary
        if (orderBySort.toBeIndexed() != null) {
            ensureSortAndFilterIndex(orderBySort);
        }

        final var numberOfWorkflowsFound = mongoTemplate
                .count(Query.of(query).limit(-1).skip(-1), Workflow.class);
        final var workflowsFound = mongoTemplate
                .find(query.with(pageRequest), Workflow.class);

        return PageableExecutionUtils.getPage(
                workflowsFound,
                pageRequest,
                () -> numberOfWorkflowsFound);

    }

    /**
     * Sorting and filtering by arbitrary properties needs an index per combination. They are
     * created the first time such a combination is asked for and remembered afterwards. A failing
     * creation is remembered as well: the query still works without the index, and retrying it on
     * every request would only cost time.
     */
    private void ensureSortAndFilterIndex(
            final WorkflowListOrder orderBySort) {

        try {
            sortAndFilterIndexReadLock.lock();
            if (sortAndFilterIndexes.contains(orderBySort.indexName)) {
                return;
            }
        } finally {
            sortAndFilterIndexReadLock.unlock();
        }

        try {
            sortAndFilterIndexWriteLock.lock();
            if (sortAndFilterIndexes.contains(orderBySort.indexName)) {
                return;
            }
            final var newIndex = new Index();
            orderBySort
                    .toBeIndexed()
                    .forEach(languageSort -> newIndex.on(languageSort, Sort.Direction.ASC));
            newIndex.named(INDEX_CUSTOM_SORT_PREFIX + orderBySort.indexName);
            try {
                mongoTemplate
                        .indexOps(Workflow.COLLECTION_NAME)
                        .createIndex(newIndex);
            } catch (Exception e) {
                logger.error("Could not create Mongo-DB index for sorting and filtering of workflowlist", e);
            }
            sortAndFilterIndexes.add(orderBySort.indexName);
        } finally {
            sortAndFilterIndexWriteLock.unlock();
        }

    }

    private record WorkflowListOrder(List<Order> order, String indexName, List<String> toBeIndexed) {}

    private WorkflowlistService.WorkflowListOrder getWorkflowListOrder(
            final String _sort,
            final boolean sortAscending) {

        final var sort = _sort == null
                ? PROPERTY_CREATEDAT
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

        return new WorkflowListOrder(order, sort, indexProps);

    }

    public Page<Workflow> getWorkflowsUpdated(
            final boolean includeDanglingWorkflows,
            final Collection<String> accessibleToUsers,
            final Collection<String> accessibleToGroups,
            final int size,
            final Collection<String> knownWorkflowIds,
            final OffsetDateTime initialTimestamp,
            final Collection<SearchQuery> searchQueries,
            final String sort,
            final boolean sortAscending,
            final RetrieveItemsMode mode) {

        final var orderBySort = getWorkflowListOrder(sort, sortAscending);
        final var pageRequest = PageRequest
                .ofSize(size)
                .withPage(0)
                .withSort(Sort.by(orderBySort.order()));

        final var effectiveMode = mode != null ? mode : RetrieveItemsMode.Active;

        final var query = new Query();
        query.fields().include("_id");
        query.addCriteria(
                buildWorkflowlistCriteria(
                        includeDanglingWorkflows,
                        accessibleToUsers,
                        accessibleToGroups,
                        initialTimestamp,
                        effectiveMode,
                        null,
			null));
        final var searchCriteria = SearchCriteriaHelper.buildSearchCriteria(searchQueries);
        if (searchCriteria != null) {
            searchCriteria.forEach(query::addCriteria);
        }
        final var numberOfWorkflows = mongoTemplate
                .count(Query.of(query).limit(-1).skip(-1), Workflow.class);

        // the query only fetches ids; workflows the client does not know yet are loaded completely
        final var result = mongoTemplate
                .find(query.with(pageRequest), Workflow.class)
                .stream()
                .map(workflow -> knownWorkflowIds.contains(workflow.getId())
                        ? Optional.of(workflow)
                        : workflowRepository.findById(workflow.getId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        return new PageImpl<>(
                result,
                Pageable
                        .ofSize(result.isEmpty() ? 1 : result.size())
                        .withPage(0),
                numberOfWorkflows);

    }

    public boolean updateWorkflow(
            final Workflow workflow) {

        if (workflow == null) {
            return false;
        }

        try {
            workflowRepository.save(workflow);
            return true;
        } catch (Exception e) {
            logger.error("Could not save workflow '{}'!",
                    workflow.getId(),
                    e);
            return false;
        }

    }

    public boolean cancelWorkflow(
            final Workflow workflow,
            final OffsetDateTime timestamp,
            final String reason) {

        if (workflow == null) {
            return false;
        }

        workflow.setEndedAt(timestamp);
        workflow.setComment(reason);

        try {
            workflowRepository.save(workflow);
            return true;
        } catch (Exception e) {
            logger.error("Could not save workflow '{}'!",
                    workflow.getId(),
                    e);
            return false;
        }

    }


    public boolean completeWorkflow(
            final Workflow workflow,
            final OffsetDateTime timestamp) {

        if (workflow == null) {
            return false;
        }

        workflow.setEndedAt(timestamp);

        try {
            workflowRepository.save(workflow);
            return true;
        } catch (Exception e) {
            logger.error("Could not save workflow '{}'!",
                    workflow.getId(),
                    e);
            return false;
        }

    }

    @EventListener
    public void subscribeToDbChanges(
            final ApplicationStartedEvent event) {

        dbChangesSubscription = changeStreamUtils.subscribe(
                Workflow.class,
                this::publishWorkflowChange);

    }

    private void publishWorkflowChange(
            final Message<ChangeStreamDocument<Document>, Workflow> message) {

        try {
            applicationEventPublisher.publishEvent(
                    WorkflowChangedNotification.build(message));
        } catch (Exception e) {
            logger.warn("Error on processing workflow change-stream event! Will resume stream.", e);
        }

    }

    public List<KwicResult> kwic(
            final OffsetDateTime endedSince,
            final boolean includeDanglingWorkflows,
            final Collection<String> accessibleToUsers,
            final Collection<String> accessibleToGroups,
            final Collection<SearchQuery> searchQueries,
            final String path,
            final String query) {

        if (!StringUtils.hasText(query)
                || (query.length() < 3)) {
            return List.of();
        }

        final var searchCriteria = new LinkedList<Criteria>();
        searchCriteria.add(new Criteria(path).regex(query, "i"));
        final var match =
                buildWorkflowlistCriteria(
                        includeDanglingWorkflows,
                        accessibleToUsers,
                        accessibleToGroups,
                        endedSince,
                        RetrieveItemsMode.Active,
                        searchCriteria,
			null);

        return kwicService.getKwicAggregatedResults(Workflow.class, match, searchQueries, path, query);
    }

    public CriteriaDefinition buildWorkflowlistCriteria(
            final boolean includeDanglingWorkflows,
            final Collection<String> accessibleToUsers,
            final Collection<String> accessibleToGroups,
            final OffsetDateTime initialTimestamp,
            final RetrieveItemsMode mode,
            final List<Criteria> predefinedCriterias,
	    final Collection<String> businessIds) {

        final var subCriterias = new LinkedList<Criteria>();

        // limit result according to users and groups

        final var userOrRestrictions = new LinkedList<Criteria>();
        if ((accessibleToUsers != null)
                && !accessibleToUsers.isEmpty()) {
            final var candidateUsersMatches = Criteria.where("accessibleToUsers.id").in(accessibleToUsers);
            userOrRestrictions.add(candidateUsersMatches);
        }
        if ((accessibleToGroups != null)
                && !accessibleToGroups.isEmpty()) {
            final var candidateGroupsMatches = Criteria.where("accessibleToGroups.id").in(accessibleToGroups);
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

	if(businessIds != null && !businessIds.isEmpty()) {
	    subCriterias.add(Criteria.where("businessId").in(businessIds));
	}

        // limit result according to predefined filters

        if ((predefinedCriterias != null)
                && !predefinedCriterias.isEmpty()) {
            subCriterias.addAll(predefinedCriterias);
        }

        return subCriterias.isEmpty() ?
                new Criteria() :
                new Criteria().andOperator(subCriterias);
    }

    @PreDestroy
    public void cleanup() {

        if (dbChangesSubscription != null) {
            changeStreamUtils.unsubscribe(dbChangesSubscription);
        }

    }

}
