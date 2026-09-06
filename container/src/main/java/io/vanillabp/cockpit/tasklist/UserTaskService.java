package io.vanillabp.cockpit.tasklist;

import com.mongodb.client.model.changestream.ChangeStreamDocument;
import io.vanillabp.cockpit.commons.exceptions.BcUnauthorizedException;
import io.vanillabp.cockpit.commons.mongo.changestreams.ChangeStreamUtils;
import io.vanillabp.cockpit.commons.mongo.updateinfo.UpdateInformationAware;
import io.vanillabp.cockpit.commons.security.usercontext.UserContext;
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
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserTaskService {

    public static final String INDEX_CUSTOM_SORT_PREFIX = "_sort_";
    public static final String PROPERTY_DUEDATE = "dueDate";
    public static final String PROPERTY_CREATEDAT = "createdAt";
    public static final String PROPERTY_ID = "id";

    public static enum RetrieveItemsMode {
        All,
        OpenTasks,
        OpenTasksWithoutFollowUp,
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
    private UserTaskRepository userTasks;

    @Autowired
    private KwicService kwicService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private UserContext currentUserContext;

    private Subscription dbChangesSubscription;

    /**
     * The initiator to be recorded for a change caused by the business cockpit itself: the
     * logged-in user, or {@link UpdateInformationAware#COCKPIT_USER} if there is none (e.g. a
     * cockpit-side job). Kept apart from {@link UpdateInformationAware#SYSTEM_USER}, which marks
     * changes reported by the workflow system.
     * <p>
     * The initiator is the only record of who caused the latest change: {@code updatedBy} is audit
     * information overwritten by {@code UpdateInformationEventListener} on every save. The
     * notification poller reads it to skip notifications a user triggered himself, so every
     * cockpit-side modification has to maintain it.
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

        final var knownSorts = mongoTemplate
                .indexOps(UserTask.COLLECTION_NAME)
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

    @EventListener
    public void subscribeToDbChanges(
            final ApplicationStartedEvent event) {

        dbChangesSubscription = changeStreamUtils.subscribe(
                UserTask.class,
                this::publishUserTaskChange);

    }

    private void publishUserTaskChange(
            final Message<ChangeStreamDocument<Document>, UserTask> message) {

        try {
            applicationEventPublisher.publishEvent(
                    UserTaskChangedNotification.build(message));
        } catch (Exception e) {
            logger.warn("Error on processing user-task change-stream event! Will resume stream.", e);
        }

    }

    @PreDestroy
    public void cleanup() {

        if (dbChangesSubscription != null) {
            changeStreamUtils.unsubscribe(dbChangesSubscription);
        }

    }

    public UserTask getUserTask(
            final String userTaskId) {

        return userTasks
                .findById(userTaskId)
                .orElse(null);

    }

    public UserTask markAsRead(
            final String userTaskId,
            final String userId) {

        final var userTask = getUserTask(userTaskId);
        if (userTask == null) {
            return null;
        }
        userTask.setReadAt(userId);
        return saveInitiatedByCockpit(userTask);

    }

    public List<UserTask> markAsRead(
            final Collection<String> userTaskIds,
            final String userId) {

        final var found = userTasks.findAllById(userTaskIds);
        found.forEach(userTask -> userTask.setReadAt(userId));
        return saveAllInitiatedByCockpit(found);

    }

    public UserTask markAsUnread(
            final String userTaskId,
            final String userId) {

        final var userTask = getUserTask(userTaskId);
        if (userTask == null) {
            return null;
        }
        userTask.clearReadAt(userId);
        return saveInitiatedByCockpit(userTask);

    }

    public List<UserTask> markAsUnread(
            final Collection<String> userTaskIds,
            final String userId) {

        final var found = userTasks.findAllById(userTaskIds);
        found.forEach(userTask -> userTask.clearReadAt(userId));
        return saveAllInitiatedByCockpit(found);

    }

    public UserTask assignTask(
            final String userTaskId,
            final Person person) {

        final var userTask = getUserTask(userTaskId);
        if (userTask == null) {
            return null;
        }
        userTask.addCandidatePerson(person);
        return saveInitiatedByCockpit(userTask);

    }

    public List<UserTask> assignTask(
            final Collection<String> userTaskIds,
            final Person person) {

        final var found = userTasks.findAllById(userTaskIds);
        found.forEach(userTask -> userTask.addCandidatePerson(person));
        return saveAllInitiatedByCockpit(found);

    }

    public UserTask unassignTask(
            final String userTaskId,
            final String personId) {

        final var userTask = getUserTask(userTaskId);
        if (userTask == null) {
            return null;
        }
        userTask.removeCandidatePerson(personId);
        return saveInitiatedByCockpit(userTask);

    }

    public List<UserTask> unassignTask(
            final Collection<String> userTaskIds,
            final String personId) {

        final var found = userTasks.findAllById(userTaskIds);
        found.forEach(userTask -> userTask.removeCandidatePerson(personId));
        return saveAllInitiatedByCockpit(found);

    }

    public UserTask setFollowUpDate(
            final String userTaskId,
            final OffsetDateTime followUpDate) {

        final var normalizedFollowUpDate = followUpDate == null
                ? null
                : followUpDate.withSecond(0).withNano(0);

        final var userTask = getUserTask(userTaskId);
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
            final String userTaskId,
            final Person person) {

        final var userTask = getUserTask(userTaskId);
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
            final Collection<String> userTaskIds,
            final Person person) {

        final var found = userTasks.findAllById(userTaskIds);
        found.forEach(userTask -> userTask.setAssignee(person));
        return saveAllInitiatedByCockpit(found);

    }

    public UserTask unclaimTask(
            final String currentUser,
            final String userTaskId,
            final String personId) {

        final var query = new Query();
        query.addCriteria(Criteria.where("id").is(userTaskId));
        query.addCriteria(Criteria.where("assignee.id").is(personId));
        final var update = new Update();
        update.unset("assignee");
        update.set("updatedAt", OffsetDateTime.now());
        update.set("updatedBy", currentUser == null ? UpdateInformationAware.SYSTEM_USER : currentUser);
        update.set("initiator", currentUser == null ? UpdateInformationAware.COCKPIT_USER : currentUser);

        mongoTemplate.updateFirst(query, update, UserTask.class);

        return getUserTask(userTaskId);

    }

    public List<UserTask> unclaimTask(
            final String currentUser,
            final Collection<String> userTaskIds,
            final String personId) {

        final var query = new Query();
        query.addCriteria(Criteria.where("id").in(userTaskIds));
        query.addCriteria(Criteria.where("assignee.id").is(personId));
        final var update = new Update();
        update.unset("assignee");
        update.set("updatedAt", OffsetDateTime.now());
        update.set("updatedBy", currentUser == null ? UpdateInformationAware.SYSTEM_USER : currentUser);
        update.set("initiator", currentUser == null ? UpdateInformationAware.COCKPIT_USER : currentUser);

        mongoTemplate.updateMulti(query, update, UserTask.class);

        final var findQuery = new Query();
        findQuery.addCriteria(Criteria.where("id").in(userTaskIds));

        return mongoTemplate.find(findQuery, UserTask.class);

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

    public Page<UserTask> getUserTasks(
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
            final RetrieveItemsMode mode) {

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
                mode);

    }

    protected Page<UserTask> retrieveUserTasks(
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

        // build index before retrieving data if necessary
        ensureSortAndFilterIndex(
                orderBySort,
                UserTask.COLLECTION_NAME,
                "Could not create Mongo-DB index for sorting and filtering of tasklist");

        final var numberOfUserTasksFound = mongoTemplate
                .count(Query.of(query).limit(-1).skip(-1), UserTask.class);
        final var userTasksFound = mongoTemplate
                .find(query.with(pageRequest), UserTask.class);

        return PageableExecutionUtils.getPage(
                userTasksFound,
                pageRequest,
                () -> numberOfUserTasksFound);

    }

    /**
     * Sorting and filtering by arbitrary properties needs an index per combination. They are
     * created the first time such a combination is asked for and remembered afterwards. A failing
     * creation is remembered as well: the query still works without the index, and retrying it on
     * every request would only cost time.
     */
    private void ensureSortAndFilterIndex(
            final UserTaskListOrder orderBySort,
            final String collectionName,
            final String errorMessage) {

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
                        .indexOps(collectionName)
                        .createIndex(newIndex);
            } catch (Exception e) {
                logger.error(errorMessage, e);
            }
            sortAndFilterIndexes.add(orderBySort.indexName);
        } finally {
            sortAndFilterIndexWriteLock.unlock();
        }

    }

    public List<KwicResult> kwic(
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
            return List.of();
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
                ).getContent();

    }

    public Page<UserTask> getUserTasksUpdated(
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
                        includeDanglingTasks,
                        notInAssignees,
                        assignees,
                        candidateUsers,
                        candidateGroups,
                        candidatesToBeExcluded,
                        initialTimestamp,
                        effectiveMode,
                        null));
        final var searchCriteria = SearchCriteriaHelper.buildSearchCriteria(searchQueries);
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

    public boolean completeUserTask(
            final UserTask userTask,
            final OffsetDateTime timestamp) {

        if (userTask == null) {
            return false;
        }

        userTask.setEndedAt(timestamp);
        userTask.setUpdatedAt(timestamp);
        userTask.setEndReason(io.vanillabp.cockpit.tasklist.model.UserTaskEndReason.COMPLETED);

        try {
            userTasks.save(userTask);
            return true;
        } catch (Exception e) {
            logger.error("Could not save user task '{}'!",
                    userTask.getId(),
                    e);
            return false;
        }

    }

    public boolean cancelUserTask(
            final UserTask userTask,
            final OffsetDateTime timestamp,
            final String reason) {

        if (userTask == null) {
            return false;
        }

        userTask.setEndedAt(timestamp);
        userTask.setUpdatedAt(timestamp);
        userTask.setComment(reason);
        userTask.setEndReason(io.vanillabp.cockpit.tasklist.model.UserTaskEndReason.CANCELLED);

        try {
            userTasks.save(userTask);
            return true;
        } catch (Exception e) {
            logger.error("Could not save user task '{}'!",
                    userTask.getId(),
                    e);
            return false;
        }

    }

    public boolean createUserTask(
            final UserTask userTask) {

        if (userTask == null) {
            return false;
        }

        if (userTask.getDueDate() == null) {
            // for correct sorting
            userTask.setDueDate(OffsetDateTime.MAX);
        }

        // the cockpit's own clock: 'createdAt' is the reporting system's timestamp and may lag
        // behind (or run ahead of) this one, which would break delta-scanning for notifications
        final var reportedAt = OffsetDateTime.now();
        userTask.setReportedAt(reportedAt);
        // the candidates a report brings along are known as of now
        userTask.stampCandidatesSince(reportedAt);

        try {
            userTasks.save(userTask);
            return true;
        } catch (Exception e) {
            logger.error("Could not save user task '{}'!",
                    userTask.getId(),
                    e);
            return false;
        }

    }

    public boolean updateUserTask(
            final UserTask userTask) {

        if (userTask == null) {
            return false;
        }

        try {
            userTasks.save(userTask);
            return true;
        } catch (Exception e) {
            logger.error("Could not save user task '{}'!",
                    userTask.getId(),
                    e);
            return false;
        }

    }

    /**
     * The distinct workflows (module + BPMN process, with the workflow title) the given user has
     * visible tasks for - the same visibility as the user task list. Used by the notification
     * configuration page to offer per-workflow exceptions (AC func 4c).
     */
    public List<UserTask> getVisibleWorkflows(
            final Collection<String> assignees,
            final Collection<String> candidateUsers,
            final Collection<String> candidateGroups,
            final Collection<String> candidatesToBeExcluded) {

        final var criteria = buildUserTasksCriteria(
                true, false, assignees, candidateUsers, candidateGroups, candidatesToBeExcluded,
                null, RetrieveItemsMode.All, List.of());

        final var aggregation = org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation(
                org.springframework.data.mongodb.core.aggregation.Aggregation.match(criteria),
                org.springframework.data.mongodb.core.aggregation.Aggregation
                        .group("workflowModuleId", "bpmnProcessId")
                        .first("workflowModuleId").as("workflowModuleId")
                        .first("bpmnProcessId").as("bpmnProcessId")
                        .first("workflowTitle").as("workflowTitle"));

        return mongoTemplate
                .aggregate(aggregation, UserTask.COLLECTION_NAME, UserTask.class)
                .getMappedResults();

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

}
