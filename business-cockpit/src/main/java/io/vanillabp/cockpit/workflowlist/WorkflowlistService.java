package io.vanillabp.cockpit.workflowlist;

import com.mongodb.client.model.changestream.ChangeStreamDocument;
import io.vanillabp.cockpit.bpms.WhatAnEndReports;
import io.vanillabp.cockpit.bpms.OrderOfReports;
import io.vanillabp.cockpit.bpms.OutcomeOfStoring;
import io.vanillabp.cockpit.commons.mongo.changestreams.ChangeStreamUtils;
import io.vanillabp.cockpit.config.startup.CockpitConfiguration;
import io.vanillabp.cockpit.config.startup.MapKeyDotReplacement;
import io.vanillabp.cockpit.config.startup.SortIndexLimit;
import io.vanillabp.cockpit.util.SearchCriteriaHelper;
import io.vanillabp.cockpit.util.ListPaths;
import io.vanillabp.cockpit.util.SearchQuery;
import io.vanillabp.cockpit.util.SortIndexes;
import io.vanillabp.cockpit.util.kwic.KwicResult;
import io.vanillabp.cockpit.util.kwic.KwicService;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.cockpit.workflowlist.model.WorkflowRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.bson.Document;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
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

    public static final String INDEX_CUSTOM_SORT_PREFIX = SortIndexes.INDEX_PREFIX;
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

    /**
     * The top-level fields a list of workflows may be sorted and filtered by: every field of
     * {@code Workflow} in the GUI API which is stored under the same name. Keys of the business data
     * are allowed as well, see {@link ListPaths}.
     */
    public static final Set<String> FIELDS_OF_THE_LIST = Set.of(
            PROPERTY_ID, "version", "initiator", PROPERTY_CREATEDAT, "updatedAt", "endedAt",
            "workflowModuleId", "comment", "bpmnProcessId", "bpmnProcessVersion", "businessId", "title",
            "uiUriType", "accessibleToUsers", "accessibleToGroups", "detailsFulltextSearch");

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

    @Autowired
    private Environment environment;

    private ListPaths listPaths;

    private SortIndexes sortIndexes;

    private Subscription dbChangesSubscription;

    @PostConstruct
    protected void initializeTrackingOfIndexes() {

        listPaths = new ListPaths(
                FIELDS_OF_THE_LIST,
                MapKeyDotReplacement.configuredIn(environment));
        sortIndexes = new SortIndexes(
                Workflow.COLLECTION_NAME,
                listPaths,
                SortIndexLimit.configuredIn(environment),
                CockpitConfiguration.MONGODB_SORT_INDEXES_PER_COLLECTION,
                mongoTemplate,
                logger);
        sortIndexes.learnExistingIndexes();

    }

    /**
     * Stores what a workflow module reports about a case it has just started.
     * <p>
     * A creation of a case the cockpit already holds stores nothing but its start. It is the
     * oldest report there is, so everything else it says has been said again since. Its start is
     * stored where it is earlier than the one the cockpit holds, which a change that created the
     * case set to the time of that change. There is one exception: a case the cockpit learned
     * about from its end alone has been waiting for exactly this report.
     *
     * @param workflowId The case the report is about
     * @param eventTimestamp When the workflow module started the case, by its own clock
     * @param asReported The case as the report describes it
     * @return Whether the cockpit is up to date about the case, and if not, whether the same
     *         report can go through when it comes again
     */
    public OutcomeOfStoring reportCreatedWorkflow(
            final String workflowId,
            final OffsetDateTime eventTimestamp,
            final Supplier<Workflow> asReported) {

        final var stored = getWorkflow(workflowId);
        if (stored == null) {
            return storeReportedWorkflow(asReported.get(), eventTimestamp);
        }

        if (!stored.isKnownFromItsEndAlone()) {
            // a creation's timestamp is the moment the case began. A change which arrived first
            // and created the case gave it the time of that change as its start instead
            if (OrderOfReports.isEarlierThanTheStoredStart(eventTimestamp, stored.getCreatedAt())) {
                stored.setCreatedAt(eventTimestamp);
                return save(stored);
            }
            reportChangesNothing(workflowId, "creation", eventTimestamp, stored);
            return OutcomeOfStoring.upToDate();
        }

        // the creation's own start replaces the one the end gave the case, which is the same
        // where the end reported it, and the time of the end where it did not
        final var workflow = asReported.get();
        // the end is the younger report of the two and stays untouched, as does everything the
        // cockpit itself has recorded about the case since the end arrived
        workflow.setVersion(stored.getVersion());
        workflow.setEndedAt(stored.getEndedAt());
        workflow.setComment(stored.getComment());
        workflow.setInitiator(stored.getInitiator());
        workflow.setLatestEventAt(stored.getLatestEventAt());
        // this report is here for what an end could not say about the case. The other way round,
        // an end which did say it holds the younger answer. One rule decides both ways
        workflow.setDetails(
                WhatAnEndReports.whatToStore(stored.getDetails(), workflow.getDetails()));
        workflow.setDetailsFulltextSearch(
                WhatAnEndReports.whatToStore(stored.getDetailsFulltextSearch(), workflow.getDetailsFulltextSearch()));
        // the cockpit reported this case when the end arrived, so its own clock reading stands
        workflow.setReportedAt(stored.getReportedAt());
        return save(workflow);

    }

    /**
     * Stores what a workflow module reports about a change of a case.
     *
     * @param workflowId The case the report is about
     * @param eventTimestamp When the change happened, by the workflow module's clock
     * @param asReported The case as the report describes it, for a case the cockpit does not hold
     * @param ontoStored Lays the report onto the case the cockpit holds
     * @return Whether the cockpit is up to date about the case, and if not, whether the same
     *         report can go through when it comes again
     */
    public OutcomeOfStoring reportChangedWorkflow(
            final String workflowId,
            final OffsetDateTime eventTimestamp,
            final Supplier<Workflow> asReported,
            final Consumer<Workflow> ontoStored) {

        final var stored = getWorkflow(workflowId);
        // reporting a change of a case the cockpit never saw creates it, so a cockpit added to a
        // running system does not stay blind to the cases that existed before
        if (stored == null) {
            return storeReportedWorkflow(asReported.get(), eventTimestamp);
        }

        if (OrderOfReports.isOlderThanWhatIsStored(eventTimestamp, stored.getLatestEventAt())) {
            reportChangesNothing(workflowId, "change", eventTimestamp, stored);
            return OutcomeOfStoring.upToDate();
        }

        // a change never reopens a case: an end is mapped onto 'endedAt' by neither mapper
        ontoStored.accept(stored);
        stored.setLatestEventAt(eventTimestamp);
        return save(stored);

    }

    /**
     * Stores that a case has ended, completed or cancelled.
     * <p>
     * An end of a case the cockpit does not hold creates the case, ended. The creation may still
     * be waiting in the outbox of the workflow module. Dropping the end would leave the cockpit
     * showing that case as running for good once the creation arrives.
     * <p>
     * An end is recorded even where the cockpit holds something younger, because nothing which
     * comes after it undoes it. Whatever such an end reports besides the end itself is older than
     * what is stored, and it is left out.
     *
     * @param workflowId The case the report is about
     * @param eventTimestamp When the case ended, by the workflow module's clock
     * @param reportedStart When the case was started, as the end reports it, or {@code null}
     *        where it does not. Read only where the end creates the case
     * @param ontoStored Lays the report onto the case the cockpit holds, or onto an empty one
     * @return Whether the cockpit is up to date about the case, and if not, whether the same
     *         report can go through when it comes again
     */
    public OutcomeOfStoring reportEndedWorkflow(
            final String workflowId,
            final OffsetDateTime eventTimestamp,
            final OffsetDateTime reportedStart,
            final Consumer<Workflow> ontoStored) {

        final var stored = getWorkflow(workflowId);
        if (stored == null) {
            final var workflow = new Workflow();
            workflow.setId(workflowId);
            ontoStored.accept(workflow);
            // the user interface needs a start for every case. Where the end does not say when
            // the case began, the end is the earliest moment the cockpit knows of
            workflow.setCreatedAt(reportedStart != null ? reportedStart : eventTimestamp);
            workflow.setKnownFromItsEndAlone(true);
            workflow.setEndedAt(eventTimestamp);
            return storeReportedWorkflow(workflow, eventTimestamp);
        }

        if (stored.getEndedAt() != null) {
            reportChangesNothing(workflowId, "end", eventTimestamp, stored);
            return OutcomeOfStoring.upToDate();
        }

        if (!OrderOfReports.isOlderThanWhatIsStored(eventTimestamp, stored.getLatestEventAt())) {
            ontoStored.accept(stored);
            stored.setLatestEventAt(eventTimestamp);
        }
        stored.setEndedAt(eventTimestamp);
        return save(stored);

    }

    /**
     * Says that a report was read and stored nothing, which is the answer to a report the cockpit
     * has already been told something younger about. It belongs in the log, because it explains a
     * change a workflow module sent and nobody finds in the cockpit.
     */
    private void reportChangesNothing(
            final String workflowId,
            final String kindOfReport,
            final OffsetDateTime eventTimestamp,
            final Workflow stored) {

        logger.info(
                "Keeping workflow '{}' as it is: the reported {} happened at {}, and what is stored is about {}",
                workflowId,
                kindOfReport,
                eventTimestamp,
                stored.getEndedAt() != null
                        ? "the end of the case"
                        : stored.getLatestEventAt());

    }

    /** A case the cockpit stores for the first time. */
    private OutcomeOfStoring storeReportedWorkflow(
            final Workflow workflow,
            final OffsetDateTime eventTimestamp) {

        // the cockpit's own clock, see Workflow#getReportedAt
        workflow.setReportedAt(OffsetDateTime.now());
        workflow.setLatestEventAt(eventTimestamp);

        return save(workflow);

    }

    private OutcomeOfStoring save(
            final Workflow workflow) {

        try {
            workflowRepository.save(workflow);
            return OutcomeOfStoring.upToDate();
        } catch (Exception e) {
            // not logged here: the way the report came in logs it once, with what it knows of the
            // report besides, and says what happens to it next
            return OutcomeOfStoring.saveFailed("workflow '" + workflow.getId() + "'", e);
        }

    }

    /**
     * One workflow by its id, with nobody asked whether the caller may see it. This is the way in
     * for what the BPMS reports, which knows the ids it sent and belongs to no user. Everything
     * answering a person goes through the overload taking a {@link WorkflowVisibility}.
     */
    public Workflow getWorkflow(
            final String workflowId) {

        return workflowRepository
                .findById(workflowId)
                .orElse(null);

    }

    /**
     * The workflow of that id which the given visibility lets through, or {@code null} when there
     * is none. The visibility sits in the query rather than in a check around it, so there is no
     * way to the workflow which skips it.
     */
    public Workflow getWorkflow(
            final WorkflowVisibility visibility,
            final String workflowId) {

        return mongoTemplate.findOne(
                new Query(buildWorkflowlistCriteria(
                        visibility,
                        null,
                        RetrieveItemsMode.All,
                        List.of(Criteria.where("id").is(workflowId)),
                        null)),
                Workflow.class);

    }

    public Page<Workflow> getWorkflows(
            final int pageNumber,
            final int pageSize,
            final OffsetDateTime initialTimestamp,
            final WorkflowVisibility visibility,
            final Collection<String> businessIds,
            final Collection<SearchQuery> searchQueries,
            final String sort,
            final boolean sortAscending,
            final RetrieveItemsMode mode) {

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
                        visibility,
                        endedSince,
                        mode,
                        null,
                        businessIds));
        final var searchCriteria = filterCriteria(searchQueries);
        if (searchCriteria != null) {
            searchCriteria.forEach(query::addCriteria);
        }

        // build index before retrieving data if necessary
        sortIndexes.ensureIndex(orderBySort.indexName(), orderBySort.toBeIndexed());

        final var numberOfWorkflowsFound = mongoTemplate
                .count(Query.of(query).limit(-1).skip(-1), Workflow.class);
        final var workflowsFound = mongoTemplate
                .find(query.with(pageRequest), Workflow.class);

        return PageableExecutionUtils.getPage(
                workflowsFound,
                pageRequest,
                () -> numberOfWorkflowsFound);

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
                .peek(sortIndexes::checkPath)
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
            final WorkflowVisibility visibility,
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
                        visibility,
                        initialTimestamp,
                        effectiveMode,
                        null,
			null));
        final var searchCriteria = filterCriteria(searchQueries);
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

    @EventListener
    public void subscribeToDbChanges(
            final ApplicationStartedEvent event) {

        dbChangesSubscription = changeStreamUtils.subscribe(
                Workflow.class,
                this::publishWorkflowChange);

    }

    /**
     * What a change to a workflow becomes: one notification per change event. Nothing is caught
     * here, for the reason given at {@code UserTaskService#publishUserTaskChange}.
     */
    private void publishWorkflowChange(
            final Message<ChangeStreamDocument<Document>, Workflow> message) {

        applicationEventPublisher.publishEvent(
                WorkflowChangedNotification.build(message));

    }

    public List<KwicResult> kwic(
            final OffsetDateTime endedSince,
            final WorkflowVisibility visibility,
            final Collection<SearchQuery> searchQueries,
            final String path,
            final String query) {

        listPaths.check("path", path);
        listPaths.checkFilters(searchQueries);
        if (!StringUtils.hasText(query)
                || (query.length() < 3)) {
            return List.of();
        }

        final var searchCriteria = new LinkedList<Criteria>();
        searchCriteria.add(new Criteria(path).regex(query, "i"));
        final var match =
                buildWorkflowlistCriteria(
                        visibility,
                        endedSince,
                        RetrieveItemsMode.Active,
                        searchCriteria,
			null);

        return kwicService.getKwicAggregatedResults(Workflow.class, match, searchQueries, path, query);
    }

    public CriteriaDefinition buildWorkflowlistCriteria(
            final WorkflowVisibility visibility,
            final OffsetDateTime initialTimestamp,
            final RetrieveItemsMode mode,
            final List<Criteria> predefinedCriterias,
	    final Collection<String> businessIds) {

        final var includeDanglingWorkflows = visibility.includeDanglingWorkflows();
        final var accessibleToUsers = visibility.accessibleToUsers();
        final var accessibleToGroups = visibility.accessibleToGroups();

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

    /**
     * The conditions of the filters of a list request, after their paths are checked.
     *
     * @throws io.vanillabp.cockpit.commons.exceptions.BcInvalidRequestException if a path breaks
     *         the rule of {@link ListPaths}
     */
    private List<? extends CriteriaDefinition> filterCriteria(
            final Collection<SearchQuery> searchQueries) {

        listPaths.checkFilters(searchQueries);
        return SearchCriteriaHelper.buildSearchCriteria(searchQueries);

    }

}
