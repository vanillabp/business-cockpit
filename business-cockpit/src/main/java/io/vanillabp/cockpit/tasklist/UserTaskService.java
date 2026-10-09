package io.vanillabp.cockpit.tasklist;

import com.mongodb.client.model.changestream.ChangeStreamDocument;
import io.vanillabp.cockpit.bpms.WhatAnEndReports;
import io.vanillabp.cockpit.bpms.OrderOfReports;
import io.vanillabp.cockpit.bpms.OutcomeOfStoring;
import io.vanillabp.cockpit.bpms.WhoSeesAUserTask;
import io.vanillabp.cockpit.commons.exceptions.BcUnauthorizedException;
import io.vanillabp.cockpit.commons.mongo.changestreams.ChangeStreamUtils;
import io.vanillabp.cockpit.commons.mongo.updateinfo.UpdateInformationAware;
import io.vanillabp.cockpit.commons.security.usercontext.UserContext;
import io.vanillabp.cockpit.config.startup.CockpitConfiguration;
import io.vanillabp.cockpit.config.startup.MapKeyDotReplacement;
import io.vanillabp.cockpit.config.startup.SortIndexLimit;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.cockpit.tasklist.model.UserTaskEndReason;
import io.vanillabp.cockpit.tasklist.model.UserTaskRepository;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.util.SearchCriteriaHelper;
import io.vanillabp.cockpit.util.ListPaths;
import io.vanillabp.cockpit.util.SearchQuery;
import io.vanillabp.cockpit.util.SortIndexes;
import io.vanillabp.cockpit.util.kwic.KwicResult;
import io.vanillabp.cockpit.util.kwic.KwicService;
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
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.messaging.Message;
import org.springframework.data.mongodb.core.messaging.Subscription;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserTaskService {

    public static final String INDEX_CUSTOM_SORT_PREFIX = SortIndexes.INDEX_PREFIX;
    public static final String PROPERTY_DUEDATE = "dueDate";
    public static final String PROPERTY_CREATEDAT = "createdAt";
    public static final String PROPERTY_ID = "id";

    public static enum RetrieveItemsMode {
        All,
        /** Every task which has not ended, whatever its follow-up date says. */
        OpenTasks,
        /** Open tasks without a follow-up date, or with one which is due. */
        OpenTasksWithoutFollowUp,
        /**
         * Another name for {@link #OpenTasks}, which shows the same tasks. The name sounds like a
         * filter, but there is none. It stays because the GUI API offers it. See decision 58 in
         * the repository's DECISIONS.md.
         */
        OpenTasksWithFollowUp,
        OpenTaskOnlyFollowUp,
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

    /**
     * The top-level fields a list of user tasks may be sorted and filtered by: every field of
     * {@code UserTask} in the GUI API which is stored under the same name. Keys of the business data
     * are allowed as well, see {@link ListPaths}.
     */
    public static final Set<String> FIELDS_OF_THE_LIST = Set.of(
            "id", "version", "initiator", "createdAt", "updatedAt", "endedAt", "workflowModuleId",
            "comment", "bpmnProcessId", "bpmnProcessVersion", "workflowTitle", "workflowId",
            "businessId", "title", "bpmnTaskId", "taskDefinition", "taskDefinitionTitle", "uiUriType",
            "assignee", "candidateUsers", "candidateGroups", PROPERTY_DUEDATE, "followUpDate",
            "detailsFulltextSearch");

    @Autowired
    private Logger logger;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private ChangeStreamUtils changeStreamUtils;

    @Autowired
    private UserTaskRepository userTasks;

    @Autowired
    private KwicService kwicService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private UserContext currentUserContext;

    @Autowired
    private Environment environment;

    private ListPaths listPaths;

    private SortIndexes sortIndexes;

    private Subscription dbChangesSubscription;

    /**
     * The initiator recorded for a change the Business Cockpit itself caused. It is the user who
     * is logged in, or {@link UpdateInformationAware#COCKPIT_USER} where there is none, a job of
     * the cockpit for example. It is kept apart from
     * {@link UpdateInformationAware#SYSTEM_USER}, which marks a change the workflow system
     * reported.
     * <p>
     * The initiator is the only record of who caused the latest change. {@code updatedBy} is audit
     * information, and {@code UpdateInformationEventListener} overwrites it on every save. The
     * notification poller reads the initiator to skip a notification a user triggered themselves,
     * so every change made in the cockpit has to keep it up to date.
     */
    private String cockpitInitiator() {

        final String userLoggedIn;
        try {
            userLoggedIn = currentUserContext.getUserLoggedIn();
        } catch (BcUnauthorizedException e) {
            return UpdateInformationAware.COCKPIT_USER;
        }
        return userLoggedIn == null
                ? UpdateInformationAware.COCKPIT_USER
                : userLoggedIn;

    }

    /** Saves a user task changed by a cockpit action, recording the acting user as initiator. */
    private UserTask saveInitiatedByCockpit(
            final UserTask userTask) {

        userTask.setInitiator(cockpitInitiator());
        return userTasks.save(userTask);

    }

    /** @see #saveInitiatedByCockpit(UserTask) */
    private List<UserTask> saveAllInitiatedByCockpit(
            final List<UserTask> changedUserTasks) {

        final var initiator = cockpitInitiator();
        changedUserTasks.forEach(userTask -> userTask.setInitiator(initiator));
        return userTasks.saveAll(changedUserTasks);

    }

    @PostConstruct
    protected void initializeTrackingOfIndexes() {

        listPaths = new ListPaths(
                FIELDS_OF_THE_LIST,
                MapKeyDotReplacement.configuredIn(environment));
        sortIndexes = new SortIndexes(
                UserTask.COLLECTION_NAME,
                listPaths,
                SortIndexLimit.configuredIn(environment),
                CockpitConfiguration.MONGODB_SORT_INDEXES_PER_COLLECTION,
                mongoTemplate,
                logger);
        sortIndexes.learnExistingIndexes();

    }

    @EventListener
    public void subscribeToDbChanges(
            final ApplicationStartedEvent event) {

        dbChangesSubscription = changeStreamUtils.subscribe(
                UserTask.class,
                this::publishUserTaskChange);

    }

    /**
     * What a change to a user task becomes: one notification per change event.
     * <p>
     * Nothing is caught here. A failure is reported by {@code ChangeStreamUtils}, which knows the
     * collection, the document and the operation, and the stream resumes there as well. Catching it
     * twice used to mean two log lines, neither of which said which change was lost.
     */
    private void publishUserTaskChange(
            final Message<ChangeStreamDocument<Document>, UserTask> message) {

        applicationEventPublisher.publishEvent(
                UserTaskChangedNotification.build(message));

    }

    @PreDestroy
    public void cleanup() {

        if (dbChangesSubscription != null) {
            changeStreamUtils.unsubscribe(dbChangesSubscription);
        }

    }

    /**
     * One user task by its id, with nobody asked whether the caller may see it. This is the way in
     * for what the BPMS reports, which knows the ids it sent and belongs to no user. Everything
     * answering a person goes through the overload taking a {@link UserTaskVisibility}.
     */
    public UserTask getUserTask(
            final String userTaskId) {

        return userTasks
                .findById(userTaskId)
                .orElse(null);

    }

    /**
     * The task of that id which the given visibility lets through, or {@code null} when there is
     * none. The visibility sits in the query and is not a filter running afterwards, which is what
     * makes the check impossible to skip. Holding an id is not enough: a caller has to name a view
     * the task belongs to.
     */
    public UserTask getUserTask(
            final UserTaskVisibility visibility,
            final String userTaskId) {

        return mongoTemplate.findOne(
                new Query(buildUserTasksCriteria(
                        visibility,
                        null,
                        RetrieveItemsMode.All,
                        List.of(Criteria.where("id").is(userTaskId)))),
                UserTask.class);

    }

    /**
     * The tasks of those ids which the given visibility lets through. The ones it does not are left
     * out silently, the same way a list leaves them out.
     */
    public List<UserTask> getUserTasks(
            final UserTaskVisibility visibility,
            final Collection<String> userTaskIds) {

        return mongoTemplate.find(
                new Query(buildUserTasksCriteria(
                        visibility,
                        null,
                        RetrieveItemsMode.All,
                        List.of(Criteria.where("id").in(userTaskIds)))),
                UserTask.class);

    }

    public UserTask markAsRead(
            final UserTaskVisibility visibility,
            final String userTaskId,
            final String userId) {

        final var userTask = getUserTask(visibility, userTaskId);
        if (userTask == null) {
            return null;
        }
        userTask.setReadAt(userId);
        return saveInitiatedByCockpit(userTask);

    }

    public List<UserTask> markAsRead(
            final UserTaskVisibility visibility,
            final Collection<String> userTaskIds,
            final String userId) {

        final var found = getUserTasks(visibility, userTaskIds);
        found.forEach(userTask -> userTask.setReadAt(userId));
        return saveAllInitiatedByCockpit(found);

    }

    public UserTask markAsUnread(
            final UserTaskVisibility visibility,
            final String userTaskId,
            final String userId) {

        final var userTask = getUserTask(visibility, userTaskId);
        if (userTask == null) {
            return null;
        }
        userTask.clearReadAt(userId);
        return saveInitiatedByCockpit(userTask);

    }

    public List<UserTask> markAsUnread(
            final UserTaskVisibility visibility,
            final Collection<String> userTaskIds,
            final String userId) {

        final var found = getUserTasks(visibility, userTaskIds);
        found.forEach(userTask -> userTask.clearReadAt(userId));
        return saveAllInitiatedByCockpit(found);

    }

    public UserTask assignTask(
            final UserTaskVisibility visibility,
            final String userTaskId,
            final Person person) {

        final var userTask = getUserTask(visibility, userTaskId);
        if (userTask == null) {
            return null;
        }
        userTask.addCandidatePerson(person);
        return saveInitiatedByCockpit(userTask);

    }

    public List<UserTask> assignTask(
            final UserTaskVisibility visibility,
            final Collection<String> userTaskIds,
            final Person person) {

        final var found = getUserTasks(visibility, userTaskIds);
        found.forEach(userTask -> userTask.addCandidatePerson(person));
        return saveAllInitiatedByCockpit(found);

    }

    public UserTask unassignTask(
            final UserTaskVisibility visibility,
            final String userTaskId,
            final String personId) {

        final var userTask = getUserTask(visibility, userTaskId);
        if (userTask == null) {
            return null;
        }
        userTask.removeCandidatePerson(personId);
        return saveInitiatedByCockpit(userTask);

    }

    public List<UserTask> unassignTask(
            final UserTaskVisibility visibility,
            final Collection<String> userTaskIds,
            final String personId) {

        final var found = getUserTasks(visibility, userTaskIds);
        found.forEach(userTask -> userTask.removeCandidatePerson(personId));
        return saveAllInitiatedByCockpit(found);

    }

    public UserTask setFollowUpDate(
            final UserTaskVisibility visibility,
            final String userTaskId,
            final OffsetDateTime followUpDate) {

        final var normalizedFollowUpDate = followUpDate == null
                ? null
                : followUpDate.withSecond(0).withNano(0);

        final var userTask = getUserTask(visibility, userTaskId);
        if (userTask == null) {
            return null;
        }
        if (userTask.getEndedAt() != null) {
            throw new UserTaskAlreadyCompletedException(userTaskId);
        }
        userTask.setFollowUpDate(normalizedFollowUpDate);
        return saveInitiatedByCockpit(userTask);

    }

    public UserTask claimTask(
            final UserTaskVisibility visibility,
            final String userTaskId,
            final Person person) {

        final var userTask = getUserTask(visibility, userTaskId);
        if (userTask == null) {
            return null;
        }
        if ((userTask.getAssignee() == null)
                || !userTask.getAssignee().getId().equals(person.getId())) {
            userTask.setAssignee(person);
            return saveInitiatedByCockpit(userTask);
        }
        return userTask;

    }

    public List<UserTask> claimTask(
            final UserTaskVisibility visibility,
            final Collection<String> userTaskIds,
            final Person person) {

        final var found = getUserTasks(visibility, userTaskIds);
        found.forEach(userTask -> userTask.setAssignee(person));
        return saveAllInitiatedByCockpit(found);

    }

    /**
     * Giving a task back is the one change which can take the task out of the very view it was
     * made from. A list of what is mine holds nothing of mine any more once I let go. So the
     * visibility decides whether the change may happen, and what comes back afterwards is read
     * without it. Answering {@code null} would tell the caller their own action failed.
     */
    public UserTask unclaimTask(
            final UserTaskVisibility visibility,
            final String currentUser,
            final String userTaskId,
            final String personId) {

        if (getUserTask(visibility, userTaskId) == null) {
            return null;
        }

        final var query = new Query();
        query.addCriteria(Criteria.where("id").is(userTaskId));
        query.addCriteria(Criteria.where("assignee.id").is(personId));

        mongoTemplate.updateFirst(query, unsetAssignee(currentUser), UserTask.class);

        return getUserTask(userTaskId);

    }

    public List<UserTask> unclaimTask(
            final UserTaskVisibility visibility,
            final String currentUser,
            final Collection<String> userTaskIds,
            final String personId) {

        final var unclaimable = getUserTasks(visibility, userTaskIds)
                .stream()
                .map(UserTask::getId)
                .toList();
        if (unclaimable.isEmpty()) {
            return List.of();
        }

        final var query = new Query();
        query.addCriteria(Criteria.where("id").in(unclaimable));
        query.addCriteria(Criteria.where("assignee.id").is(personId));

        mongoTemplate.updateMulti(query, unsetAssignee(currentUser), UserTask.class);

        final var findQuery = new Query();
        findQuery.addCriteria(Criteria.where("id").in(unclaimable));

        return mongoTemplate.find(findQuery, UserTask.class);

    }

    private Update unsetAssignee(
            final String currentUser) {

        final var update = new Update();
        update.unset("assignee");
        update.set("updatedAt", OffsetDateTime.now());
        update.set("updatedBy", currentUser == null ? UpdateInformationAware.SYSTEM_USER : currentUser);
        update.set("initiator", currentUser == null ? UpdateInformationAware.COCKPIT_USER : currentUser);
        return update;

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

        return new UserTaskListOrder(order, sort, indexProps);

    }

    public Page<UserTask> getUserTasks(
            final UserTaskVisibility visibility,
            final int pageNumber,
            final int pageSize,
            final OffsetDateTime initialTimestamp,
            final Collection<SearchQuery> searchQueries,
            final String sort,
            final boolean sortAscending,
            final RetrieveItemsMode mode) {

        return retrieveUserTasks(
                visibility,
                pageNumber,
                pageSize,
                initialTimestamp,
                searchQueries,
                sort,
                sortAscending,
                null,
                mode);

    }

    protected Page<UserTask> retrieveUserTasks(
            final UserTaskVisibility visibility,
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
        final var searchCriteria = filterCriteria(searchQueries);
        query.addCriteria(
                buildUserTasksCriteria(
                        visibility,
                        initialTimestamp,
                        mode,
                        predefinedCriterias));
        if (searchCriteria != null) {
            searchCriteria.forEach(query::addCriteria);
        }

        // build index before retrieving data if necessary
        sortIndexes.ensureIndex(orderBySort.indexName(), orderBySort.toBeIndexed());

        final var numberOfUserTasksFound = mongoTemplate
                .count(Query.of(query).limit(-1).skip(-1), UserTask.class);
        final var userTasksFound = mongoTemplate
                .find(query.with(pageRequest), UserTask.class);

        return PageableExecutionUtils.getPage(
                userTasksFound,
                pageRequest,
                () -> numberOfUserTasksFound);

    }

    public List<KwicResult> kwic(
            final UserTaskVisibility visibility,
            final OffsetDateTime initialTimestamp,
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
                buildUserTasksCriteria(
                        visibility,
                        initialTimestamp,
                        RetrieveItemsMode.OpenTasks,
                        searchCriteria);

        return kwicService.getKwicAggregatedResults(UserTask.class, match, searchQueries, path, query);
    }

    public List<UserTask> getUserTasksOfWorkflow(
            final String workflowId,
            final boolean activeOnly,
            final boolean limitListAccordingToCurrentUsersPermissions,
            final String currentUser,
            final Collection<String> currentUserGroups,
            final int size,
            final String sort,
            final boolean sortAscending) {

        return retrieveUserTasks(
                    limitListAccordingToCurrentUsersPermissions
                            ? UserTaskVisibility.everythingTheUserMayWorkOn(currentUser, currentUserGroups)
                            : UserTaskVisibility.everyUserTask(),
                    0,
                    size,
                    OffsetDateTime.now(),
                    null,
                    sort,
                    sortAscending,
                    List.of(Criteria.where("workflowId").is(workflowId)),
                    activeOnly ? RetrieveItemsMode.OpenTasks : RetrieveItemsMode.All
                ).getContent();

    }

    public Page<UserTask> getUserTasksUpdated(
            final UserTaskVisibility visibility,
            final int size,
            final Collection<String> knownUserTasksIds,
            final OffsetDateTime initialTimestamp,
            final Collection<SearchQuery> searchQueries,
            final String sort,
            final boolean sortAscending,
            final RetrieveItemsMode mode) {

        final var orderBySort = getUserTaskListOrder(sort, sortAscending);
        final var pageRequest = PageRequest
                .ofSize(size)
                .withPage(0)
                .withSort(Sort.by(orderBySort.order()));

        final var effectiveMode = mode != null ? mode : RetrieveItemsMode.OpenTasks;

        final var query = new Query();
        query.fields().include("_id");
        query.addCriteria(
                buildUserTasksCriteria(
                        visibility,
                        initialTimestamp,
                        effectiveMode,
                        null));
        final var searchCriteria = filterCriteria(searchQueries);
        if (searchCriteria != null) {
            searchCriteria.forEach(query::addCriteria);
        }
        final var numberOfUserTasks = mongoTemplate
                .count(Query.of(query).limit(-1).skip(-1), UserTask.class);

        // the query only fetches ids; tasks the client does not know yet are loaded completely
        final var result = mongoTemplate
                .find(query.with(pageRequest), UserTask.class)
                .stream()
                .map(task -> knownUserTasksIds.contains(task.getId())
                        ? Optional.of(task)
                        : userTasks.findById(task.getId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        return new PageImpl<>(
                result,
                Pageable
                        .ofSize(result.isEmpty() ? 1 : result.size())
                        .withPage(0),
                numberOfUserTasks);

    }

    /**
     * Stores what a workflow module reports about a user task it has just created.
     * <p>
     * A creation of a task the cockpit already holds stores nothing but its start. It is the
     * oldest report there is, so everything else it says has been said again since. Storing it
     * would throw away what the cockpit itself knows about the task: who took it over, who read
     * it, and that it has ended. Its start is stored where it is earlier than the one the cockpit
     * holds, which a change that created the task set to the time of that change. There is one
     * exception: a task the cockpit learned about from its end alone has been waiting for exactly
     * this report. It takes everything the creation reports, except who sees the task where the end
     * said it. A task which names nobody, because the report which created it did not say who
     * sees it, takes that from the creation as well ({@link WhoSeesAUserTask}).
     *
     * @param userTaskId The task the report is about
     * @param eventTimestamp When the workflow module created the task, by its own clock
     * @param asReported The task as the report describes it
     * @return Whether the cockpit is up to date about the task, and if not, whether the same
     *         report can go through when it comes again
     */
    public OutcomeOfStoring reportCreatedUserTask(
            final String userTaskId,
            final OffsetDateTime eventTimestamp,
            final Supplier<UserTask> asReported) {

        final var stored = getUserTask(userTaskId);
        if (stored == null) {
            return storeReportedUserTask(asReported.get(), eventTimestamp);
        }

        if (!stored.isKnownFromItsEndAlone()) {
            // the change which created the task said who sees it, unless it named nobody
            final var tookWhoSeesTheTask = WhoSeesAUserTask.takeFromALateCreation(stored, asReported);
            // a creation's timestamp is the moment the task began. A change which arrived first
            // and created the task gave it the time of that change as its start instead
            final var tookTheStart = OrderOfReports.isEarlierThanTheStoredStart(
                    eventTimestamp, stored.getCreatedAt());
            if (tookTheStart) {
                stored.setCreatedAt(eventTimestamp);
            }
            if (tookWhoSeesTheTask || tookTheStart) {
                return save(stored);
            }
            reportChangesNothing(userTaskId, "creation", eventTimestamp, stored);
            return OutcomeOfStoring.upToDate();
        }

        // the creation's own start replaces the one the end gave the task, which is the same
        // where the end reported it, and the time of the end where it did not
        final var task = asReported.get();
        // the end is the younger report of the two and stays untouched, as does everything the
        // cockpit itself has recorded about the task since the end arrived
        task.setVersion(stored.getVersion());
        task.setEndedAt(stored.getEndedAt());
        task.setEndReason(stored.getEndReason());
        task.setComment(stored.getComment());
        task.setInitiator(stored.getInitiator());
        task.setReadBy(stored.getReadBy());
        task.setLatestEventAt(stored.getLatestEventAt());
        // this report is here for what an end could not say about the task. The other way round,
        // an end which did say it holds the younger answer. One rule decides both ways
        task.setDetails(
                WhatAnEndReports.whatToStore(stored.getDetails(), task.getDetails()));
        task.setDetailsFulltextSearch(
                WhatAnEndReports.whatToStore(stored.getDetailsFulltextSearch(), task.getDetailsFulltextSearch()));
        // the end created the task, so who sees it is what the end said, where it said anything
        WhoSeesAUserTask.keepWhatTheEndSaid(stored, task);
        // the cockpit reported this task when the end arrived, so its own clock reading stands
        task.setReportedAt(stored.getReportedAt());
        // the candidates this report brings along become known to the cockpit now
        task.stampCandidatesSince(OffsetDateTime.now());
        return save(task);

    }

    /**
     * Stores what a workflow module reports about a change of a user task.
     * <p>
     * A change of a task the cockpit holds keeps who sees the task. Only a change which creates the
     * task says who sees it, or a change of a task which names nobody ({@link WhoSeesAUserTask}).
     *
     * @param userTaskId The task the report is about
     * @param eventTimestamp When the change happened, by the workflow module's clock
     * @param asReported The task as the report describes it, for a task the cockpit does not hold
     * @param ontoStored Lays the report onto the task the cockpit holds
     * @return Whether the cockpit is up to date about the task, and if not, whether the same
     *         report can go through when it comes again
     */
    public OutcomeOfStoring reportChangedUserTask(
            final String userTaskId,
            final OffsetDateTime eventTimestamp,
            final Supplier<UserTask> asReported,
            final Consumer<UserTask> ontoStored) {

        final var stored = getUserTask(userTaskId);
        // reporting a change of a task the cockpit never saw creates it, so a cockpit added to a
        // running system does not stay blind to the tasks that existed before
        if (stored == null) {
            return storeReportedUserTask(asReported.get(), eventTimestamp);
        }

        if (OrderOfReports.isOlderThanWhatIsStored(eventTimestamp, stored.getLatestEventAt())) {
            reportChangesNothing(userTaskId, "change", eventTimestamp, stored);
            return OutcomeOfStoring.upToDate();
        }

        // a change never reopens a task: an end is mapped onto neither 'endedAt' nor 'endReason'.
        // And it never changes who sees the task
        WhoSeesAUserTask.keepWhoSeesTheStoredTask(stored, "change", ontoStored);
        stored.setLatestEventAt(eventTimestamp);
        return save(stored);

    }

    /**
     * Stores that a user task has ended, completed or cancelled.
     * <p>
     * An end of a task the cockpit does not hold creates the task, ended. The creation may still
     * be waiting in the outbox of the workflow module. Dropping the end would leave the cockpit
     * showing that task as open for good once the creation arrives.
     * <p>
     * An end is recorded even where the cockpit holds something younger, because nothing which
     * comes after it undoes it. Whatever such an end reports besides the end itself is older than
     * what is stored, and it is left out.
     * <p>
     * An end of a task the cockpit holds keeps who sees the task. Only an end which creates the task
     * says who sees it, or an end of a task which names nobody ({@link WhoSeesAUserTask}).
     *
     * @param userTaskId The task the report is about
     * @param eventTimestamp When the task ended, by the workflow module's clock
     * @param endReason Whether the task was completed or cancelled
     * @param reportedStart When the task was created, as the end reports it, or {@code null}
     *        where it does not. Read only where the end creates the task
     * @param ontoStored Lays the report onto the task the cockpit holds, or onto an empty one
     * @return Whether the cockpit is up to date about the task, and if not, whether the same
     *         report can go through when it comes again
     */
    public OutcomeOfStoring reportEndedUserTask(
            final String userTaskId,
            final OffsetDateTime eventTimestamp,
            final UserTaskEndReason endReason,
            final OffsetDateTime reportedStart,
            final Consumer<UserTask> ontoStored) {

        final var stored = getUserTask(userTaskId);
        if (stored == null) {
            final var task = new UserTask();
            task.setId(userTaskId);
            ontoStored.accept(task);
            // the user interface needs a start for every task. Where the end does not say when
            // the task began, the end is the earliest moment the cockpit knows of
            task.setCreatedAt(reportedStart != null ? reportedStart : eventTimestamp);
            task.setKnownFromItsEndAlone(true);
            endUserTask(task, eventTimestamp, endReason);
            return storeReportedUserTask(task, eventTimestamp);
        }

        if (stored.getEndedAt() != null) {
            reportChangesNothing(userTaskId, "end", eventTimestamp, stored);
            return OutcomeOfStoring.upToDate();
        }

        if (!OrderOfReports.isOlderThanWhatIsStored(eventTimestamp, stored.getLatestEventAt())) {
            WhoSeesAUserTask.keepWhoSeesTheStoredTask(stored, "end", ontoStored);
            stored.setLatestEventAt(eventTimestamp);
        }
        endUserTask(stored, eventTimestamp, endReason);
        return save(stored);

    }

    private static void endUserTask(
            final UserTask userTask,
            final OffsetDateTime timestamp,
            final UserTaskEndReason endReason) {

        userTask.setEndedAt(timestamp);
        userTask.setEndReason(endReason);

    }

    /**
     * Says that a report was read and stored nothing. It belongs in the log, because it explains
     * a change a workflow module sent and nobody finds in the cockpit.
     */
    private void reportChangesNothing(
            final String userTaskId,
            final String kindOfReport,
            final OffsetDateTime eventTimestamp,
            final UserTask stored) {

        logger.info(
                "Keeping user task '{}' as it is: the reported {} happened at {}, and what is stored is about {}",
                userTaskId,
                kindOfReport,
                eventTimestamp,
                stored.getEndedAt() != null
                        ? "the end of the task"
                        : stored.getLatestEventAt());

    }

    /**
     * The task list is sorted by the due date, so a task without one needs a reading which sorts
     * behind every real date rather than an empty field. MongoDB sorts an empty field to the front
     * of an ascending list, which would put the tasks nobody set a date for ahead of the ones which
     * are almost late.
     * <p>
     * Every report the cockpit stores passes here, so a workflow module cannot leave a stored
     * task without a reading. It cannot do so by reporting a change of a task whose due date the
     * process removed, and not by ending a task, which keeps the date it does not report. The
     * reading is an internal one, and the GUI mapper turns it back into an empty date.
     */
    private static void keepSortableByDueDate(
            final UserTask userTask) {

        if (userTask.getDueDate() == null) {
            userTask.setDueDate(OffsetDateTime.MAX);
        }

    }

    /** A user task the cockpit stores for the first time. */
    private OutcomeOfStoring storeReportedUserTask(
            final UserTask userTask,
            final OffsetDateTime eventTimestamp) {

        // the cockpit's own clock: 'createdAt' is the reporting system's timestamp and may lag
        // behind (or run ahead of) this one, which would break delta-scanning for notifications
        final var reportedAt = OffsetDateTime.now();
        userTask.setReportedAt(reportedAt);
        // the candidates a report brings along are known as of now
        userTask.stampCandidatesSince(reportedAt);
        userTask.setLatestEventAt(eventTimestamp);

        return save(userTask);

    }

    private OutcomeOfStoring save(
            final UserTask userTask) {

        keepSortableByDueDate(userTask);

        try {
            userTasks.save(userTask);
            return OutcomeOfStoring.upToDate();
        } catch (Exception e) {
            // not logged here: the way the report came in logs it once, with what it knows of the
            // report besides, and says what happens to it next
            return OutcomeOfStoring.saveFailed("user task '" + userTask.getId() + "'", e);
        }

    }

    /**
     * The workflows the given user has visible tasks for, each one once. A workflow here is the
     * workflow module and the BPMN process of a business case, together with a title, and the
     * visibility is the one the user task list uses. The page for configuring notifications reads
     * it to offer an exception per workflow.
     * <p>
     * A task counts for the process of its case, not for the process it sits in. A task of a
     * process the case started by a call activity is a step of that case, so it adds no workflow
     * of its own. The case is the workflow stored under the task's {@code workflowId}, and a task
     * whose workflow the cockpit does not hold counts for its own process. That is the rule of
     * {@link io.vanillabp.cockpit.notification.CaseProcess}, written here as a query.
     * <p>
     * The title is the {@code workflowTitle} of a task which sits in the case's own process. Only
     * where the user sees no such task, it is the title of a task of a called process.
     */
    public List<UserTask> getVisibleWorkflows(
            final UserTaskVisibility visibility) {

        final var criteria = buildUserTasksCriteria(
                visibility, null, RetrieveItemsMode.All, List.of());

        final var caseModule = ConditionalOperators
                .ifNull(ArrayOperators.ArrayElemAt.arrayOf("theCase.workflowModuleId").elementAt(0))
                .thenValueOf("workflowModuleId");
        final var caseProcess = ConditionalOperators
                .ifNull(ArrayOperators.ArrayElemAt.arrayOf("theCase.bpmnProcessId").elementAt(0))
                .thenValueOf("bpmnProcessId");
        // typed, so that the criteria are mapped the way the list of user tasks maps them. A
        // nested 'id', the one of a candidate group for example, is stored as '_id'
        final var aggregation = Aggregation.newAggregation(
                UserTask.class,
                Aggregation.match(criteria),
                Aggregation.lookup(Workflow.COLLECTION_NAME, "workflowId", "_id", "theCase"),
                Aggregation.addFields()
                        .addField("caseModuleId").withValueOf(caseModule)
                        .addField("caseBpmnProcessId").withValueOf(caseProcess)
                        .build(),
                Aggregation.addFields()
                        .addField("sitsInTheCaseProcess")
                        .withValueOf(ComparisonOperators.Eq.valueOf("bpmnProcessId").equalTo("caseBpmnProcessId"))
                        .build(),
                // a task of the case's own process first, so that its title names the group
                Aggregation.sort(Sort.by(Sort.Direction.DESC, "sitsInTheCaseProcess")),
                Aggregation
                        .group("caseModuleId", "caseBpmnProcessId")
                        .first("caseModuleId").as("workflowModuleId")
                        .first("caseBpmnProcessId").as("bpmnProcessId")
                        .first("workflowTitle").as("workflowTitle"));

        return mongoTemplate
                .aggregate(aggregation, UserTask.class)
                .getMappedResults();

    }

    /**
     * The query behind every list and every request naming a single task. The part which honours
     * the user's permissions reads: the task admits the user, or it names them as assignee,
     * candidate user or candidate group, or it addresses nobody, and it does not exclude them. So
     * an exclusion cancels each of the other reasons, while an admission stands next to all of
     * them and is not cancelled. {@link UserTaskVisibility} says why.
     */
    public Criteria buildUserTasksCriteria(
            final UserTaskVisibility visibility,
            final OffsetDateTime initialTimestamp,
            final RetrieveItemsMode mode,
            final List<Criteria> predefinedCriterias) {

        final var includeDanglingTasks = visibility.includeDanglingTasks();
        final var notInAssignees = visibility.notInAssignees();
        final var assignees = visibility.assignees();
        final var candidateUsers = visibility.candidateUsers();
        final var candidateGroups = visibility.candidateGroups();
        final var candidatesToBeExcluded = visibility.excludedCandidates();
        final var admittedUsers = visibility.admittedUsers();

        final var subCriterias = new LinkedList<Criteria>();

        // honour user's permissions

        // a task is visible for one of these reasons, and an exclusion cancels each of them
        final var reasonsAnExclusionCancels = new LinkedList<Criteria>();
        // an admission is the reason no exclusion reaches, so it stands beside the group above
        final var reasonsNoExclusionCancels = new LinkedList<Criteria>();
        // what the task says about keeping somebody out, weighed against the group above only
        final var exclusions = new LinkedList<Criteria>();
        // this one holds whatever the reason is, because it says which view was asked for
        final var restrictionsOnTheAssignee = new LinkedList<Criteria>();

        if ((assignees != null)
                && !assignees.isEmpty()) {
            if (notInAssignees) {
                final var assigneeMatches = Criteria.where("assignee.id").not().in(assignees);
                restrictionsOnTheAssignee.add(assigneeMatches);
            } else {
                final var assigneeMatches = Criteria.where("assignee.id").in(assignees);
                reasonsAnExclusionCancels.add(assigneeMatches);
            }
        } else if (notInAssignees) {
            final var assigneeMatches = Criteria.where("assignee").exists(false);
            restrictionsOnTheAssignee.add(assigneeMatches);
        }

        if ((candidateUsers != null)
                && !candidateUsers.isEmpty()) {
            final var candidateUsersMatches = Criteria.where("candidateUsers.id").in(candidateUsers);
            reasonsAnExclusionCancels.add(candidateUsersMatches);
        }
        if ((candidateGroups != null)
                && !candidateGroups.isEmpty()) {
            final var candidateGroupsMatches = Criteria.where("candidateGroups.id").in(candidateGroups);
            reasonsAnExclusionCancels.add(candidateGroupsMatches);
        }
        if ((admittedUsers != null)
                && !admittedUsers.isEmpty()) {
            // the workflow module let these users through, whether or not they are candidates
            final var admittedUsersMatches = Criteria.where("admittedUsers.id").in(admittedUsers);
            reasonsNoExclusionCancels.add(admittedUsersMatches);
        }

        if(candidatesToBeExcluded != null && !candidatesToBeExcluded.isEmpty()){
            final var candidateUserExclusions =
                    Criteria.where("excludedCandidateUsers.id")
                            .not().in(candidatesToBeExcluded);
            exclusions.add(candidateUserExclusions);
        }

        if (!restrictionsOnTheAssignee.isEmpty()
                || !reasonsAnExclusionCancels.isEmpty()
                || !reasonsNoExclusionCancels.isEmpty()
                || !exclusions.isEmpty()) {
            if (includeDanglingTasks) {
                final var noAssigneeOrNoCandidate = Criteria.where("dangling").is(Boolean.TRUE);
                reasonsAnExclusionCancels.add(noAssigneeOrNoCandidate);
            }
            final var reasonsToBeVisible = new LinkedList<Criteria>();
            if (!reasonsAnExclusionCancels.isEmpty()
                    || !exclusions.isEmpty()) {
                final var whatIsLeftAfterTheExclusions = new LinkedList<Criteria>();
                if (!reasonsAnExclusionCancels.isEmpty()) {
                    whatIsLeftAfterTheExclusions.add(
                            new Criteria().orOperator(reasonsAnExclusionCancels));
                }
                whatIsLeftAfterTheExclusions.addAll(exclusions);
                reasonsToBeVisible.add(
                        whatIsLeftAfterTheExclusions.size() == 1
                                ? whatIsLeftAfterTheExclusions.getFirst()
                                : new Criteria().andOperator(whatIsLeftAfterTheExclusions));
            }
            reasonsToBeVisible.addAll(reasonsNoExclusionCancels);
            if (!reasonsToBeVisible.isEmpty()) {
                subCriterias.add(
                        reasonsToBeVisible.size() == 1
                                ? reasonsToBeVisible.getFirst()
                                : new Criteria().orOperator(reasonsToBeVisible));
            }
            subCriterias.addAll(restrictionsOnTheAssignee);
        }

        // limit result according to list mode

        // return consistent results across multiple requests of pages
        switch (mode) {
            case All:
                break;
            case OpenTasks:
            case OpenTasksWithFollowUp:
            case OpenTasksWithoutFollowUp:
            case OpenTaskOnlyFollowUp:
                if (initialTimestamp == null) {
                    subCriterias.add(
                            Criteria.where("endedAt").exists(false));
                } else {
                    subCriterias.add(new Criteria().orOperator(
                            Criteria.where("endedAt").exists(false),
                            Criteria.where("endedAt").gte(initialTimestamp)));
                }
                break;
            case ClosedTasksOnly:
                subCriterias.add(Criteria.where("endedAt").exists(true));
                break;
            default:
                throw new RuntimeException("Unsupported mode '"
                        + mode
                        + "'! Did you forget to extend this switch instruction?");
        }

        // take followup-date into account
        if (mode == RetrieveItemsMode.OpenTasksWithoutFollowUp) {
            final Criteria notSet = Criteria.where("followUpDate").exists(false);
            final Criteria inPast = Criteria.where("followUpDate").lte(OffsetDateTime.now());
            final Criteria excludeFollowUps = new Criteria().orOperator(notSet, inPast);
            subCriterias.add(excludeFollowUps);
        } else if (mode == RetrieveItemsMode.OpenTaskOnlyFollowUp) {
            subCriterias.add(Criteria.where("followUpDate").gt(OffsetDateTime.now()));
        }

        // limit result according to predefined filters
        if (predefinedCriterias != null) {
            subCriterias.addAll(predefinedCriterias);
        }

        return new Criteria().andOperator(subCriterias);

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
