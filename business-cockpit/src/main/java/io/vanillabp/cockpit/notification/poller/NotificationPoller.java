package io.vanillabp.cockpit.notification.poller;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.notification.NotificationDeliveryException;
import io.vanillabp.cockpit.notification.NotificationService;
import io.vanillabp.cockpit.notification.model.NotificationConfiguration;
import io.vanillabp.cockpit.notification.model.NotificationOutboxEntry;
import io.vanillabp.cockpit.notification.model.NotificationOutboxRepository;
import io.vanillabp.cockpit.notification.model.NotificationPollState;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.tasklist.model.UserTaskRepository;
import io.vanillabp.cockpit.users.model.User;
import io.vanillabp.cockpit.users.model.UserRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Once-per-interval poller that determines and sends notifications (AC func 2, AC tech 4/6/8/9).
 * <p>
 * A Mongo lease ({@link NotificationPollState}) makes a cycle run on only one cluster node; the
 * persisted cursor makes delta-scanning resilient across restarts; the persistent
 * {@link NotificationOutboxEntry} (with its unique index) makes enqueue idempotent and delivery
 * retried until sent - per recipient, as long as the medium reports which recipients it failed to
 * reach ({@link NotificationDeliveryException}). Registered only when at least one {@link NotificationService} bean exists so
 * installations without notifications keep their exact runtime behavior.
 */
public class NotificationPoller {

    private static final Logger logger = LoggerFactory.getLogger(NotificationPoller.class);

    private static final Duration LEASE_TTL = Duration.ofMinutes(5);

    private final MongoTemplate mongoTemplate;
    private final UserRepository userRepository;
    private final UserTaskRepository userTaskRepository;
    private final NotificationOutboxRepository outboxRepository;
    private final List<NotificationService> notificationServices;
    private final io.vanillabp.cockpit.users.UserDetailsProvider userDirectory;
    private final int maxDeliveryAttempts;
    private final Duration cleanupSentOlderThan;
    private final NotificationScanner scanner = new NotificationScanner();

    private final String nodeId = UUID.randomUUID().toString();

    public NotificationPoller(
            final MongoTemplate mongoTemplate,
            final UserRepository userRepository,
            final UserTaskRepository userTaskRepository,
            final NotificationOutboxRepository outboxRepository,
            final List<NotificationService> notificationServices,
            final io.vanillabp.cockpit.users.UserDetailsProvider userDirectory,
            final int maxDeliveryAttempts,
            final Duration cleanupSentOlderThan) {

        this.mongoTemplate = mongoTemplate;
        this.userRepository = userRepository;
        this.userTaskRepository = userTaskRepository;
        this.outboxRepository = outboxRepository;
        this.notificationServices = notificationServices;
        this.userDirectory = userDirectory;
        this.maxDeliveryAttempts = maxDeliveryAttempts;
        this.cleanupSentOlderThan = cleanupSentOlderThan;

    }

    @Scheduled(
            fixedRateString = "${business-cockpit.notification.interval:PT1M}",
            initialDelayString = "${business-cockpit.notification.interval:PT1M}")
    public void poll() {

        if (notificationServices.isEmpty()) {
            return; // no medium configured: keep runtime behavior unchanged (AC tech 4)
        }

        try {
            runCycle();
        } catch (Exception e) {
            logger.warn("Notification poll cycle failed", e);
        }

    }

    /**
     * Hourly (configurable) cleanup: deletes successfully sent outbox entries older than the
     * configured retention. The delete is idempotent, so no cluster lease is needed - if several
     * nodes run it, they simply remove the same already-gone rows.
     */
    @Scheduled(
            fixedRateString = "${business-cockpit.notification.cleanup-interval:PT1H}",
            initialDelayString = "${business-cockpit.notification.cleanup-interval:PT1H}")
    public void cleanupOutbox() {

        if (notificationServices.isEmpty()) {
            return;
        }

        try {
            runCleanup();
        } catch (Exception e) {
            logger.warn("Notification outbox cleanup failed", e);
        }

    }

    void runCleanup() {

        final var threshold = OffsetDateTime.now().minus(cleanupSentOlderThan);
        // sentAt is only set on successful delivery; a date comparison never matches pending (null)
        // entries, so this removes exactly the sent entries older than the retention threshold
        final var query = Query.query(Criteria.where("sentAt").lt(threshold));
        final var deleted = mongoTemplate.remove(query, NotificationOutboxEntry.class);
        if (deleted.getDeletedCount() > 0) {
            logger.info("Cleaned up {} sent notification outbox entries older than {}",
                    deleted.getDeletedCount(), cleanupSentOlderThan);
        }

    }

    void runCycle() {

        final var now = OffsetDateTime.now();

        ensureStateExists(now);
        final var state = acquireLease(now);
        if (state == null) {
            return; // lease held by another node
        }

        try {
            final var cursor = state.getCursor();
            if (cursor != null) {
                enqueueNotifications(cursor, now);
            }
            advanceCursorAndRelease(now);
            drainOutbox(now);
        } catch (Exception e) {
            logger.warn("Notification poll cycle aborted", e);
            releaseLease(now);
        }

    }

    private void enqueueNotifications(
            final OffsetDateTime cursor,
            final OffsetDateTime now) {

        final var changed = mongoTemplate
                .find(Query.query(new Criteria().andOperator(
                        Criteria.where("updatedAt").gt(cursor),
                        Criteria.where("updatedAt").lte(now))), UserTask.class);
        if (changed.isEmpty()) {
            return;
        }

        final var directory = buildDirectory();
        for (final var task : changed) {
            for (final var planned : scanner.scan(task, cursor, directory)) {
                insertOutbox(planned, now);
            }
        }

    }

    private RecipientDirectory buildDirectory() {

        final var usersById = userRepository
                .findAll()
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        final var mediaTypes = notificationServices.stream()
                .map(NotificationService::getType)
                .toList();

        return new RecipientDirectory() {
            @Override
            public Collection<String> mediaTypes() {
                return mediaTypes;
            }

            @Override
            public boolean isLoggedIn(String userId) {
                return usersById.containsKey(userId);
            }

            @Override
            public Collection<String> loggedInUserIds() {
                return usersById.keySet();
            }

            @Override
            public List<String> authoritiesOf(String userId) {
                // the user directory caches authorities (they are resolved on every browser
                // request), so a direct lookup per user is cheap - no local cache needed
                return userDirectory.getUser(userId).map(UserDetails::getAuthorities).orElse(null);
            }

            @Override
            public NotificationConfiguration configOf(String userId) {
                final var user = usersById.get(userId);
                return user == null ? null : user.getNotificationConfiguration();
            }
        };

    }

    private void insertOutbox(
            final PlannedNotification planned,
            final OffsetDateTime now) {

        final var entry = new NotificationOutboxEntry();
        entry.setUserTaskId(planned.userTaskId());
        entry.setNotificationType(planned.notificationType());
        entry.setMedium(planned.medium());
        entry.setRecipientUserId(planned.recipientUserId());
        entry.setForced(planned.forced());
        entry.setCreatedAt(now);

        // the unique index makes this idempotent: a duplicate simply means the notification was
        // already planned in an earlier (overlapping) scan
        try {
            outboxRepository.insert(entry);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // already planned in an earlier (overlapping) scan
        }

    }

    /** Package-private for testability, like {@link #runCycle()} and {@link #runCleanup()}. */
    void drainOutbox(
            final OffsetDateTime now) {

        final var pending = outboxRepository
                .findBySentAtIsNullAndAttemptsLessThanOrderByCreatedAtAsc(maxDeliveryAttempts);
        if (pending.isEmpty()) {
            return;
        }

        final var serviceByType = notificationServices.stream()
                .collect(Collectors.toMap(NotificationService::getType, s -> s, (a, b) -> a));

        final Map<BulkKey, List<NotificationOutboxEntry>> bulks = pending.stream()
                .collect(Collectors.groupingBy(e -> new BulkKey(
                        e.getMedium(), e.getUserTaskId(), e.getNotificationType(), e.isForced())));

        for (final var bulk : bulks.entrySet()) {
            sendBulk(bulk.getKey(), bulk.getValue(), serviceByType, now);
        }

    }

    private void sendBulk(
            final BulkKey key,
            final List<NotificationOutboxEntry> entries,
            final Map<String, NotificationService> serviceByType,
            final OffsetDateTime now) {

        final var service = serviceByType.get(key.medium());
        if (service == null) {
            recordFailure(entries, key, "no NotificationService for medium '" + key.medium() + "'", null);
            return;
        }

        final var task = userTaskRepository.findById(key.userTaskId()).orElse(null);
        if (task == null) {
            // the task vanished - drop these entries so they are not retried forever
            markSent(entries, now);
            return;
        }

        task.setNotificationType(key.notificationType());
        task.setForced(key.forced());
        final var recipientIds = entries.stream()
                .map(NotificationOutboxEntry::getRecipientUserId)
                .distinct()
                .toList();

        try {
            service.sendNotification(recipientIds, task);
            markSent(entries, now);
        } catch (NotificationDeliveryException e) {
            // the medium told us which recipients it could not reach: keep exactly those pending,
            // so the ones already reached are not notified again on the next attempt
            final var failed = e.getFailedRecipientUserIds();
            if (failed.isEmpty()) {
                recordFailure(entries, key, "delivery failed", e);
                return;
            }
            final var pending = entries.stream()
                    .filter(entry -> failed.contains(entry.getRecipientUserId()))
                    .toList();
            final var delivered = entries.stream()
                    .filter(entry -> !failed.contains(entry.getRecipientUserId()))
                    .toList();
            if (!delivered.isEmpty()) {
                markSent(delivered, now);
            }
            recordFailure(pending, key, "delivery failed for " + failed, e);
        } catch (Exception e) {
            // no per-recipient information: the whole bulk stays pending
            recordFailure(entries, key, "delivery failed", e);
        }

    }

    private void recordFailure(
            final List<NotificationOutboxEntry> entries,
            final BulkKey key,
            final String reason,
            final Exception cause) {

        if (entries.isEmpty()) {
            return;
        }

        // count this delivery attempt for the entries which stay pending; once attempts reaches the
        // maximum an entry becomes stale and is excluded from future scans until its counter is
        // reset manually in MongoDB
        entries.forEach(e -> e.setAttempts(e.getAttempts() + 1));
        final var attemptNo = entries.stream()
                .mapToInt(NotificationOutboxEntry::getAttempts)
                .max()
                .orElse(1);

        // persist the incremented attempt counter; these entries stay pending and are retried next
        // cycle (at-least-once per recipient) until they either succeed or turn stale
        outboxRepository.saveAll(entries);

        if (attemptNo >= maxDeliveryAttempts) {
            logger.error("Notification bulk {} is now stale after {} attempt(s) ({}); it will not be "
                    + "retried until its 'attempts' counter is reset in MongoDB", key, attemptNo, reason, cause);
        } else if (attemptNo >= 3) {
            // from the third attempt on, escalate WARN to ERROR
            logger.error("Could not send notification bulk {} (attempt {}: {})", key, attemptNo, reason, cause);
        } else {
            logger.warn("Could not send notification bulk {} (attempt {}: {})", key, attemptNo, reason, cause);
        }

    }

    private void markSent(
            final List<NotificationOutboxEntry> entries,
            final OffsetDateTime now) {

        entries.forEach(e -> e.setSentAt(now));
        outboxRepository.saveAll(entries);

    }

    private void ensureStateExists(
            final OffsetDateTime now) {

        final var exists = mongoTemplate
                .exists(Query.query(Criteria.where("_id").is(NotificationPollState.SINGLETON_ID)),
                        NotificationPollState.class);
        if (exists) {
            return;
        }

        final var initial = new NotificationPollState();
        initial.setId(NotificationPollState.SINGLETON_ID);
        // no cursor yet: the first cycle sets it to "now" and skips any backlog
        try {
            mongoTemplate.insert(initial);
        } catch (Exception e) {
            // another node created it in the meantime
        }

    }

    private NotificationPollState acquireLease(
            final OffsetDateTime now) {

        final var query = Query.query(new Criteria().andOperator(
                Criteria.where("_id").is(NotificationPollState.SINGLETON_ID),
                new Criteria().orOperator(
                        Criteria.where("lockedUntil").is(null),
                        Criteria.where("lockedUntil").lt(now))));
        final var update = new Update()
                .set("lockedUntil", now.plus(LEASE_TTL))
                .set("owner", nodeId);
        return mongoTemplate
                .findAndModify(query, update,
                        FindAndModifyOptions.options().returnNew(true), NotificationPollState.class);

    }

    private void advanceCursorAndRelease(
            final OffsetDateTime now) {

        mongoTemplate
                .updateFirst(
                        Query.query(Criteria.where("_id").is(NotificationPollState.SINGLETON_ID)),
                        new Update().set("cursor", now).set("lockedUntil", now),
                        NotificationPollState.class);

    }

    private void releaseLease(
            final OffsetDateTime now) {

        try {
            mongoTemplate
                    .updateFirst(
                            Query.query(Criteria.where("_id").is(NotificationPollState.SINGLETON_ID)),
                            new Update().set("lockedUntil", now),
                            NotificationPollState.class);
        } catch (Exception e) {
            logger.debug("Could not release the notification poll lease", e);
        }

    }

    private record BulkKey(
            String medium,
            String userTaskId,
            io.vanillabp.cockpit.notification.NotificationType notificationType,
            boolean forced) {
        // grouping key for one sendNotification bulk
    }

}
